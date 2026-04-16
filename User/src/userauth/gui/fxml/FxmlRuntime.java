package userauth.gui.fxml;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;

final class FxmlRuntime {
    private static final String FXML_BASE = "/userauth/gui/fxml/";

    private FxmlRuntime() {
    }

    static <T> LoadedView<T> loadView(Class<?> anchor, String fileName, String resourceKind) {
        FXMLLoader loader = new FXMLLoader(anchor.getResource(FXML_BASE + fileName));
        try {
            Parent root = loader.load();
            return new LoadedView<>(root, loader.getController());
        } catch (IOException ex) {
            throw new IllegalStateException("Khong the tai FXML " + resourceKind + ": " + fileName, ex);
        }
    }

    static Stage createModalDialog(Window owner, String title, Parent root, double width, double height) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setTitle(title == null || title.isBlank() ? "Thong bao" : title);
        dialog.setResizable(false);
        dialog.setScene(new Scene(root, width, height));
        return dialog;
    }
}
