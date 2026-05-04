package userauth.gui.fxml;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import userauth.model.AuctionItem;
import userauth.model.BidTransaction;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BidHistoryDialogController {
    private static final DateTimeFormatter BID_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @FXML
    private Label lblAuctionName;

    @FXML
    private Label lblSummary;

    @FXML
    private TextArea txtHistory;

    private Stage dialogStage;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setAuction(AuctionItem auctionItem) {
        lblAuctionName.setText(auctionItem == null
                ? UiText.text("Product") + ": -"
                : UiText.text("Product") + ": " + auctionItem.getName() + " | " + UiText.text("Category") + ": " + auctionItem.getCategory());
    }

    public void setBids(List<BidTransaction> bids) {
        int count = bids == null ? 0 : bids.size();
        lblSummary.setText(UiText.text("Total transactions") + ": " + count);

        if (bids == null || bids.isEmpty()) {
            txtHistory.setText(UiText.text("No bid transactions yet."));
            return;
        }

        StringBuilder history = new StringBuilder();
        for (BidTransaction bid : bids) {
            history.append(UiText.text("User ID"))
                    .append(": ")
                    .append(bid.getBidderId())
                    .append(" | ")
                    .append(UiText.text("Price"))
                    .append(": ")
                    .append(AuctionViewFormatter.formatMoney(bid.getAmount()))
                    .append(" | ")
                    .append(UiText.text("At"))
                    .append(": ")
                    .append(BID_TIME.format(Instant.ofEpochMilli(bid.getTimestamp()).atZone(ZoneId.systemDefault())))
                    .append(" | ")
                    .append(UiText.text("Status"))
                    .append(": ")
                    .append(UiText.text(bid.getStatus()))
                    .append('\n');
        }
        txtHistory.setText(history.toString());
        txtHistory.positionCaret(0);
    }

    @FXML
    private void handleClose() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
}
