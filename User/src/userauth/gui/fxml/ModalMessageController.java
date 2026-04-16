package userauth.gui.fxml;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class ModalMessageController {
    @FXML
    private Label lblTitle;

    @FXML
    private Label lblMessage;

    @FXML
    private Button btnPrimary;

    @FXML
    private Button btnSecondary;

    private Stage dialogStage;
    private boolean confirmed;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void configure(String title,
                          String message,
                          String primaryText,
                          String primaryStyleClass,
                          String secondaryText,
                          boolean showSecondary) {
        lblTitle.setText(title == null || title.isBlank() ? "THONG BAO" : title);
        lblMessage.setText(message == null ? "" : message);

        btnPrimary.setText(primaryText == null || primaryText.isBlank() ? "DONG" : primaryText);
        btnPrimary.getStyleClass().setAll("button", primaryStyleClass == null || primaryStyleClass.isBlank()
                ? "primary-button"
                : primaryStyleClass);

        btnSecondary.setManaged(showSecondary);
        btnSecondary.setVisible(showSecondary);
        if (showSecondary) {
            btnSecondary.setText(secondaryText == null || secondaryText.isBlank() ? "HUY" : secondaryText);
            btnSecondary.getStyleClass().setAll("button", "ghost-button");
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    @FXML
    private void handlePrimary() {
        confirmed = true;
        closeDialog();
    }

    @FXML
    private void handleSecondary() {
        confirmed = false;
        closeDialog();
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
}
