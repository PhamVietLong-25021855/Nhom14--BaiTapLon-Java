package userauth.gui.fxml;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import userauth.controller.AuthController;
import userauth.model.Role;
import userauth.validation.UserValidator;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

public class RegisterViewController {
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

    private AuthController authController;
    private Runnable showHomeHandler = () -> {};
    private Runnable backToLoginHandler = () -> {};
    private Consumer<String> successHandler = message -> NotificationUtil.success(null, "Thong bao", message);
    private Consumer<String> warningHandler = message -> NotificationUtil.warning(null, "Thong bao", message);
    private Consumer<String> errorHandler = message -> NotificationUtil.error(null, "Loi", message);

    @FXML
    private void initialize() {
        if (cbRole.getItems().isEmpty()) {
            cbRole.getItems().addAll(Role.BIDDER.name(), Role.SELLER.name(), Role.ADMIN.name());
        }
        if (cbRole.getValue() == null) {
            cbRole.setValue(Role.BIDDER.name());
        }
    }

    @FXML
    private void handleRegister() {
        if (authController == null) {
            warningHandler.accept("Chua gan AuthController cho RegisterViewController.");
            return;
        }

        String username = txtUsername.getText().trim();
        String fullName = txtFullName.getText().trim();
        String email = txtEmail.getText().trim();
        String password = txtPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();
        Role role = parseRole(cbRole.getValue());

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || fullName.isEmpty()) {
            errorHandler.accept("Vui long nhap day du thong tin.");
            return;
        }

        if (!UserValidator.isValidEmail(email)) {
            errorHandler.accept("Email khong hop le.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            errorHandler.accept("Mat khau nhap lai khong khop.");
            return;
        }

        if (!UserValidator.isValidPassword(password)) {
            errorHandler.accept("Mat khau can it nhat 6 ky tu, gom chu va so.");
            return;
        }

        if (role == null) {
            errorHandler.accept("Vai tro khong hop le.");
            return;
        }

        String result = authController.registerGUI(username, password, fullName, email, role);
        if ("SUCCESS".equals(result) || result.toLowerCase(Locale.ROOT).contains("thanh cong")) {
            clearInputs();
            successHandler.accept("Dang ky thanh cong. Vui long dang nhap.");
            backToLoginHandler.run();
            return;
        }

        warningHandler.accept(result);
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
        this.successHandler = Objects.requireNonNullElse(successHandler, message -> NotificationUtil.success(null, "Thong bao", message));
    }

    public void setWarningHandler(Consumer<String> warningHandler) {
        this.warningHandler = Objects.requireNonNullElse(warningHandler, message -> NotificationUtil.warning(null, "Thong bao", message));
    }

    public void setErrorHandler(Consumer<String> errorHandler) {
        this.errorHandler = Objects.requireNonNullElse(errorHandler, message -> NotificationUtil.error(null, "Loi", message));
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
    }
}
