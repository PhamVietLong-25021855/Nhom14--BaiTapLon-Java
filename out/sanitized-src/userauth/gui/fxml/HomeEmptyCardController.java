package userauth.gui.fxml;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

// File note: Controller card trạng thái rỗng khi trang chủ chưa có dữ liệu phù hợp.
public class HomeEmptyCardController {
    @FXML
    private Label lblTitle;

    @FXML
    private Label lblBody;

    public void setContent(String title, String body) {
        String fallbackTitle = title == null || title.isBlank() ? "Information will be updated later" : title;
        lblTitle.setText(UiText.text(fallbackTitle));
        lblBody.setText(UiText.text(body == null ? "" : body));
    }
}

