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

// Ghi chu file: File controller JavaFX; dieu khien hanh vi cua man hinh, hop thoai hoac thanh phan UI.
// Khai bao lop RegisterViewController; dieu khien mot man hinh hoac thanh phan JavaFX cu the.
public class RegisterViewController {
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho error.
    private static final String INPUT_ERROR = "input-error";
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho valid.
    private static final String INPUT_VALID = "input-valid";

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho txt username.
    private TextField txtUsername;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho txt full name.
    private TextField txtFullName;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho txt email.
    private TextField txtEmail;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho txt password.
    private PasswordField txtPassword;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho txt confirm password.
    private PasswordField txtConfirmPassword;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho cb role.
    private ComboBox<String> cbRole;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl status.
    private Label lblStatus;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl validation hint.
    private Label lblValidationHint;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho register card.
    private VBox registerCard;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho field container.
    private VBox fieldContainer;
    // Thuoc tinh: giu tham chieu den AuthController de phoi hop xu ly.
    private AuthController authController;
    private Runnable showHomeHandler = () -> {};
    private Runnable backToLoginHandler = () -> {};
    private Consumer<String> successHandler = message -> NotificationUtil.success(null, "Notification", message);
    private Consumer<String> warningHandler = message -> NotificationUtil.warning(null, "Notification", message);
    private Consumer<String> errorHandler = message -> NotificationUtil.error(null, "Error", message);
    // Thuoc tinh: luu trang thai hoac du lieu tam cho register in progress.
    private boolean registerInProgress;

    @FXML
    // Phuong thuc: khoi dong hoac khoi tao tien trinh initialize.
    private void initialize() {
        if (cbRole.getItems().isEmpty()) {
            cbRole.getItems().addAll(Role.BIDDER.name(), Role.SELLER.name());
        }
        if (cbRole.getValue() == null) {
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
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle register.
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
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle back to login.
    private void handleBackToLogin() {
        backToLoginHandler.run();
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle show home.
    private void handleShowHome() {
        showHomeHandler.run();
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set auth controller.
    public void setAuthController(AuthController authController) {
        this.authController = authController;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set show home handler.
    public void setShowHomeHandler(Runnable showHomeHandler) {
        this.showHomeHandler = Objects.requireNonNullElse(showHomeHandler, () -> {});
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set back to login handler.
    public void setBackToLoginHandler(Runnable backToLoginHandler) {
        this.backToLoginHandler = Objects.requireNonNullElse(backToLoginHandler, () -> {});
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set success handler.
    public void setSuccessHandler(Consumer<String> successHandler) {
        this.successHandler = Objects.requireNonNullElse(successHandler, message -> NotificationUtil.success(null, "Notification", message));
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set warning handler.
    public void setWarningHandler(Consumer<String> warningHandler) {
        this.warningHandler = Objects.requireNonNullElse(warningHandler, message -> NotificationUtil.warning(null, "Notification", message));
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set error handler.
    public void setErrorHandler(Consumer<String> errorHandler) {
        this.errorHandler = Objects.requireNonNullElse(errorHandler, message -> NotificationUtil.error(null, "Error", message));
    }
    // Phuong thuc: bien doi du lieu cho thao tac parse role.
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
    // Phuong thuc: huy, xoa, dong hoac don trang thai cho thao tac clear inputs.
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
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac register validation listeners.
    private void registerValidationListeners() {
        txtUsername.textProperty().addListener((observable, oldValue, newValue) -> updateLiveValidation());
        txtFullName.textProperty().addListener((observable, oldValue, newValue) -> updateLiveValidation());
        txtEmail.textProperty().addListener((observable, oldValue, newValue) -> updateLiveValidation());
        txtPassword.textProperty().addListener((observable, oldValue, newValue) -> updateLiveValidation());
        txtConfirmPassword.textProperty().addListener((observable, oldValue, newValue) -> updateLiveValidation());
        cbRole.valueProperty().addListener((observable, oldValue, newValue) -> updateLiveValidation());
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac update live validation.
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
    // Phuong thuc: kiem tra dieu kien hoac xac thuc cho thao tac validate before submit.
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
        if (role == null) {
            valid = false;
        } else if (role == Role.ADMIN) {
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
            message = "Admin accounts cannot be created from registration.";
        } else if (username.isEmpty() || fullName.isEmpty()) {
            message = "Please fill in all required information.";
        }

        showInlineError(message);
        return false;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac update required state.
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
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac update optional validated state.
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
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac show inline error.
    private void showInlineError(String message) {
        if (lblStatus == null) {
            return;
        }
        lblStatus.setText(UiText.text(message == null ? "" : message));
        lblStatus.setManaged(true);
        lblStatus.setVisible(true);
        UiEffects.shake(registerCard);
    }
    // Phuong thuc: thuc hien chuc nang hide status trong lop RegisterViewController.
    private void hideStatus() {
        if (lblStatus == null) {
            return;
        }
        lblStatus.setManaged(false);
        lblStatus.setVisible(false);
        lblStatus.setText("");
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac apply error state.
    private void applyErrorState(Control control) {
        if (control == null) {
            return;
        }
        control.getStyleClass().remove(INPUT_VALID);
        if (!control.getStyleClass().contains(INPUT_ERROR)) {
            control.getStyleClass().add(INPUT_ERROR);
        }
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac apply valid state.
    private void applyValidState(Control control) {
        if (control == null) {
            return;
        }
        control.getStyleClass().remove(INPUT_ERROR);
        if (!control.getStyleClass().contains(INPUT_VALID)) {
            control.getStyleClass().add(INPUT_VALID);
        }
    }
    // Phuong thuc: huy, xoa, dong hoac don trang thai cho thao tac clear field state.
    private void clearFieldState(Control... controls) {
        for (Control control : controls) {
            if (control == null) {
                continue;
            }
            control.getStyleClass().removeAll(INPUT_ERROR, INPUT_VALID);
        }
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set busy.
    private void setBusy(boolean busy) {
        if (registerCard != null) {
            registerCard.setDisable(busy);
        }
    }
}
