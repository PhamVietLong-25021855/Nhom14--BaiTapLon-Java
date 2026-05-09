package userauth.gui.fxml.shared;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.net.URL;

public final class FxmlRuntime {
    private static final String FXML_BASE = "/userauth/gui/fxml/";

    private FxmlRuntime() {
    }

    public static <T> LoadedView<T> loadView(Class<?> anchor, String fileName, String resourceKind) {
        URL resource = FxmlRuntime.class.getResource(FXML_BASE + fileName);
        if (resource == null) {
            throw new IllegalStateException("FXML resource not found: " + FXML_BASE + fileName);
        }

        FXMLLoader loader = new FXMLLoader(resource);
        try {
            Parent root = loader.load();
            UiText.apply(root);
            return new LoadedView<>(root, loader.getController());
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load FXML " + resourceKind + ": " + fileName, ex);
        }
    }

    public static Stage createModalDialog(Window owner, String title, Parent root, double width, double height) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.setTitle(UiText.text(title == null || title.isBlank() ? "Notification" : title));
        dialog.setResizable(false);
        Scene scene = new Scene(root, width, height);
        scene.setFill(null);
        dialog.setScene(scene);
        if (root != null) {
            root.setOpacity(0);
            root.setTranslateX(0);
            root.setTranslateY(10);
            enableWindowDrag(dialog, root);
            dialog.addEventHandler(WindowEvent.WINDOW_SHOWN, event -> playDialogReveal(root));
        }
        return dialog;
    }

    private static void enableWindowDrag(Stage dialog, Parent root) {
        final double[] dragOffset = new double[2];
        root.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (isInteractiveTarget(event.getTarget())) {
                return;
            }
            dragOffset[0] = event.getScreenX() - dialog.getX();
            dragOffset[1] = event.getScreenY() - dialog.getY();
        });
        root.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (isInteractiveTarget(event.getTarget())) {
                return;
            }
            dialog.setX(event.getScreenX() - dragOffset[0]);
            dialog.setY(event.getScreenY() - dragOffset[1]);
        });
    }

    private static boolean isInteractiveTarget(Object target) {
        if (!(target instanceof Node node)) {
            return false;
        }
        for (Node current = node; current != null; current = current.getParent()) {
            if (current instanceof ButtonBase || current instanceof TextInputControl) {
                return true;
            }
        }
        return false;
    }

    private static void playDialogReveal(Parent root) {
        if (root == null) {
            return;
        }
        UiEffects.playEntrance(root, 0, 0, 10);
    }
}
