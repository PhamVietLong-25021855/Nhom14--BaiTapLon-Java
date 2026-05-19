package userauth.gui.fxml.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import userauth.controller.WalletController;
import userauth.gui.fxml.shared.UiAsync;
import userauth.gui.fxml.shared.UiInput;
import userauth.gui.fxml.shared.UiText;
import userauth.model.PaymentMethod;
import userauth.model.User;

import java.util.Objects;
import java.util.function.Consumer;

public class TopUpDialogController {
    @FXML
    private Label lblUsername;

    @FXML
    private TextField txtAmount;

    @FXML
    private ComboBox<PaymentMethod> cbPaymentMethod;

    @FXML
    private Label lblError;

    private Stage dialogStage;
    private WalletController walletController;
    private User user;
    private Consumer<String> successHandler = ignored -> {};
    private boolean submitInProgress;

    @FXML
    private void initialize() {
        hideError();
        UiInput.installMoneyInput(txtAmount);
        cbPaymentMethod.getItems().setAll(PaymentMethod.values());
        cbPaymentMethod.setValue(PaymentMethod.BANK_TRANSFER);
        cbPaymentMethod.setConverter(new StringConverter<>() {
            @Override
            public String toString(PaymentMethod method) {
                if (method == null) {
                    return "";
                }
                return switch (method) {
                    case CREDIT_CARD -> UiText.text("Credit Card");
                    case BANK_TRANSFER -> UiText.text("Bank Transfer");
                    case E_WALLET -> UiText.text("E-Wallet");
                    case CASH -> UiText.text("Cash");
                };
            }

            @Override
            public PaymentMethod fromString(String string) {
                return null;
            }
        });
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setWalletController(WalletController walletController) {
        this.walletController = walletController;
    }

    public void setUser(User user) {
        this.user = user;
        lblUsername.setText(user == null ? UiText.text("Account: -") : UiText.text("Account:") + " " + user.getUsername());
    }

    public void setSuccessHandler(Consumer<String> successHandler) {
        this.successHandler = Objects.requireNonNullElse(successHandler, ignored -> {});
    }

    @FXML
    private void handleSubmit() {
        hideError();
        if (walletController == null || user == null) {
            showError(UiText.text("Not enough information to process top-up."));
            return;
        }

        String amountText = txtAmount.getText() == null ? "" : txtAmount.getText().trim();
        PaymentMethod method = cbPaymentMethod.getValue();
        if (amountText.isEmpty()) {
            showError(UiText.text("Please enter the top-up amount."));
            return;
        }
        if (method == null) {
            showError(UiText.text("Please select a payment method."));
            return;
        }

        double amount;
        try {
            amount = UiInput.parsePositiveDecimal(amountText, "Top-up amount");
        } catch (NumberFormatException ex) {
            showError(UiText.text("Invalid amount format."));
            return;
        }

        if (submitInProgress) {
            return;
        }

        submitInProgress = true;
        setBusy(true);
        UiAsync.run(
                () -> walletController.createTopUpRequest(user.getId(), amount, method),
                result -> {
                    submitInProgress = false;
                    setBusy(false);
                    if (result != null && result.startsWith("SUCCESS: Transaction ID ")) {
                        String transactionId = result.substring("SUCCESS: Transaction ID ".length()).trim();
                        closeDialog();
                        successHandler.accept(UiText.text("Top-up successful. Transaction ID: ") + transactionId);
                        return;
                    }
                    showError(result);
                },
                error -> {
                    submitInProgress = false;
                    setBusy(false);
                    showError(UiText.text("Unable to process the top-up request right now."));
                }
        );
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void showError(String message) {
        lblError.setText(UiText.text(message == null ? "" : message));
        lblError.setManaged(true);
        lblError.setVisible(true);
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

    private void setBusy(boolean busy) {
        if (txtAmount != null) {
            txtAmount.setDisable(busy);
        }
        if (cbPaymentMethod != null) {
            cbPaymentMethod.setDisable(busy);
        }
    }
}
