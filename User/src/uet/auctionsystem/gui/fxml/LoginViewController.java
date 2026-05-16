package uet.auctionsystem.gui.fxml;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import uet.auctionsystem.controller.AuthController;
import uet.auctionsystem.exception.UnauthorizedException;
import uet.auctionsystem.model.User;
import java.util.Objects;
import java.util.function.Consumer;

// Ghi chu file: File controller JavaFX; dieu khien hanh vi cua man hinh, hop thoai hoac thanh phan UI.
// Khai bao lop LoginViewController; dieu khien mot man hinh hoac thanh phan JavaFX cu the.
public class LoginViewController {
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho error.
    private static final String INPUT_ERROR = "input-error";

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho txt username.
    private TextField txtUsername;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho txt password.
    private PasswordField txtPassword;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho txt visible password.
    private TextField txtVisiblePassword;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho chk remember me.
    private CheckBox chkRememberMe;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho chk show password.
    private CheckBox chkShowPassword;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl status.
    private Label lblStatus;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho auth card.
    private VBox authCard;
    // Thuoc tinh: giu tham chieu den AuthController de phoi hop xu ly.
    private AuthController authController;
    private Runnable showHomeHandler = () -> {};
    private Runnable showRegisterHandler = () -> {};
    // Thuoc tinh: luu trang thai hoac du lieu tam cho login success handler.
    private Consumer<User> loginSuccessHandler = user -> {};
    private Consumer<String> infoHandler = message -> NotificationUtil.info(null, "Notification", message);
    private Consumer<String> errorHandler = message -> NotificationUtil.error(null, "Login failed", message);
    // Thuoc tinh: luu trang thai hoac du lieu tam cho login in progress.
    private boolean loginInProgress;

    @FXML
    // Phuong thuc: khoi dong hoac khoi tao tien trinh initialize.
    private void initialize() {
        initializePasswordVisibilityToggle();
        hideStatus();
        Platform.runLater(() -> UiEffects.playEntrance(authCard, 140, 24, 0));
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle login.
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
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle show register.
    private void handleShowRegister() {
        showRegisterHandler.run();
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle show home.
    private void handleShowHome() {
        showHomeHandler.run();
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle forgot password.
    private void handleForgotPassword() {
        showErrorState("A dedicated password recovery flow is not available in this version.");
        infoHandler.accept("Please contact an admin for password assistance.");
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set auth controller.
    public void setAuthController(AuthController authController) {
        this.authController = authController;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set show home handler.
    public void setShowHomeHandler(Runnable showHomeHandler) {
        this.showHomeHandler = Objects.requireNonNullElse(showHomeHandler, () -> {});
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set show register handler.
    public void setShowRegisterHandler(Runnable showRegisterHandler) {
        this.showRegisterHandler = Objects.requireNonNullElse(showRegisterHandler, () -> {});
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set login success handler.
    public void setLoginSuccessHandler(Consumer<User> loginSuccessHandler) {
        this.loginSuccessHandler = Objects.requireNonNullElse(loginSuccessHandler, user -> {});
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set info handler.
    public void setInfoHandler(Consumer<String> infoHandler) {
        this.infoHandler = Objects.requireNonNullElse(infoHandler, message -> NotificationUtil.info(null, "Notification", message));
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set error handler.
    public void setErrorHandler(Consumer<String> errorHandler) {
        this.errorHandler = Objects.requireNonNullElse(errorHandler, message -> NotificationUtil.error(null, "Login failed", message));
    }
    // Phuong thuc: huy, xoa, dong hoac don trang thai cho thao tac clear inputs.
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
    // Phuong thuc: khoi dong hoac khoi tao tien trinh initialize password visibility toggle.
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
    // Phuong thuc: lay hoac doc du lieu cho thao tac get password text.
    private String getPasswordText() {
        if (chkShowPassword != null && chkShowPassword.isSelected() && txtVisiblePassword != null) {
            return txtVisiblePassword.getText();
        }
        return txtPassword.getText();
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac update password visibility.
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
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac show error state.
    private void showErrorState(String message) {
        if (lblStatus == null) {
            return;
        }
        lblStatus.setText(UiText.text(message == null ? "" : message));
        lblStatus.setManaged(true);
        lblStatus.setVisible(true);
        UiEffects.shake(authCard);
    }
    // Phuong thuc: thuc hien chuc nang hide status trong lop LoginViewController.
    private void hideStatus() {
        if (lblStatus == null) {
            return;
        }
        lblStatus.setManaged(false);
        lblStatus.setVisible(false);
        lblStatus.setText("");
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac apply error state.
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
    // Phuong thuc: huy, xoa, dong hoac don trang thai cho thao tac clear field state.
    private void clearFieldState(Control... controls) {
        for (Control control : controls) {
            if (control == null) {
                continue;
            }
            control.getStyleClass().remove(INPUT_ERROR);
        }
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set busy.
    private void setBusy(boolean busy) {
        if (authCard != null) {
            authCard.setDisable(busy);
        }
    }
}
