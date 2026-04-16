package userauth.gui.fxml;

import javafx.stage.Stage;
import javafx.stage.Window;

public final class NotificationUtil {
    private NotificationUtil() {
    }

    public static void success(Window owner, String title, String message) {
        showMessage(owner, title, message, "DONG", "success-button");
    }

    public static void info(Window owner, String title, String message) {
        showMessage(owner, title, message, "DONG", "primary-button");
    }

    public static void warning(Window owner, String title, String message) {
        showMessage(owner, title, message, "DONG", "warning-button");
    }

    public static void error(Window owner, String title, String message) {
        showMessage(owner, title, message, "DONG", "danger-button");
    }

    public static boolean confirm(Window owner, String title, String message) {
        LoadedView<ModalMessageController> view = FxmlRuntime.loadView(NotificationUtil.class, "modal-message.fxml", "dialog");
        Stage dialog = FxmlRuntime.createModalDialog(owner, title, view.root(), 420, 240);
        view.controller().setDialogStage(dialog);
        view.controller().configure(title, message, "XAC NHAN", "primary-button", "HUY", true);
        dialog.showAndWait();
        return view.controller().isConfirmed();
    }

    public static String input(Window owner, String title, String message, String defaultValue) {
        LoadedView<TextInputDialogController> view = FxmlRuntime.loadView(NotificationUtil.class, "text-input-dialog.fxml", "dialog");
        Stage dialog = FxmlRuntime.createModalDialog(owner, title, view.root(), 430, 280);
        view.controller().setDialogStage(dialog);
        view.controller().configure(title, message, defaultValue, "XAC NHAN", "HUY", "primary-button");
        dialog.setOnShown(event -> view.controller().requestInputFocus());
        dialog.showAndWait();
        return view.controller().getInputValue();
    }

    private static void showMessage(Window owner, String title, String message, String primaryText, String primaryStyleClass) {
        LoadedView<ModalMessageController> view = FxmlRuntime.loadView(NotificationUtil.class, "modal-message.fxml", "dialog");
        Stage dialog = FxmlRuntime.createModalDialog(owner, title, view.root(), 420, 240);
        view.controller().setDialogStage(dialog);
        view.controller().configure(title, message, primaryText, primaryStyleClass, null, false);
        dialog.showAndWait();
    }
}
