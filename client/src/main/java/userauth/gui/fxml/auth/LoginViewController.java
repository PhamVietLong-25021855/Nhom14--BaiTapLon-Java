package userauth.gui.fxml.auth;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import userauth.controller.AuthController;
import userauth.exception.UnauthorizedException;
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
        hideStatus();
        Platform.runLater(() -> UiEffects.playEntrance(authCard, 140, 24, 0));
    }


    @FXML
    private void handleLogin() {
        // Tránh việc người dùng click liên tục khi đang xử lý đăng nhập
        if (loginInProgress) {
            return;
        }

        String username = txtUsername.getText() == null ? "" : txtUsername.getText().trim();
        String password = txtPassword.getText() == null ? "" : txtPassword.getText();

        // 1. Dọn dẹp trạng thái lỗi cũ trước khi gửi yêu cầu mới
        hideStatus();
        clearFieldState(txtUsername, txtPassword);

        // 2. Kiểm tra dữ liệu trống ngay tại Client
        if (username.isEmpty() || password.isEmpty()) {
            applyErrorState(txtUsername, txtPassword);
            showErrorState("Please enter both username and password.");
            return;
        }

        loginInProgress = true;

        // 3. Đẩy luồng kết nối Socket xuống nền thông qua UiAsync để không gây treo giao diện
        UiAsync.run(
                () -> authController.login(username, password),
                user -> {
                    loginInProgress = false;
                    if (user != null) {
                        // Đăng nhập thành công -> Chuyển màn hình dựa trên Role
                        loginSuccessHandler.accept(user);
                    } else {
                        // Trường hợp khẩn cấp nếu trả về object null
                        applyErrorState(txtUsername, txtPassword);
                        showErrorState("Invalid username or password.");
                    }
                },
                error -> {
                    loginInProgress = false;
                    // Đăng nhập thất bại (Sai pass, không tồn tại user, rớt mạng...)
                    applyErrorState(txtUsername, txtPassword);

                    // Lấy thông báo lỗi chi tiết từ Server ném ra qua Exception
                    String errorMessage = error.getMessage();
                    if (errorMessage == null || errorMessage.isBlank()) {
                        errorMessage = "Invalid username or password.";
                    }

                    // Hiển thị chữ đỏ lỗi lên nhãn lblStatus và thực hiện hiệu ứng rung form
                    showErrorState(errorMessage);
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
        if (chkRememberMe != null) {
            chkRememberMe.setSelected(true);
        }
        hideStatus();
        clearFieldState(txtUsername, txtPassword);
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
