package userauth.gui.fxml;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import userauth.controller.AuthController;
import userauth.model.User;

import java.util.Objects;
import java.util.function.Consumer;

public class ChangePasswordDialogController {
    @FXML
    private Label lblUsername;

    @FXML
    private PasswordField txtOldPassword;

    @FXML
    private PasswordField txtNewPassword;

    @FXML
    private Label lblError;

    private Stage dialogStage;
    private AuthController authController;
    private User user;
    private Consumer<String> successHandler = message -> {};

    @FXML
    private void initialize() {
        hideError();
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setAuthController(AuthController authController) {
        this.authController = authController;
    }

    public void setUser(User user) {
        this.user = user;
        lblUsername.setText(user == null ? "Tai khoan: -" : "Tai khoan: " + user.getUsername());
    }

    public void setSuccessHandler(Consumer<String> successHandler) {
        this.successHandler = Objects.requireNonNullElse(successHandler, message -> {});
    }

    @FXML
    private void handleSubmit() {
        hideError();

        if (authController == null || user == null) {
            showError("Chua du thong tin de doi mat khau.");
            return;
        }

        String oldPassword = txtOldPassword.getText() == null ? "" : txtOldPassword.getText().trim();
        String newPassword = txtNewPassword.getText() == null ? "" : txtNewPassword.getText().trim();

        if (oldPassword.isEmpty() || newPassword.isEmpty()) {
            showError("Vui long nhap day du mat khau hien tai va mat khau moi.");
            return;
        }

        String result = authController.changePassword(user.getUsername(), oldPassword, newPassword);
        if ("SUCCESS".equals(result)) {
            successHandler.accept("Doi mat khau thanh cong.");
            closeDialog();
            return;
        }

        showError(result);
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void showError(String message) {
        lblError.setText(message == null ? "" : message);
        lblError.setManaged(true);
        lblError.setVisible(true);
    }

    private void hideError() {
        lblError.setManaged(false);
        lblError.setVisible(false);
        lblError.setText("");
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
}
