package userauth.gui.fxml;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HomeEmptyCardController {
    @FXML
    private Label lblTitle;

    @FXML
    private Label lblBody;

    public void setContent(String title, String body) {
        lblTitle.setText(title == null || title.isBlank() ? "Thong tin se cap nhat sau" : title);
        lblBody.setText(body == null ? "" : body);
    }
}
