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

    public String getInputValue() {
        return inputValue;
    }

    public void requestInputFocus() {
        txtInput.requestFocus();
    }

    public void showError(String message) {
        lblError.setText(UiText.text(message == null ? "" : message));
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
