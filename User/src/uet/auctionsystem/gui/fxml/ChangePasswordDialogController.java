package uet.auctionsystem.gui.fxml;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import uet.auctionsystem.controller.AuthController;
import uet.auctionsystem.model.User;
import java.util.Objects;
import java.util.function.Consumer;

// Ghi chu file: File controller JavaFX; dieu khien hanh vi cua man hinh, hop thoai hoac thanh phan UI.
// Khai bao lop ChangePasswordDialogController; dieu khien mot man hinh hoac thanh phan JavaFX cu the.
public class ChangePasswordDialogController {
    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl username.
    private Label lblUsername;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho txt old password.
    private PasswordField txtOldPassword;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho txt new password.
    private PasswordField txtNewPassword;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl error.
    private Label lblError;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho dialog stage.
    private Stage dialogStage;
    // Thuoc tinh: giu tham chieu den AuthController de phoi hop xu ly.
    private AuthController authController;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho user.
    private User user;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho success handler.
    private Consumer<String> successHandler = message -> {};
    // Thuoc tinh: luu trang thai hoac du lieu tam cho submit in progress.
    private boolean submitInProgress;

    @FXML
    // Phuong thuc: khoi dong hoac khoi tao tien trinh initialize.
    private void initialize() {
        hideError();
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set dialog stage.
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set auth controller.
    public void setAuthController(AuthController authController) {
        this.authController = authController;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set user.
    public void setUser(User user) {
        this.user = user;
        lblUsername.setText(user == null ? UiText.text("Account: -") : UiText.text("Account:") + " " + user.getUsername());
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set success handler.
    public void setSuccessHandler(Consumer<String> successHandler) {
        this.successHandler = Objects.requireNonNullElse(successHandler, message -> {});
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle submit.
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
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle cancel.
    private void handleCancel() {
        closeDialog();
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac show error.
    private void showError(String message) {
        lblError.setText(UiText.text(message == null ? "" : message));
        lblError.setManaged(true);
        lblError.setVisible(true);
    }
    // Phuong thuc: thuc hien chuc nang hide error trong lop ChangePasswordDialogController.
    private void hideError() {
        lblError.setManaged(false);
        lblError.setVisible(false);
        lblError.setText("");
    }
    // Phuong thuc: huy, xoa, dong hoac don trang thai cho thao tac close dialog.
    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set busy.
    private void setBusy(boolean busy) {
        if (txtOldPassword != null) {
            txtOldPassword.setDisable(busy);
        }
        if (txtNewPassword != null) {
            txtNewPassword.setDisable(busy);
        }
    }
}
