package userauth.gui.fxml;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

// Ghi chu file: File controller JavaFX; dieu khien hanh vi cua man hinh, hop thoai hoac thanh phan UI.
// Khai bao lop ModalMessageController; dieu khien mot man hinh hoac thanh phan JavaFX cu the.
public class ModalMessageController {
    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl title.
    private Label lblTitle;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl message.
    private Label lblMessage;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho btn primary.
    private Button btnPrimary;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho btn secondary.
    private Button btnSecondary;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho dialog stage.
    private Stage dialogStage;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho confirmed.
    private boolean confirmed;
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set dialog stage.
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
    // Phuong thuc: thuc hien chuc nang configure trong lop ModalMessageController.
    public void configure(String title,
                          String message,
                          String primaryText,
                          String primaryStyleClass,
                          String secondaryText,
                          boolean showSecondary) {
        lblTitle.setText(UiText.text(title == null || title.isBlank() ? "NOTIFICATION" : title));
        lblMessage.setText(UiText.text(message == null ? "" : message));

        btnPrimary.setText(UiText.text(primaryText == null || primaryText.isBlank() ? "CLOSE" : primaryText));
        btnPrimary.getStyleClass().setAll("button", primaryStyleClass == null || primaryStyleClass.isBlank()
                ? "primary-button"
                : primaryStyleClass);

        btnSecondary.setManaged(showSecondary);
        btnSecondary.setVisible(showSecondary);
        if (showSecondary) {
            btnSecondary.setText(UiText.text(secondaryText == null || secondaryText.isBlank() ? "CANCEL" : secondaryText));
            btnSecondary.getStyleClass().setAll("button", "ghost-button");
        }
    }
    // Phuong thuc: kiem tra dieu kien hoac xac thuc cho thao tac is confirmed.
    public boolean isConfirmed() {
        return confirmed;
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle primary.
    private void handlePrimary() {
        confirmed = true;
        closeDialog();
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle secondary.
    private void handleSecondary() {
        confirmed = false;
        closeDialog();
    }
    // Phuong thuc: huy, xoa, dong hoac don trang thai cho thao tac close dialog.
    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
}
