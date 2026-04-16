package userauth.gui.fxml;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import userauth.controller.AuthController;
import userauth.exception.UnauthorizedException;
import userauth.model.User;

import java.util.Objects;
import java.util.function.Consumer;

public class LoginViewController {
    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    private AuthController authController;
    private Runnable showHomeHandler = () -> {};
    private Runnable showRegisterHandler = () -> {};
    private Consumer<User> loginSuccessHandler = user -> {};
    private Consumer<String> infoHandler = message -> NotificationUtil.info(null, "Thong bao", message);
    private Consumer<String> errorHandler = message -> NotificationUtil.error(null, "Dang nhap that bai", message);

    @FXML
    private void handleLogin() {
        if (authController == null) {
            infoHandler.accept("Chua gan AuthController cho LoginViewController.");
            return;
        }

        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();

        try {
            User user = authController.login(username, password);
            clearInputs();
            loginSuccessHandler.accept(user);
        } catch (UnauthorizedException ex) {
            errorHandler.accept(ex.getMessage());
        }
    }

    @FXML
    private void handleShowRegister() {
        showRegisterHandler.run();
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

    public void setShowRegisterHandler(Runnable showRegisterHandler) {
        this.showRegisterHandler = Objects.requireNonNullElse(showRegisterHandler, () -> {});
    }

    public void setLoginSuccessHandler(Consumer<User> loginSuccessHandler) {
        this.loginSuccessHandler = Objects.requireNonNullElse(loginSuccessHandler, user -> {});
    }

    public void setInfoHandler(Consumer<String> infoHandler) {
        this.infoHandler = Objects.requireNonNullElse(infoHandler, message -> NotificationUtil.info(null, "Thong bao", message));
    }

    public void setErrorHandler(Consumer<String> errorHandler) {
        this.errorHandler = Objects.requireNonNullElse(errorHandler, message -> NotificationUtil.error(null, "Dang nhap that bai", message));
    }

    private void clearInputs() {
        txtUsername.clear();
        txtPassword.clear();
    }
}
