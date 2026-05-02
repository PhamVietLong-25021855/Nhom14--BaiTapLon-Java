package userauth.gui.fxml;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import userauth.controller.WalletController;
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
    private Consumer<String> successHandler = message -> {};
    private boolean submitInProgress;

    @FXML
    private void initialize() {
        hideError();
        cbPaymentMethod.getItems().addAll(PaymentMethod.values());
        cbPaymentMethod.setValue(PaymentMethod.CREDIT_CARD);
        cbPaymentMethod.setConverter(new javafx.util.StringConverter<PaymentMethod>() {
            @Override
            public String toString(PaymentMethod method) {
                if (method == null) return "";
                return switch (method) {
                    case CREDIT_CARD -> UiText.text("Credit Card");
                    case BANK_TRANSFER -> UiText.text("Bank Transfer");
                    case E_WALLET -> UiText.text("E-Wallet");
                    case CASH -> UiText.text("Cash");
                };
            }

            @Override
            public PaymentMethod fromString(String string) {
                return null; // Not needed for display-only
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
        this.successHandler = Objects.requireNonNullElse(successHandler, message -> {});
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
            amount = Double.parseDouble(amountText);
            if (amount <= 0) {
                showError(UiText.text("Amount must be greater than 0."));
                return;
            }
        } catch (NumberFormatException e) {
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
                    if (result.startsWith("SUCCESS:")) {
                        String transactionId = result.substring("SUCCESS: Transaction ID ".length());

                        closeDialog(); // Đóng dialog trước khi hiển thị thông báo thành công để tránh lỗi focus

                        successHandler.accept(UiText.text("Top-up request created successfully. Transaction ID: ") + transactionId);
                        return;
                    }
                    NotificationUtil.error(dialogStage.getOwner(), "Lỗi Nạp Tiền", result);
                    showError(result);
                },
                error -> {
                    submitInProgress = false;
                    setBusy(false);
                    showError(UiText.text("Unable to process top-up request right now."));
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
