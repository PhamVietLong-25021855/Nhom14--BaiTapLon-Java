package userauth.gui;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FxmlResourceConsistencyTest {
    private static final Path FXML_ROOT = Paths.get("User", "resources", "userauth", "gui", "fxml");
    private static final Path SOURCE_ROOT = Paths.get("User", "src", "userauth");
    private static final String FXML_NAMESPACE = "http://javafx.com/fxml/1";
    private static final Pattern LOAD_VIEW_PATTERN = Pattern.compile(
            "FxmlRuntime\\.loadView\\([^;]*?,\\s*\"([^\"]+\\.fxml)\"",
            Pattern.DOTALL
    );

    @Test
    void everyFxmlFileIsParsableHasControllerAndValidCssReferences() throws Exception {
        assertTrue(Files.isDirectory(FXML_ROOT), "Missing FXML root: " + FXML_ROOT);

        List<Path> fxmlFiles = listFiles(FXML_ROOT, ".fxml");

        assertFalse(fxmlFiles.isEmpty(), "No FXML files were found.");
        for (Path fxml : fxmlFiles) {
            Document document = parseXml(fxml);
            Element root = document.getDocumentElement();
            String controller = controllerName(root);

            assertFalse(controller.isBlank(), () -> "Missing fx:controller in " + fxml);
            assertDoesNotThrow(() -> Class.forName(controller), () -> "Controller class not found for " + fxml);
            assertClasspathResourceExists(fxml);
            assertStylesheetReferencesExist(fxml, root);
        }
    }

    @Test
    void fxmlRuntimeLoadViewCallsPointToExistingResources() throws IOException {
        List<String> missingResources = new ArrayList<>();
        int referenceCount = 0;

        for (Path sourceFile : listFiles(SOURCE_ROOT, ".java")) {
            String source = Files.readString(sourceFile, StandardCharsets.UTF_8);
            Matcher matcher = LOAD_VIEW_PATTERN.matcher(source);
            while (matcher.find()) {
                referenceCount++;
                String fxmlPath = matcher.group(1);
                Path resource = FXML_ROOT.resolve(fxmlPath).normalize();
                if (!Files.isRegularFile(resource)) {
                    missingResources.add(sourceFile + " -> " + fxmlPath);
                }
            }
        }

        assertTrue(referenceCount > 0, "No FxmlRuntime.loadView calls were found.");
        assertTrue(missingResources.isEmpty(), () -> "Missing FXML resources: " + missingResources);
    }

    @Test
    void registerViewDoesNotExposeAdminRole() throws IOException {
        Path registerView = FXML_ROOT.resolve(Paths.get("auth", "register-view.fxml"));
        String content = Files.readString(registerView, StandardCharsets.UTF_8);

        assertTrue(content.contains("BIDDER"));
        assertTrue(content.contains("SELLER"));
        assertFalse(content.contains("fx:value=\"ADMIN\""));
    }

    @Test
    void authFrameDoesNotOpenFullscreenByDefault() throws Exception {
        var openFullscreen = Class.forName("userauth.gui.fxml.shell.AuthFrame")
                .getDeclaredField("OPEN_FULLSCREEN");
        openFullscreen.setAccessible(true);

        assertFalse(openFullscreen.getBoolean(null));
    }

    private static List<Path> listFiles(Path root, String suffix) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(suffix))
                    .sorted()
                    .toList();
        }
    }

    private static Document parseXml(Path fxml)
            throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(fxml.toFile());
    }

    private static String controllerName(Element root) {
        String controller = root.getAttributeNS(FXML_NAMESPACE, "controller");
        if (controller == null || controller.isBlank()) {
            controller = root.getAttribute("fx:controller");
        }
        return controller == null ? "" : controller;
    }

    private static void assertClasspathResourceExists(Path fxml) {
        String relativePath = FXML_ROOT.relativize(fxml).toString().replace(File.separatorChar, '/');
        String classpathPath = "/userauth/gui/fxml/" + relativePath;

        assertNotNull(
                FxmlResourceConsistencyTest.class.getResource(classpathPath),
                () -> "FXML is not available on the test classpath: " + classpathPath
        );
    }

    private static void assertStylesheetReferencesExist(Path fxml, Node node) {
        NamedNodeMap attributes = node.getAttributes();
        if (attributes != null) {
            for (int i = 0; i < attributes.getLength(); i++) {
                String value = attributes.item(i).getNodeValue();
                assertCssTokensExist(fxml, value);
            }
        }

        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            assertStylesheetReferencesExist(fxml, child);
        }
    }

    private static void assertCssTokensExist(Path fxml, String value) {
        if (value == null || !value.contains(".css")) {
            return;
        }

        for (String token : value.split("\\s+")) {
            if (!token.startsWith("@") || !token.endsWith(".css")) {
                continue;
            }

            Path cssFile = fxml.getParent().resolve(token.substring(1)).normalize();
            assertTrue(Files.isRegularFile(cssFile), () -> "Missing CSS resource in " + fxml + ": " + token);
        }
    }
}
