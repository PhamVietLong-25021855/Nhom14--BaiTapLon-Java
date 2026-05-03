package userauth.gui.fxml;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import userauth.controller.AuthController;
import userauth.model.Role;
import userauth.validation.UserValidator;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

public class RegisterViewController {
    private static final String INPUT_ERROR = "input-error";
    private static final String INPUT_VALID = "input-valid";

    @FXML
    private TextField txtUsername;

    @FXML
    private TextField txtFullName;

    @FXML
    private TextField txtEmail;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private PasswordField txtConfirmPassword;

    @FXML
    private ComboBox<String> cbRole;

    @FXML
    private Label lblStatus;

    @FXML
    private Label lblValidationHint;

    @FXML
    private VBox registerCard;

    @FXML
    private VBox fieldContainer;

    private AuthController authController;
    private Runnable showHomeHandler = () -> {};
    private Runnable backToLoginHandler = () -> {};
    private Consumer<String> successHandler = message -> NotificationUtil.success(null, "Notification", message);
    private Consumer<String> warningHandler = message -> NotificationUtil.warning(null, "Notification", message);
    private Consumer<String> errorHandler = message -> NotificationUtil.error(null, "Error", message);
    private boolean registerInProgress;

    @FXML
    private void initialize() {
        if (cbRole != null) {
            cbRole.getItems().setAll(Role.BIDDER.name(), Role.SELLER.name());
        }
        if (cbRole.getValue() == null || Role.ADMIN.name().equals(cbRole.getValue())) {
            cbRole.setValue(Role.BIDDER.name());
        }
        UiText.configureTranslatedComboBox(cbRole);

        hideStatus();
        registerValidationListeners();
        Platform.runLater(() -> {
            UiEffects.playEntrance(registerCard, 120, 24, 0);
            UiEffects.playStaggered(fieldContainer.getChildren(), 180, 34, 14);
        });
    }

    @FXML
    private void handleRegister() {
        hideStatus();

        if (authController == null) {
            showInlineError("AuthController has not been assigned to RegisterViewController.");
            warningHandler.accept("AuthController has not been assigned to RegisterViewController.");
            return;
        }

        String username = txtUsername.getText().trim();
        String fullName = txtFullName.getText().trim();
        String email = txtEmail.getText().trim();
        String password = txtPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();
        Role role = parseRole(cbRole.getValue());

        if (!validateBeforeSubmit(username, fullName, email, password, confirmPassword, role)) {
            return;
        }

        if (registerInProgress) {
            return;
        }

        registerInProgress = true;
        setBusy(true);
        UiAsync.run(
                () -> authController.registerGUI(username, password, fullName, email, role),
                result -> {
                    registerInProgress = false;
                    setBusy(false);
                    if ("SUCCESS".equals(result) || result.toLowerCase(Locale.ROOT).contains("success")) {
                        clearInputs();
                        successHandler.accept("Registration completed successfully. Please log in.");
                        backToLoginHandler.run();
                        return;
                    }

                    warningHandler.accept(result);
                },
                error -> {
                    registerInProgress = false;
                    setBusy(false);
                    errorHandler.accept("Unable to complete registration.");
                }
        );
    }

    @FXML
    private void handleBackToLogin() {
        backToLoginHandler.run();
    }

    @FXML
    private void handleShowHome() {
        showHomeHandler.run();
    }

    public void setAuthController(AuthController authController) {
        this.authController = authController;
    }

    public void setShowHomeHandler(Runnable showHomeHandler) {
        this.showHomeHandler = Objects.requireNonNullElse(showHomeHandler, () -> {});
    }

    public void setBackToLoginHandler(Runnable backToLoginHandler) {
        this.backToLoginHandler = Objects.requireNonNullElse(backToLoginHandler, () -> {});
    }

    public void setSuccessHandler(Consumer<String> successHandler) {
        this.successHandler = Objects.requireNonNullElse(successHandler, message -> NotificationUtil.success(null, "Notification", message));
    }

    public void setWarningHandler(Consumer<String> warningHandler) {
        this.warningHandler = Objects.requireNonNullElse(warningHandler, message -> NotificationUtil.warning(null, "Notification", message));
    }

    public void setErrorHandler(Consumer<String> errorHandler) {
        this.errorHandler = Objects.requireNonNullElse(errorHandler, message -> NotificationUtil.error(null, "Error", message));
    }

