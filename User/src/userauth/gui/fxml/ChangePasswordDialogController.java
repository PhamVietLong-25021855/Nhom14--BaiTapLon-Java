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
    private boolean submitInProgress;

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
        lblUsername.setText(user == null ? UiText.text("Account: -") : UiText.text("Account:") + " " + user.getUsername());
    }

    public void setSuccessHandler(Consumer<String> successHandler) {
        this.successHandler = Objects.requireNonNullElse(successHandler, message -> {});
    }

    @FXML
    private void handleSubmit() {
        hideError();

        if (authController == null || user == null) {
            showError(UiText.text("Not enough information to change the password."));
            return;
        }

        String oldPassword = txtOldPassword.getText() == null ? "" : txtOldPassword.getText().trim();
        String newPassword = txtNewPassword.getText() == null ? "" : txtNewPassword.getText().trim();

        if (oldPassword.isEmpty() || newPassword.isEmpty()) {
            showError(UiText.text("Please enter both the current password and the new password."));
            return;
        }

        if (submitInProgress) {
            return;
        }

        submitInProgress = true;
        setBusy(true);
        UiAsync.run(
                () -> authController.changePassword(user.getUsername(), oldPassword, newPassword),
                result -> {
                    submitInProgress = false;
                    setBusy(false);
                    if ("SUCCESS".equals(result)) {
                        successHandler.accept(UiText.text("Password changed successfully."));
                        closeDialog();
                        return;
                    }

                    showError(result);
                },
                error -> {
                    submitInProgress = false;
                    setBusy(false);
                    showError(UiText.text("Unable to change the password right now."));
                }
        );
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void showError(String message) {
        lblError.setText(UiText.text(message == null ? "" : message));
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

    private void setBusy(boolean busy) {
        if (txtOldPassword != null) {
            txtOldPassword.setDisable(busy);
        }
        if (txtNewPassword != null) {
            txtNewPassword.setDisable(busy);
        }
    }
}
