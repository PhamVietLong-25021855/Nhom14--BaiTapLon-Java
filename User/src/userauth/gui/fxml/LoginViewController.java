package userauth.gui.fxml;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import userauth.controller.AuthController;
import userauth.exception.UnauthorizedException;
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
    private TextField txtVisiblePassword;

    @FXML
    private CheckBox chkRememberMe;

    @FXML
    private CheckBox chkShowPassword;

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
        initializePasswordVisibilityToggle();
        hideStatus();
        Platform.runLater(() -> UiEffects.playEntrance(authCard, 140, 24, 0));
    }

    @FXML
    private void handleLogin() {
        hideStatus();
        clearFieldState(txtUsername, txtPassword);

        if (authController == null) {
            showErrorState("AuthController has not been assigned to LoginViewController.");
            infoHandler.accept("AuthController has not been assigned to LoginViewController.");
            return;
        }

        String username = txtUsername.getText().trim();
        String password = getPasswordText();
        if (username.isEmpty() || password.isBlank()) {
            if (username.isEmpty()) {
                applyErrorState(txtUsername);
            }
            if (password.isBlank()) {
                applyErrorState(txtPassword);
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
                () -> {
                    try {
                        return authController.login(username, password);
                    } catch (UnauthorizedException ex) {
                        throw new IllegalStateException(ex.getMessage(), ex);
                    }
                },
                user -> {
                    loginInProgress = false;
                    setBusy(false);
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
        if (txtVisiblePassword != null) {
            txtVisiblePassword.clear();
        }
        if (chkShowPassword != null) {
            chkShowPassword.setSelected(false);
            updatePasswordVisibility(false);
        }
        if (chkRememberMe != null) {
            chkRememberMe.setSelected(true);
        }
        hideStatus();
        clearFieldState(txtUsername, txtPassword, txtVisiblePassword);
    }

    private void initializePasswordVisibilityToggle() {
        if (txtVisiblePassword == null || txtPassword == null) {
            return;
        }

        txtVisiblePassword.textProperty().bindBidirectional(txtPassword.textProperty());
        updatePasswordVisibility(chkShowPassword != null && chkShowPassword.isSelected());
        if (chkShowPassword != null) {
            chkShowPassword.selectedProperty().addListener((observable, oldValue, showPassword) ->
                    updatePasswordVisibility(showPassword));
        }
    }

    private String getPasswordText() {
        if (chkShowPassword != null && chkShowPassword.isSelected() && txtVisiblePassword != null) {
            return txtVisiblePassword.getText();
        }
        return txtPassword.getText();
    }

    private void updatePasswordVisibility(boolean showPassword) {
        if (txtVisiblePassword == null || txtPassword == null) {
            return;
        }

        txtPassword.setManaged(!showPassword);
        txtPassword.setVisible(!showPassword);
        txtVisiblePassword.setManaged(showPassword);
        txtVisiblePassword.setVisible(showPassword);
        if (showPassword) {
            txtVisiblePassword.requestFocus();
            txtVisiblePassword.positionCaret(txtVisiblePassword.getText().length());
        } else {
            txtPassword.requestFocus();
            txtPassword.positionCaret(txtPassword.getText().length());
        }
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
}