    private Role parseRole(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Role.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void clearInputs() {
        txtUsername.clear();
        txtFullName.clear();
        txtEmail.clear();
        txtPassword.clear();
        txtConfirmPassword.clear();
        cbRole.setValue(Role.BIDDER.name());
        hideStatus();
        updateLiveValidation();
    }

    private void registerValidationListeners() {
        txtUsername.textProperty().addListener((observable, oldValue, newValue) -> updateLiveValidation());
        txtFullName.textProperty().addListener((observable, oldValue, newValue) -> updateLiveValidation());
        txtEmail.textProperty().addListener((observable, oldValue, newValue) -> updateLiveValidation());
        txtPassword.textProperty().addListener((observable, oldValue, newValue) -> updateLiveValidation());
        txtConfirmPassword.textProperty().addListener((observable, oldValue, newValue) -> updateLiveValidation());
        cbRole.valueProperty().addListener((observable, oldValue, newValue) -> updateLiveValidation());
    }

    private void updateLiveValidation() {
        updateRequiredState(txtUsername);
        updateRequiredState(txtFullName);

        String email = txtEmail.getText() == null ? "" : txtEmail.getText().trim();
        updateOptionalValidatedState(txtEmail, email.isEmpty() || UserValidator.isValidEmail(email));

        String password = txtPassword.getText() == null ? "" : txtPassword.getText();
        boolean passwordValid = password.isEmpty() || UserValidator.isValidPassword(password);
        updateOptionalValidatedState(txtPassword, passwordValid);

        String confirmPassword = txtConfirmPassword.getText() == null ? "" : txtConfirmPassword.getText();
        boolean confirmValid = confirmPassword.isEmpty() || confirmPassword.equals(password);
        updateOptionalValidatedState(txtConfirmPassword, confirmValid);

        if (lblValidationHint != null) {
            if (password.isEmpty()) {
                lblValidationHint.setText(UiText.text("A valid password must be at least 6 characters long and include letters and numbers."));
                lblValidationHint.getStyleClass().removeAll("success-text", "error-text");
                return;
            }

            if (passwordValid && confirmValid) {
                lblValidationHint.setText(UiText.text("Password looks good. You can continue registration."));
                lblValidationHint.getStyleClass().remove("error-text");
                if (!lblValidationHint.getStyleClass().contains("success-text")) {
                    lblValidationHint.getStyleClass().add("success-text");
                }
                return;
            }

            lblValidationHint.setText(UiText.text("Password must be at least 6 characters long with letters and numbers. Confirmation must match."));
            lblValidationHint.getStyleClass().remove("success-text");
            if (!lblValidationHint.getStyleClass().contains("error-text")) {
                lblValidationHint.getStyleClass().add("error-text");
            }
        }
    }

    private boolean validateBeforeSubmit(String username,
                                         String fullName,
                                         String email,
                                         String password,
                                         String confirmPassword,
                                         Role role) {
        boolean valid = true;

        if (username.isEmpty()) {
            applyErrorState(txtUsername);
            valid = false;
        }
        if (fullName.isEmpty()) {
            applyErrorState(txtFullName);
            valid = false;
        }
        if (email.isEmpty() || !UserValidator.isValidEmail(email)) {
            applyErrorState(txtEmail);
            valid = false;
        }
        if (password.isEmpty() || !UserValidator.isValidPassword(password)) {
            applyErrorState(txtPassword);
            valid = false;
        }
        if (confirmPassword.isEmpty() || !confirmPassword.equals(password)) {
            applyErrorState(txtConfirmPassword);
            valid = false;
        }
        if (role == null || role == Role.ADMIN) {
            valid = false;
        }

        if (valid) {
            return true;
        }

        String message = "Please review the registration information.";
        if (email.isEmpty() || !UserValidator.isValidEmail(email)) {
            message = "Invalid email.";
        } else if (password.isEmpty() || !UserValidator.isValidPassword(password)) {
            message = "Password must be at least 6 characters long and include letters and numbers.";
        } else if (confirmPassword.isEmpty() || !confirmPassword.equals(password)) {
            message = "Password confirmation does not match.";
        } else if (role == null) {
            message = "Invalid role.";
        } else if (role == Role.ADMIN) {
            message = "Admin accounts cannot be created from the registration screen.";
        } else if (username.isEmpty() || fullName.isEmpty()) {
            message = "Please fill in all required information.";
        }

        showInlineError(message);
        return false;
    }

    private void updateRequiredState(Control control) {
        if (control == null) {
            return;
        }
        String value = control instanceof TextField textField ? textField.getText() : "";
        if (value == null || value.isBlank()) {
            clearFieldState(control);
            return;
        }
        applyValidState(control);
    }

    private void updateOptionalValidatedState(Control control, boolean valid) {
        if (control == null) {
            return;
        }
        String value = control instanceof TextField textField ? textField.getText() : "";
        if (value == null || value.isBlank()) {
            clearFieldState(control);
            return;
        }
        if (valid) {
            applyValidState(control);
        } else {
            applyErrorState(control);
        }
    }

    private void showInlineError(String message) {
        if (lblStatus == null) {
            return;
        }
        lblStatus.setText(UiText.text(message == null ? "" : message));
        lblStatus.setManaged(true);
        lblStatus.setVisible(true);
        UiEffects.shake(registerCard);
    }

    private void hideStatus() {
        if (lblStatus == null) {
            return;
        }
        lblStatus.setManaged(false);
        lblStatus.setVisible(false);
        lblStatus.setText("");
    }

    private void applyErrorState(Control control) {
        if (control == null) {
            return;
        }
        control.getStyleClass().remove(INPUT_VALID);
        if (!control.getStyleClass().contains(INPUT_ERROR)) {
            control.getStyleClass().add(INPUT_ERROR);
        }
    }

    private void applyValidState(Control control) {
        if (control == null) {
            return;
        }
        control.getStyleClass().remove(INPUT_ERROR);
        if (!control.getStyleClass().contains(INPUT_VALID)) {
            control.getStyleClass().add(INPUT_VALID);
        }
    }

    private void clearFieldState(Control... controls) {
        for (Control control : controls) {
            if (control == null) {
                continue;
            }
            control.getStyleClass().removeAll(INPUT_ERROR, INPUT_VALID);
        }
    }

    private void setBusy(boolean busy) {
        if (registerCard != null) {
            registerCard.setDisable(busy);
        }
    }
}
