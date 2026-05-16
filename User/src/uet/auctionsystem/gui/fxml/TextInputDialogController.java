package uet.auctionsystem.gui.fxml;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

// Ghi chu file: File controller JavaFX; dieu khien hanh vi cua man hinh, hop thoai hoac thanh phan UI.
// Khai bao lop TextInputDialogController; dieu khien mot man hinh hoac thanh phan JavaFX cu the.
public class TextInputDialogController {
    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl title.
    private Label lblTitle;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl message.
    private Label lblMessage;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho txt input.
    private TextField txtInput;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl error.
    private Label lblError;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho btn primary.
    private Button btnPrimary;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho btn secondary.
    private Button btnSecondary;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho dialog stage.
    private Stage dialogStage;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho input value.
    private String inputValue;

    @FXML
    // Phuong thuc: khoi dong hoac khoi tao tien trinh initialize.
    private void initialize() {
        hideError();
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set dialog stage.
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
    // Phuong thuc: thuc hien chuc nang configure trong lop TextInputDialogController.
    public void configure(String title,
                          String message,
                          String defaultValue,
                          String primaryText,
                          String secondaryText,
                          String primaryStyleClass) {
        lblTitle.setText(UiText.text(title == null || title.isBlank() ? "INPUT" : title));
        lblMessage.setText(UiText.text(message == null ? "" : message));
        txtInput.setText(defaultValue == null ? "" : defaultValue);
        txtInput.positionCaret(txtInput.getText().length());
        btnPrimary.setText(UiText.text(primaryText == null || primaryText.isBlank() ? "CONFIRM" : primaryText));
        btnPrimary.getStyleClass().setAll("button", primaryStyleClass == null || primaryStyleClass.isBlank()
                ? "primary-button"
                : primaryStyleClass);
        btnSecondary.setText(UiText.text(secondaryText == null || secondaryText.isBlank() ? "CANCEL" : secondaryText));
        btnSecondary.getStyleClass().setAll("button", "ghost-button");
        hideError();
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get input value.
    public String getInputValue() {
        return inputValue;
    }
    // Phuong thuc: thuc hien chuc nang request input focus trong lop TextInputDialogController.
    public void requestInputFocus() {
        txtInput.requestFocus();
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac show error.
    public void showError(String message) {
        lblError.setText(UiText.text(message == null ? "" : message));
        lblError.setManaged(true);
        lblError.setVisible(true);
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle primary.
    private void handlePrimary() {
        inputValue = txtInput.getText() == null ? "" : txtInput.getText().trim();
        closeDialog();
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle secondary.
    private void handleSecondary() {
        inputValue = null;
        closeDialog();
    }
    // Phuong thuc: thuc hien chuc nang hide error trong lop TextInputDialogController.
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
}
