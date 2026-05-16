package uet.auctionsystem.gui.fxml;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

// Ghi chu file: File controller JavaFX; dieu khien hanh vi cua man hinh, hop thoai hoac thanh phan UI.
// Khai bao lop ToastNotificationController; dieu khien mot man hinh hoac thanh phan JavaFX cu the.
public class ToastNotificationController {
    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho toast root.
    private HBox toastRoot;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl toast title.
    private Label lblToastTitle;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl toast message.
    private Label lblToastMessage;
    // Phuong thuc: thuc hien chuc nang configure trong lop ToastNotificationController.
    public void configure(String title, String message, String toneStyleClass) {
        lblToastTitle.setText(UiText.text(title == null || title.isBlank() ? "Notification" : title));
        lblToastMessage.setText(UiText.text(message == null ? "" : message));

        toastRoot.getStyleClass().removeAll(
                "toast-success",
                "toast-info",
                "toast-warning",
                "toast-error"
        );
        if (toneStyleClass != null && !toneStyleClass.isBlank()) {
            toastRoot.getStyleClass().add(toneStyleClass);
        }
    }
}
