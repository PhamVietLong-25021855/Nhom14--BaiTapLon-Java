package userauth.gui.fxml.dialog;

import userauth.gui.fxml.shared.*;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;
import userauth.model.BidTransaction;
import userauth.model.User;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BidHistoryDialogController {
    private static final DateTimeFormatter BID_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @FXML
    private Label lblBidderName;

    @FXML
    private Label lblSummary;

    @FXML
    private TableView<TransactionRow> tableTransactions;

    @FXML
    private TableColumn<TransactionRow, String> colTransactionTime;

    @FXML
    private TableColumn<TransactionRow, String> colTransactionProduct;

    @FXML
    private TableColumn<TransactionRow, String> colTransactionCategory;

    @FXML
    private TableColumn<TransactionRow, String> colTransactionAmount;

    @FXML
    private TableColumn<TransactionRow, String> colTransactionStatus;

    @FXML
    private TableColumn<TransactionRow, String> colTransactionResult;

    @FXML
    private TableView<WonProductRow> tableWonProducts;

    @FXML
    private TableColumn<WonProductRow, String> colWonProduct;

    @FXML
    private TableColumn<WonProductRow, String> colWonCategory;

    @FXML
    private TableColumn<WonProductRow, String> colWonPrice;

    @FXML
    private TableColumn<WonProductRow, String> colWonStatus;

    @FXML
    private TableColumn<WonProductRow, String> colWonEndedAt;

    private Stage dialogStage;

    @FXML
    private void initialize() {
        colTransactionTime.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().time()));
        colTransactionProduct.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().product()));
        colTransactionCategory.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().category()));
        colTransactionAmount.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().amount()));
        colTransactionStatus.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().status()));
        colTransactionResult.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().result()));

        colWonProduct.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().product()));
        colWonCategory.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().category()));
        colWonPrice.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().price()));
        colWonStatus.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().status()));
        colWonEndedAt.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().endedAt()));

        tableTransactions.setPlaceholder(new Label(UiText.text("No bid transactions yet.")));
        tableWonProducts.setPlaceholder(new Label(UiText.text("No won products yet.")));
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setBidderHistory(User bidder, List<AuctionItem> auctions, List<BidTransaction> bids) {
        int bidderId = bidder == null ? -1 : bidder.getId();
        Map<Integer, AuctionItem> auctionsById = mapAuctionsById(auctions);
        List<BidTransaction> bidderBids = safeBids(bids).stream()
                .filter(bid -> bid.getBidderId() == bidderId)
                .sorted(Comparator.comparingLong(BidTransaction::getTimestamp).reversed())
                .toList();
        List<AuctionItem> wonAuctions = safeAuctions(auctions).stream()
                .filter(item -> item.getWinnerId() == bidderId)
                .filter(this::isWonAuction)
                .sorted(Comparator.comparingLong(AuctionItem::getEndTime).reversed())
                .toList();

        lblBidderName.setText(formatBidderName(bidder));
        lblSummary.setText(UiText.text("Total transactions") + ": " + bidderBids.size()
                + " | " + UiText.text("Won products") + ": " + wonAuctions.size());

        tableTransactions.setItems(FXCollections.observableArrayList(
                bidderBids.stream()
                        .map(bid -> toTransactionRow(bid, auctionsById.get(bid.getAuctionId()), bidderId))
                        .toList()
        ));
        tableWonProducts.setItems(FXCollections.observableArrayList(
                wonAuctions.stream()
                        .map(this::toWonProductRow)
                        .toList()
        ));
    }

    @FXML
    private void handleClose() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    private Map<Integer, AuctionItem> mapAuctionsById(List<AuctionItem> auctions) {
        return safeAuctions(auctions).stream()
                .collect(Collectors.toMap(AuctionItem::getId, Function.identity(), (left, ignored) -> left));
    }

    private List<AuctionItem> safeAuctions(List<AuctionItem> auctions) {
        return auctions == null ? List.of() : auctions;
    }

    private List<BidTransaction> safeBids(List<BidTransaction> bids) {
        return bids == null ? List.of() : bids;
    }

    private String formatBidderName(User bidder) {
        if (bidder == null) {
            return UiText.text("Bidder") + ": -";
        }

        String fullName = safeText(bidder.getFullName(), bidder.getUsername());
        return UiText.text("Bidder") + ": " + fullName + " (#" + bidder.getId() + ")";
    }

    private TransactionRow toTransactionRow(BidTransaction bid, AuctionItem auction, int bidderId) {
        return new TransactionRow(
                formatTime(bid.getTimestamp()),
                auction == null ? UiText.text("Unknown auction") : auction.getName(),
                auction == null ? "-" : safeText(auction.getCategory(), "-"),
                AuctionViewFormatter.formatMoney(bid.getAmount()),
                UiText.text(bid.getStatus()),
                resolveBidResult(auction, bidderId)
        );
    }

    private WonProductRow toWonProductRow(AuctionItem auction) {
        return new WonProductRow(
                auction.getName(),
                safeText(auction.getCategory(), "-"),
                AuctionViewFormatter.formatMoney(auction.getCurrentHighestBid()),
                UiText.auctionStatus(auction.getStatus()),
                formatTime(auction.getEndTime())
        );
    }

    private boolean isWonAuction(AuctionItem auction) {
        return auction.getStatus() == AuctionStatus.FINISHED || auction.getStatus() == AuctionStatus.PAID;
    }

    private String resolveBidResult(AuctionItem auction, int bidderId) {
        if (auction == null) {
            return "-";
        }
        if (auction.getStatus() == AuctionStatus.CANCELED) {
            return UiText.text("Cancelled");
        }
        if (auction.getWinnerId() == bidderId && auction.getStatus() == AuctionStatus.RUNNING) {
            return UiText.text("Leading");
        }
        if (auction.getWinnerId() == bidderId && isWonAuction(auction)) {
            return UiText.text("Won");
        }
        if (auction.getStatus() == AuctionStatus.OPEN) {
            return UiText.text("Pending");
        }
        return UiText.text("Outbid");
    }

    private String formatTime(long timestamp) {
        if (timestamp <= 0) {
            return "-";
        }
        return BID_TIME.format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()));
    }

    private String safeText(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private record TransactionRow(
            String time,
            String product,
            String category,
            String amount,
            String status,
            String result
    ) {}

    private record WonProductRow(
            String product,
            String category,
            String price,
            String status,
            String endedAt
    ) {}
}
