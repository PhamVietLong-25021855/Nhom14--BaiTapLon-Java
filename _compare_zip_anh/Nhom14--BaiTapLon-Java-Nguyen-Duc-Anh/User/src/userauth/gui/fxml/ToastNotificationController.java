package userauth.gui.fxml;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class ToastNotificationController {
    @FXML
    private HBox toastRoot;

    @FXML
    private Label lblToastTitle;

    @FXML
    private Label lblToastMessage;

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
