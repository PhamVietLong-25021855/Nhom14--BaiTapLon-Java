package userauth.fxml.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import userauth.controller.AuthController;
import userauth.gui.fxml.shared.UiAsync;
import userauth.gui.fxml.shared.UiText;
import userauth.model.User;
import userauth.validation.UserValidator;

import java.util.Objects;
import java.util.function.Consumer;

public class ProfileDialogController {
    @FXML
    private Label lblUsername;

    @FXML
    private Label lblRole;

    @FXML
    private TextField txtFullName;

    @FXML
    private TextField txtEmail;

    @FXML
    private Label lblError;

    private Stage dialogStage;
    private AuthController authController;
    private User user;
    private Consumer<User> successHandler = ignored -> {};
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
        lblRole.setText(user == null ? UiText.text("Role: -") : UiText.text("Role: " + user.getRoleName()));
        txtFullName.setText(user == null || user.getFullName() == null ? "" : user.getFullName());
        txtEmail.setText(user == null || user.getEmail() == null ? "" : user.getEmail());
    }

    public void setSuccessHandler(Consumer<User> successHandler) {
        this.successHandler = Objects.requireNonNullElse(successHandler, ignored -> {});
    }

    @FXML
    private void handleSubmit() {
        hideError();

        if (authController == null || user == null) {
            showError(UiText.text("Not enough information to update the profile."));
            return;
        }

        String fullName = txtFullName.getText() == null ? "" : txtFullName.getText().trim();
        String email = txtEmail.getText() == null ? "" : txtEmail.getText().trim();
        if (fullName.isBlank() || email.isBlank()) {
            showError(UiText.text("Please enter your full name and email."));
            return;
        }
        if (!UserValidator.isValidEmail(email)) {
            showError(UiText.text("Invalid email."));
            return;
        }

        if (submitInProgress) {
            return;
        }

        submitInProgress = true;
        setBusy(true);
        UiAsync.run(
                () -> authController.updateProfileGUI(user, fullName, email),
                result -> {
                    submitInProgress = false;
                    setBusy(false);
                    if ("SUCCESS".equals(result)) {
                        successHandler.accept(user);
                        closeDialog();
                        return;
                    }

                    showError(result);
                },
                error -> {
                    submitInProgress = false;
                    setBusy(false);
                    showError(UiText.text("Unable to update the profile right now."));
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
        if (txtFullName != null) {
            txtFullName.setDisable(busy);
        }
        if (txtEmail != null) {
            txtEmail.setDisable(busy);
        }
    }
}
