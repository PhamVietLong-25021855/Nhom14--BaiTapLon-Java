package userauth.gui.fxml.auth;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import userauth.controller.AuthController;
import userauth.gui.fxml.shared.*;
import userauth.model.User;

import java.util.Objects;
import java.util.function.Consumer;

public class LoginViewController {
    private static final String INPUT_ERROR = "input-error";

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private TextField txtPasswordVisible;

    @FXML
    private CheckBox chkShowPassword;

    @FXML
    private CheckBox chkRememberMe;

    @FXML
    private Label lblStatus;

    @FXML
    private VBox authCard;

    private AuthController authController;
    private Runnable showHomeHandler = () -> {};
    private Runnable showRegisterHandler = () -> {};
    private Consumer<User> loginSuccessHandler = user -> {};
    private Consumer<String> infoHandler = message -> NotificationUtil.info(null, "Notification", message);
    private Consumer<String> errorHandler = message -> NotificationUtil.error(null, "Login failed", message);
    private boolean loginInProgress;

    @FXML
    private void initialize() {
        if (txtPassword != null && txtPasswordVisible != null) {
            txtPasswordVisible.textProperty().bindBidirectional(txtPassword.textProperty());
        }
        if (chkShowPassword != null) {
            chkShowPassword.selectedProperty().addListener((observable, oldValue, showPassword) -> setPasswordVisible(showPassword, true));
        }
        setPasswordVisible(false, false);
        hideStatus();
        Platform.runLater(() -> UiEffects.playEntrance(authCard, 140, 24, 0));
    }

    @FXML
    private void handleLogin() {
        hideStatus();
        clearFieldState(txtUsername, txtPassword, txtPasswordVisible);

        if (authController == null) {
            showErrorState("AuthController has not been assigned to LoginViewController.");
            infoHandler.accept("AuthController has not been assigned to LoginViewController.");
            return;
        }

        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();
        if (username.isEmpty() || password.isBlank()) {
            if (username.isEmpty()) {
                applyErrorState(txtUsername);
            }
            if (password.isBlank()) {
                applyErrorState(txtPassword);
                applyErrorState(txtPasswordVisible);
            }
            showErrorState("Please enter both username and password.");
            return;
        }

        if (loginInProgress) {
            return;
        }

        loginInProgress = true;
        setBusy(true);
        UiAsync.run(
                () -> authController.login(username, password),
                user -> {
                    loginInProgress = false;
                    setBusy(false);
                    if (user == null) {
                        applyErrorState(txtUsername, txtPassword);
                        applyErrorState(txtPasswordVisible);
                        showErrorState("Login failed.");
                        errorHandler.accept("Login failed.");
                        return;
                    }
                    hideStatus();
                    clearInputs();
                    loginSuccessHandler.accept(user);
                },
                error -> {
                    loginInProgress = false;
                    setBusy(false);
                    String message = error.getMessage() == null || error.getMessage().isBlank()
                            ? "Login failed."
                            : error.getMessage();
                    applyErrorState(txtUsername, txtPassword);
                    applyErrorState(txtPasswordVisible);
                    showErrorState(message);
                    errorHandler.accept(message);
                }
        );
    }

    @FXML
    private void handleShowRegister() {
        showRegisterHandler.run();
    }

    @FXML
    private void handleShowHome() {
        showHomeHandler.run();
    }

    @FXML
    private void handleForgotPassword() {
        showErrorState("A dedicated password recovery flow is not available in this version.");
        infoHandler.accept("Please contact an admin for password assistance.");
    }

    public void setAuthController(AuthController authController) {
        this.authController = authController;
    }

    public void setShowHomeHandler(Runnable showHomeHandler) {
        this.showHomeHandler = Objects.requireNonNullElse(showHomeHandler, () -> {});
    }

    public void setShowRegisterHandler(Runnable showRegisterHandler) {
        this.showRegisterHandler = Objects.requireNonNullElse(showRegisterHandler, () -> {});
    }

    public void setLoginSuccessHandler(Consumer<User> loginSuccessHandler) {
        this.loginSuccessHandler = Objects.requireNonNullElse(loginSuccessHandler, user -> {});
    }

    public void setInfoHandler(Consumer<String> infoHandler) {
        this.infoHandler = Objects.requireNonNullElse(infoHandler, message -> NotificationUtil.info(null, "Notification", message));
    }

    public void setErrorHandler(Consumer<String> errorHandler) {
        this.errorHandler = Objects.requireNonNullElse(errorHandler, message -> NotificationUtil.error(null, "Login failed", message));
    }

    private void clearInputs() {
        txtUsername.clear();
        txtPassword.clear();
        if (chkShowPassword != null) {
            chkShowPassword.setSelected(false);
        }
        if (chkRememberMe != null) {
            chkRememberMe.setSelected(true);
        }
        hideStatus();
        clearFieldState(txtUsername, txtPassword, txtPasswordVisible);
    }

    private void showErrorState(String message) {
        if (lblStatus == null) {
            return;
        }
        lblStatus.setText(UiText.text(message == null ? "" : message));
        lblStatus.setManaged(true);
        lblStatus.setVisible(true);
        UiEffects.shake(authCard);
    }

    private void hideStatus() {
        if (lblStatus == null) {
            return;
        }
        lblStatus.setManaged(false);
        lblStatus.setVisible(false);
        lblStatus.setText("");
    }

    private void applyErrorState(Control... controls) {
        for (Control control : controls) {
            if (control == null) {
                continue;
            }
            if (!control.getStyleClass().contains(INPUT_ERROR)) {
                control.getStyleClass().add(INPUT_ERROR);
            }
        }
    }

    private void clearFieldState(Control... controls) {
        for (Control control : controls) {
            if (control == null) {
                continue;
            }
            control.getStyleClass().remove(INPUT_ERROR);
        }
    }

    private void setBusy(boolean busy) {
        if (authCard != null) {
            authCard.setDisable(busy);
        }
    }

    private void setPasswordVisible(boolean visible, boolean requestFocus) {
        if (txtPassword == null || txtPasswordVisible == null) {
            return;
        }
        txtPassword.setVisible(!visible);
        txtPassword.setManaged(!visible);
        txtPasswordVisible.setVisible(visible);
        txtPasswordVisible.setManaged(visible);

        Control activeField = visible ? txtPasswordVisible : txtPassword;
        Control inactiveField = visible ? txtPassword : txtPasswordVisible;
        if (inactiveField.getStyleClass().contains(INPUT_ERROR) && !activeField.getStyleClass().contains(INPUT_ERROR)) {
            activeField.getStyleClass().add(INPUT_ERROR);
        }
        if (!inactiveField.getStyleClass().contains(INPUT_ERROR)) {
            activeField.getStyleClass().remove(INPUT_ERROR);
        }

        if (requestFocus) {
            Platform.runLater(() -> {
                activeField.requestFocus();
                if (activeField instanceof TextInputControl input) {
                    input.positionCaret(input.getText() == null ? 0 : input.getText().length());
                }
            });
        }
    }
}
