package userauth.gui.fxml;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import java.io.IOException;

final class FxmlRuntime {
    private static final String FXML_BASE = "/userauth/gui/fxml/";

    private FxmlRuntime() {
    }

    static <T> LoadedView<T> loadView(Class<?> anchor, String fileName, String resourceKind) {
        FXMLLoader loader = new FXMLLoader(anchor.getResource(FXML_BASE + fileName));
        try {
            Parent root = loader.load();
            UiText.apply(root);
            return new LoadedView<>(root, loader.getController());
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load FXML " + resourceKind + ": " + fileName, ex);
        }
    }

    static Stage createModalDialog(Window owner, String title, Parent root, double width, double height) {
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
            dialog.addEventHandler(WindowEvent.WINDOW_SHOWN, event -> playDialogReveal(root));
        }
        return dialog;
    }

    private static void playDialogReveal(Parent root) {
        if (root == null) {
            return;
        }
        UiEffects.playEntrance(root, 0, 0, 10);
    }
}
