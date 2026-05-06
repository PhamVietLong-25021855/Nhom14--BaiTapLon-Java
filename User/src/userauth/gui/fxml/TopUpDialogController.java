package userauth.gui.fxml;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import userauth.controller.WalletController;
import userauth.model.PaymentMethod;
import userauth.model.User;
import java.util.Objects;
import java.util.function.Consumer;

// Ghi chu file: File controller JavaFX; dieu khien hanh vi cua man hinh, hop thoai hoac thanh phan UI.
// Khai bao lop TopUpDialogController; dieu khien mot man hinh hoac thanh phan JavaFX cu the.
public class TopUpDialogController {
    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl username.
    private Label lblUsername;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho txt amount.
    private TextField txtAmount;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho cb payment method.
    private ComboBox<PaymentMethod> cbPaymentMethod;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl error.
    private Label lblError;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho dialog stage.
    private Stage dialogStage;
    // Thuoc tinh: giu tham chieu den WalletController de phoi hop xu ly.
    private WalletController walletController;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho user.
    private User user;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho success handler.
    private Consumer<String> successHandler = message -> {
    };
    // Thuoc tinh: luu trang thai hoac du lieu tam cho submit in progress.
    private boolean submitInProgress;

    @FXML
    // Phuong thuc: khoi dong hoac khoi tao tien trinh initialize.
    private void initialize() {
        hideError();
        cbPaymentMethod.getItems().setAll(PaymentMethod.values());
        cbPaymentMethod.setValue(PaymentMethod.CREDIT_CARD);
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
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set dialog stage.
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set wallet controller.
    public void setWalletController(WalletController walletController) {
        this.walletController = walletController;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set user.
    public void setUser(User user) {
        this.user = user;
        lblUsername.setText(user == null ? UiText.text("Account: -") : UiText.text("Account:") + " " + user.getUsername());
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set success handler.
    public void setSuccessHandler(Consumer<String> successHandler) {
        this.successHandler = Objects.requireNonNullElse(successHandler, message -> {
        });
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle submit.
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
        } catch (NumberFormatException ex) {
            showError(UiText.text("Invalid amount format."));
            return;
        }
        if (amount <= 0) {
            showError(UiText.text("Amount must be greater than 0."));
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
                    if (result.startsWith("SUCCESS: Transaction ID ")) {
                        String transactionId = result.substring("SUCCESS: Transaction ID ".length());
                        closeDialog();
                        successHandler.accept(UiText.text("Top-up request created successfully. Transaction ID: ") + transactionId);
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
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle cancel.
    private void handleCancel() {
        closeDialog();
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac show error.
    private void showError(String message) {
        lblError.setText(UiText.text(message == null ? "" : message));
        lblError.setManaged(true);
        lblError.setVisible(true);
    }
    // Phuong thuc: thuc hien chuc nang hide error trong lop TopUpDialogController.
    private void hideError() {
        lblError.setManaged(false);
        lblError.setVisible(false);
        lblError.setText("");
    }
    // Phuong thuc: huy, xoa, dong hoac don trang thai cho thao tac close dialog.
    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set busy.
    private void setBusy(boolean busy) {
        if (txtAmount != null) {
            txtAmount.setDisable(busy);
        }
        if (cbPaymentMethod != null) {
            cbPaymentMethod.setDisable(busy);
        }
    }
}
