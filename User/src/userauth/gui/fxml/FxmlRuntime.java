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

// Ghi chu file: File controller JavaFX; dieu khien hanh vi cua man hinh, hop thoai hoac thanh phan UI.
// Khai bao lop FxmlRuntime; dieu khien mot man hinh hoac thanh phan JavaFX cu the.
final class FxmlRuntime {
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho base.
    private static final String FXML_BASE = "/userauth/gui/fxml/";
    // Ham tao: khoi tao doi tuong FxmlRuntime voi cac phu thuoc can thiet.
    private FxmlRuntime() {
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac load view.
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
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac create modal dialog.
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
    // Phuong thuc: thuc hien chuc nang play dialog reveal trong lop FxmlRuntime.
    private static void playDialogReveal(Parent root) {
        if (root == null) {
            return;
        }
        UiEffects.playEntrance(root, 0, 0, 10);
    }
}
