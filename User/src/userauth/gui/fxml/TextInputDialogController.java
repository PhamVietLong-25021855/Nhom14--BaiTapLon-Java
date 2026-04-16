package userauth.gui.fxml;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class TextInputDialogController {
    @FXML
    private Label lblTitle;

    @FXML
    private Label lblMessage;

    @FXML
    private TextField txtInput;

    @FXML
    private Label lblError;

    @FXML
    private Button btnPrimary;

    @FXML
    private Button btnSecondary;

    private Stage dialogStage;
    private String inputValue;

    @FXML
    private void initialize() {
        hideError();
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void configure(String title,
                          String message,
                          String defaultValue,
                          String primaryText,
                          String secondaryText,
                          String primaryStyleClass) {
        lblTitle.setText(title == null || title.isBlank() ? "NHAP DU LIEU" : title);
        lblMessage.setText(message == null ? "" : message);
        txtInput.setText(defaultValue == null ? "" : defaultValue);
        txtInput.positionCaret(txtInput.getText().length());
        btnPrimary.setText(primaryText == null || primaryText.isBlank() ? "XAC NHAN" : primaryText);
        btnPrimary.getStyleClass().setAll("button", primaryStyleClass == null || primaryStyleClass.isBlank()
                ? "primary-button"
                : primaryStyleClass);
        btnSecondary.setText(secondaryText == null || secondaryText.isBlank() ? "HUY" : secondaryText);
        btnSecondary.getStyleClass().setAll("button", "ghost-button");
        hideError();
    }

    public String getInputValue() {
        return inputValue;
    }

    public void requestInputFocus() {
        txtInput.requestFocus();
    }

    public void showError(String message) {
        lblError.setText(message == null ? "" : message);
        lblError.setManaged(true);
        lblError.setVisible(true);
    }

    @FXML
    private void handlePrimary() {
        inputValue = txtInput.getText() == null ? "" : txtInput.getText().trim();
        closeDialog();
    }

    @FXML
    private void handleSecondary() {
        inputValue = null;
        closeDialog();
    }

    private void hideError() {
        lblError.setManaged(false);
        lblError.setVisible(false);
        lblError.setText("");
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
}
