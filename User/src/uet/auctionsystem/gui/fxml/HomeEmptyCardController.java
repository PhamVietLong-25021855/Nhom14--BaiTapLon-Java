package uet.auctionsystem.gui.fxml;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

// Ghi chu file: File controller JavaFX; dieu khien hanh vi cua man hinh, hop thoai hoac thanh phan UI.
// Khai bao lop HomeEmptyCardController; dieu khien mot man hinh hoac thanh phan JavaFX cu the.
public class HomeEmptyCardController {
    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl title.
    private Label lblTitle;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl body.
    private Label lblBody;
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set content.
    public void setContent(String title, String body) {
        String fallbackTitle = title == null || title.isBlank() ? "Information will be updated later" : title;
        lblTitle.setText(UiText.text(fallbackTitle));
        lblBody.setText(UiText.text(body == null ? "" : body));
    }
}
