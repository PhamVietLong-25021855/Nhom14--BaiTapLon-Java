package userauth.gui.fxml;

import javafx.animation.*;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import userauth.controller.AuctionController;
import userauth.controller.AutobidController;
import userauth.controller.WalletController;
import userauth.model.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BidderDashboardViewController {
    private static final String FILTER_ALL = "All";
    private static final String FILTER_RUNNING = "Running";
    private static final String FILTER_OPEN = "Opening Soon";
    private static final String FILTER_FINISHED = "Finished";
    private static final long ENDING_SOON_THRESHOLD_MS = 5 * 60 * 1000;
    private static final DateTimeFormatter LIVE_TIME =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final double MILLIS_PER_MINUTE = 60_000.0;



    @FXML
    private TableView<AuctionItem> tableAuctions;

    @FXML
    private TableColumn<AuctionItem, Integer> colId;

    @FXML
    private TableColumn<AuctionItem, String> colName;

    @FXML
    private TableColumn<AuctionItem, String> colCategory;

    @FXML
    private TableColumn<AuctionItem, String> colHighestBid;

    @FXML
    private TableColumn<AuctionItem, String> colStatus;

    @FXML
    private TableColumn<AuctionItem, String> colTimeLeft;

    @FXML
    private TableColumn<AutoBid, Integer> colIdAB;

    @FXML
    private TableColumn<AutoBid, Double> colIncrementAB;

    @FXML
    private TableColumn<AutoBid, Integer> colItemAB;

    @FXML
    private TableColumn<AutoBid, Double> colMaxPriceAB;


    @FXML
    private TextField txtSearch;

    @FXML
    private ComboBox<String> cbStatusFilter;

    @FXML
    private Label lblSidebarUser;

    @FXML
    private Label lblUserName;

    @FXML
    private Label lblRunningCount;

    @FXML
    private Label lblEndingSoonCount;

    @FXML
    private Label lblLeadingCount;

    @FXML
    private Label lblDetailName;

    @FXML
    private ImageView imgDetailAuction;

    @FXML
    private Label lblDetailImageInitial;

    @FXML
    private Label lblDetailDescription;

    @FXML
    private Label lblDetailCurrentBid;

    @FXML
    private Label lblDetailState;

    @FXML
    private Label lblDetailTimeLeft;

    @FXML
    private Label lblDetailStartPrice;

    @FXML
    private Label lblDetailLeader;

    @FXML
    private Label lblDetailSchedule;

    @FXML
    private Label lblDetailCategory;

    @FXML
    private Label lblBidStatus;

    @FXML
    private Label lblLiveBidCount;

    @FXML
    private TextField txtBidAmount;

    @FXML
    private VBox bidsLiveContainer;

    @FXML
    private LineChart<Number, Number> chartBidTrend;

    @FXML
    private NumberAxis xAxisBidTrend;

    @FXML
    private NumberAxis yAxisBidTrend;

    @FXML
    private TextField idAutobid;

    @FXML
    private TextField maxPrice;

    @FXML
    private TextField incrementAutobid;

    @FXML
    private TableView<AutoBid> tableAutoBid;

    @FXML
    private Label lblWalletBalance;

    private AuthFrame frame;
    private AuctionController auctionController;
    private AutobidController autobidController;
    private WalletController walletController;
    private User currentUser;
    private Timeline timeline;
    private final PauseTransition filterRefreshDebounce = new PauseTransition(Duration.millis(220));
    private Map<Integer, List<BidTransaction>> bidsByAuction = Map.of();
    private int lastSelectedAuctionId = -1;
    private int lastSelectedWinnerId = -1;
    private double lastSelectedHighestBid = -1;
    private long refreshTicket;
    private boolean bidActionInProgress;

    @FXML
    private void initialize() {
        colId.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        colName.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getName()));
        colCategory.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getCategory()));
        colHighestBid.setCellValueFactory(data -> new ReadOnlyStringWrapper(AuctionViewFormatter.formatMoney(data.getValue().getCurrentHighestBid())));
        colStatus.setCellValueFactory(data -> new ReadOnlyStringWrapper(UiText.auctionStatus(data.getValue().getStatus())));
        colTimeLeft.setCellValueFactory(data -> new ReadOnlyStringWrapper(AuctionViewFormatter.formatTimeLeft(data.getValue())));

        colIdAB.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        colItemAB.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getAuctionId()));
        colIncrementAB.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getIncrement()));
        colMaxPriceAB.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getMaxPrice()));

        AuctionImageUtil.installRoundedClip(imgDetailAuction, 32, 32);

        cbStatusFilter.setItems(FXCollections.observableArrayList(
                FILTER_ALL,
                FILTER_RUNNING,
                FILTER_OPEN,
                FILTER_FINISHED
        ));
        cbStatusFilter.setValue(FILTER_ALL);
        UiText.configureTranslatedComboBox(cbStatusFilter);
        filterRefreshDebounce.setOnFinished(event -> refreshData());
        cbStatusFilter.valueProperty().addListener((observable, oldValue, newValue) -> scheduleRefreshData());
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> scheduleRefreshData());

        tableAuctions.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
                renderSelectedAuction(newValue, false));
        tableAuctions.setRowFactory(this::createAuctionRow);
        tableAutoBid.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
                populateAutobidForm(newValue));

        chartBidTrend.setAnimated(false);
        xAxisBidTrend.setAutoRanging(true);
        yAxisBidTrend.setAutoRanging(true);

        timeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> refreshData()));
        timeline.setCycleCount(Animation.INDEFINITE);

        setBidStatus("Select an auction to view details.", false);
        showEmptySelectionState();
    }

    public void setFrame(AuthFrame frame) {
        this.frame = frame;
    }

    public void setAuctionController(AuctionController auctionController) {
        this.auctionController = auctionController;
    }
    public void setAutobidController(AutobidController autobidController) {
        this.autobidController = autobidController;
    }

    public void setWalletController(WalletController walletController) {
        this.walletController = walletController;
    }

    public void setUser(User user) {
        this.currentUser = user;
        String displayName = user == null ? UiText.text("Bidder") : abbreviate(resolveDisplayName(user), 26);
        String sidebarName = user == null
                ? "@" + UiText.text("Bidder")
                : "@" + abbreviate(safeText(user.getUsername(), UiText.text("Bidder")), 18);
        lblUserName.setText(displayName);
        lblSidebarUser.setText(sidebarName);
        updateWalletBalance();
        lastSelectedAuctionId = -1;
        lastSelectedWinnerId = -1;
        lastSelectedHighestBid = -1;
        txtBidAmount.clear();
    }

    public void activate() {
        refreshData();
        if (timeline != null && timeline.getStatus() != Animation.Status.RUNNING) {
            timeline.play();
        }
    }

    public void deactivate() {
        refreshTicket++;
        if (timeline != null) {
            timeline.stop();
        }
        filterRefreshDebounce.stop();
    }

    public void refreshData() {
        if (auctionController == null || autobidController == null || currentUser == null) {
            return;
        }

        long ticket = ++refreshTicket;
        int selectedId = selectedAuctionId();
        String keyword = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase(Locale.ROOT);
        String statusFilter = cbStatusFilter.getValue();

        UiAsync.run(
                () -> loadBidderSnapshot(keyword, statusFilter),
                snapshot -> {
                    if (ticket != refreshTicket) {
                        return;
                    }
                    applyBidderSnapshot(snapshot, selectedId);
                },
                error -> {
                }
        );
        UiAsync.run(
                () -> loadAutobidSnapshot(),
                snapshot -> {
                    applyAutobidSnapshot(snapshot);
                },
                error -> {
                }
        );
        updateWalletBalance();
    }

    @FXML
    private void handlePlaceBid() {
        if (auctionController == null || currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Bidding is not ready.");
            return;
        }

        AuctionItem selected = tableAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Please select an auction.");
            return;
        }


        String bidInput = txtBidAmount.getText() == null ? "" : txtBidAmount.getText().trim();
        if (bidInput.isBlank()){
            setBidStatus("Please enter a bid amount before placing a bid.", true);
            NotificationUtil.warning(ownerWindow(), "Notification", "Please enter a bid amount.");
            return;
        }

        try {
            double amount = Double.parseDouble(bidInput);
            int auctionId = selected.getId();
            int bidderId = currentUser.getId();
            setBid(amount,auctionId,bidderId);
        } catch (NumberFormatException ex) {
            setBidStatus("Invalid amount.", true);
            NotificationUtil.error(ownerWindow(), "Error", "Invalid amount.");
        }
    }

    @FXML
    private void handleAutobid(){
        if (autobidController == null || currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Auto-bid is not ready.");
            return;
        }
        AuctionItem selectedAuction = tableAuctions.getSelectionModel().getSelectedItem();
        if (selectedAuction == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Please select an auction.");
            return;
        }

        String max = maxPrice.getText() == null ? "" : maxPrice.getText().trim();
        String increment = incrementAutobid.getText() == null ? "" : incrementAutobid.getText().trim();
        String id = idAutobid.getText() == null ? "" : idAutobid.getText().trim();

        try {
            int bidderId = currentUser.getId();
            int auctionId = selectedAuction.getId();
            String result;

            if (id.isBlank()) {
                if (max.isBlank() || increment.isBlank()) {
                    NotificationUtil.warning(ownerWindow(), "Notification", "Enter max price and increment to create an auto-bid.");
                    return;
                }
                result = autobidController.createAutobid(
                        bidderId,
                        auctionId,
                        Double.parseDouble(max),
                        Double.parseDouble(increment)
                );
            } else if (max.isBlank() && increment.isBlank()) {
                result = autobidController.deleteAutoBid(bidderId, Integer.parseInt(id));
            } else {
                if (max.isBlank() || increment.isBlank()) {
                    NotificationUtil.warning(ownerWindow(), "Notification", "Provide both max price and increment when updating an auto-bid.");
                    return;
                }
                result = autobidController.updateAutobid(
                        bidderId,
                        Integer.parseInt(id),
                        Double.parseDouble(max),
                        Double.parseDouble(increment)
                );
            }

            if ("SUCCESS".equals(result)) {
                clearAutobidForm();
                tableAutoBid.getSelectionModel().clearSelection();
                NotificationUtil.success(ownerWindow(), "Notification", "Auto-bid updated successfully.");
                refreshData();
                return;
            }

            NotificationUtil.error(ownerWindow(), "Error", result);
        } catch (NumberFormatException ex) {
            NotificationUtil.error(ownerWindow(), "Error", "Invalid number.");
        }
    }

    @FXML
    private void handleShowHistory() {
        if (auctionController == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "AuctionController has not been assigned to the bidder screen.");
            return;
        }

        AuctionItem selected = tableAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Please select an auction.");
            return;
        }

        if (frame == null) {
            NotificationUtil.info(ownerWindow(), "Notification", "Connect this controller to AuthFrame to open bid history using FXML.");
            return;
        }

        List<BidTransaction> bids = bidsByAuction.getOrDefault(selected.getId(), List.of());
        frame.showBidHistoryDialog(selected, bids);
    }

    @FXML
    private void handleChangePassword() {
        if (currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Current user information is unavailable.");
            return;
        }
        if (frame == null) {
            NotificationUtil.info(ownerWindow(), "Notification", "The change-password action is prepared. Connect this controller to AuthFrame when integrating.");
            return;
        }

        frame.showChangePasswordDialog(currentUser);
    }

    @FXML
    private void handleSwitchToEnglish() {
        switchLanguage(AppLanguage.ENGLISH);
    }

    @FXML
    private void handleSwitchToVietnamese() {
        switchLanguage(AppLanguage.VIETNAMESE);
    }

    @FXML
    private void handleLogout() {
        currentUser = null;
        deactivate();
        if (frame != null) {
            frame.showLogin();
        } else {
            NotificationUtil.info(ownerWindow(), "Notification", "The logout action is prepared. Connect this controller to AuthFrame when integrating.");
        }
    }

    @FXML
    private void handleTopUp() {
        if (currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Current user information is unavailable.");
            return;
        }
        if (frame == null) {
            NotificationUtil.info(ownerWindow(), "Notification", "The top-up action is prepared. Connect this controller to AuthFrame when integrating.");
            return;
        }

        LoadedView<TopUpDialogController> view = FxmlRuntime.loadView(AuthFrame.class, "top-up-dialog.fxml", "dialog");
        Stage dialog = FxmlRuntime.createModalDialog(frame.getWindow(), "TOP UP WALLET", view.root(), 480, 380);
        view.controller().setDialogStage(dialog);
        view.controller().setWalletController(walletController);
        view.controller().setUser(currentUser);
        view.controller().setSuccessHandler(message -> {
            showFancySuccess(message);
            updateWalletBalance();
        });
        dialog.showAndWait();
    }

    private TableRow<AuctionItem> createAuctionRow(TableView<AuctionItem> ignored) {
        return new TableRow<>() {
            @Override
            protected void updateItem(AuctionItem item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("table-row-live", "table-row-ending", "table-row-closed");
                if (empty || item == null) {
                    return;
                }

                if (item.getStatus() == AuctionStatus.RUNNING) {
                    long remaining = item.getEndTime() - System.currentTimeMillis();
                    getStyleClass().add(remaining <= ENDING_SOON_THRESHOLD_MS ? "table-row-ending" : "table-row-live");
                    return;
                }

                if (item.getStatus() == AuctionStatus.FINISHED
                        || item.getStatus() == AuctionStatus.CANCELED
                        || item.getStatus() == AuctionStatus.PAID) {
                    getStyleClass().add("table-row-closed");
                }
            }
        };
    }

    private void updateMetrics(List<AuctionItem> allAuctions) {
        long running = allAuctions.stream()
                .filter(item -> item.getStatus() == AuctionStatus.RUNNING)
                .count();
        long endingSoon = allAuctions.stream()
                .filter(item -> item.getStatus() == AuctionStatus.RUNNING)
                .filter(item -> (item.getEndTime() - System.currentTimeMillis()) <= ENDING_SOON_THRESHOLD_MS)
                .count();
        long leading = allAuctions.stream()
                .filter(item -> item.getStatus() == AuctionStatus.RUNNING)
                .filter(item -> item.getWinnerId() == currentUser.getId())
                .count();

        lblRunningCount.setText(String.valueOf(running));
        lblEndingSoonCount.setText(String.valueOf(Math.max(endingSoon, 0)));
        lblLeadingCount.setText(String.valueOf(leading));
    }

    private boolean matchesSearch(AuctionItem item, String keyword) {
        if (keyword.isBlank()) {
            return true;
        }

        return item.getName().toLowerCase(Locale.ROOT).contains(keyword)
                || item.getCategory().toLowerCase(Locale.ROOT).contains(keyword);
    }

    private boolean matchesStatusFilter(AuctionItem item, String filter) {
        if (filter == null || FILTER_ALL.equals(filter)) {
            return true;
        }

        return switch (filter) {
            case FILTER_RUNNING -> item.getStatus() == AuctionStatus.RUNNING;
            case FILTER_OPEN -> item.getStatus() == AuctionStatus.OPEN;
            case FILTER_FINISHED -> item.getStatus() == AuctionStatus.FINISHED
                    || item.getStatus() == AuctionStatus.CANCELED
                    || item.getStatus() == AuctionStatus.PAID;
            default -> true;
        };
    }

    private int selectedAuctionId() {
        AuctionItem selected = tableAuctions.getSelectionModel().getSelectedItem();
        return selected == null ? -1 : selected.getId();
    }

    private void reselectAuction(int selectedId) {
        if (selectedId < 0) {
            return;
        }

        tableAuctions.getItems().stream()
                .filter(item -> item.getId() == selectedId)
                .findFirst()
                .ifPresent(item -> tableAuctions.getSelectionModel().select(item));
    }

    private void renderSelectedAuction(AuctionItem auction, boolean allowNotifications) {
        if (auction == null) {
            showEmptySelectionState();
            return;
        }

        List<BidTransaction> bids = bidsByAuction.getOrDefault(auction.getId(), List.of());
        if (allowNotifications && auction.getId() == lastSelectedAuctionId) {
            notifySelectedAuctionStateChanges(auction);
        }

        lblDetailName.setText(auction.getName());
        AuctionImageUtil.applyAuctionImage(imgDetailAuction, lblDetailImageInitial, auction.getImageSource(), auction.getName());
        lblDetailDescription.setText(auction.getDescription() == null || auction.getDescription().isBlank()
                ? UiText.text("This product does not have a detailed description yet.")
                : auction.getDescription());
        lblDetailCurrentBid.setText(AuctionViewFormatter.formatMoney(auction.getCurrentHighestBid()));
        lblDetailStartPrice.setText(AuctionViewFormatter.formatMoney(auction.getStartPrice()));
        lblDetailLeader.setText(formatLeader(auction));
        lblDetailSchedule.setText(AuctionViewFormatter.formatScheduleRange(auction));
        lblDetailCategory.setText(auction.getCategory());
        lblLiveBidCount.setText(formatTransactionCount(bids.size()));

        applyStatusChip(auction);
        applyTimeChip(auction);
        populateBidFeed(bids);
        updateBidTrend(bids);

        lastSelectedAuctionId = auction.getId();
        lastSelectedWinnerId = auction.getWinnerId();
        lastSelectedHighestBid = auction.getCurrentHighestBid();
    }

    private void notifySelectedAuctionStateChanges(AuctionItem auction) {
        if (auction.getCurrentHighestBid() != lastSelectedHighestBid) {
            UiEffects.pulse(lblDetailCurrentBid);
            lblDetailCurrentBid.getStyleClass().remove("live-glow");
            lblDetailCurrentBid.getStyleClass().add("live-glow");
            PauseTransition glowReset = new PauseTransition(Duration.millis(720));
            glowReset.setOnFinished(event -> lblDetailCurrentBid.getStyleClass().remove("live-glow"));
            glowReset.play();
        }

        if (lastSelectedWinnerId == currentUser.getId() && auction.getWinnerId() != currentUser.getId()) {
            setBidStatus("You have just been outbid in this auction.", true);
            NotificationUtil.error(ownerWindow(), "Outbid", "You have just been outbid in the auction you are watching.");
        } else if (lastSelectedWinnerId != currentUser.getId()
                && auction.getWinnerId() == currentUser.getId()
                && auction.getStatus() == AuctionStatus.RUNNING) {
            setBidStatus("You are currently leading.", false);
            NotificationUtil.success(ownerWindow(), "Leading", "You currently have the highest bid in this auction.");
        }
    }

    private void applyStatusChip(AuctionItem auction) {
        lblDetailState.getStyleClass().setAll("status-chip");
        switch (auction.getStatus()) {
            case RUNNING -> {
                lblDetailState.setText(UiText.text("LIVE"));
                lblDetailState.getStyleClass().add("status-chip-live");
            }
            case OPEN -> {
                lblDetailState.setText(UiText.text("OPENING SOON"));
                lblDetailState.getStyleClass().add("status-chip-upcoming");
            }
            case FINISHED, PAID -> {
                lblDetailState.setText(UiText.text("FINISHED"));
                lblDetailState.getStyleClass().add("status-chip-neutral");
            }
            case CANCELED -> {
                lblDetailState.setText(UiText.text("CANCELLED"));
                lblDetailState.getStyleClass().add("status-chip-danger");
            }
        }
    }

    private void applyTimeChip(AuctionItem auction) {
        lblDetailTimeLeft.getStyleClass().setAll("status-chip");
        lblDetailTimeLeft.setText(AuctionViewFormatter.formatTimeLeft(auction));

        if (auction.getStatus() != AuctionStatus.RUNNING) {
            lblDetailTimeLeft.getStyleClass().add("status-chip-neutral");
            return;
        }

        long remaining = auction.getEndTime() - System.currentTimeMillis();
        if (remaining <= 60_000) {
            lblDetailTimeLeft.getStyleClass().add("status-chip-danger");
        } else if (remaining <= ENDING_SOON_THRESHOLD_MS) {
            lblDetailTimeLeft.getStyleClass().add("status-chip-upcoming");
        } else {
            lblDetailTimeLeft.getStyleClass().add("status-chip-live");
        }
    }

    private void populateBidFeed(List<BidTransaction> bids) {
        bidsLiveContainer.getChildren().clear();
        if (bids == null || bids.isEmpty()) {
            VBox placeholder = new VBox(4);
            placeholder.getStyleClass().add("activity-card");
            placeholder.getChildren().addAll(
                    createLabel(UiText.text("No bid transactions yet."), "activity-title"),
                    createLabel(UiText.text("New bid activity will appear here immediately."), "activity-meta")
            );
            bidsLiveContainer.getChildren().add(placeholder);
            return;
        }

        List<BidTransaction> latestBids = bids.stream()
                .sorted(Comparator.comparingLong(BidTransaction::getTimestamp).reversed())
                .limit(5)
                .toList();

        for (BidTransaction bid : latestBids) {
            VBox card = new VBox(4);
            card.getStyleClass().add("activity-card");

            HBox row = new HBox(8);
            Label bidder = createLabel(UiText.text("Bidder #") + bid.getBidderId(), "activity-title");
            Label time = createLabel(LIVE_TIME.format(Instant.ofEpochMilli(bid.getTimestamp())), "activity-meta");
            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            row.getChildren().addAll(bidder, spacer, time);

            card.getChildren().addAll(
                    row,
                    createLabel(AuctionViewFormatter.formatMoney(bid.getAmount()), "activity-price"),
                    createLabel(UiText.text("Status") + ": " + UiText.text(bid.getStatus()), "activity-meta")
            );
            bidsLiveContainer.getChildren().add(card);
        }
    }

    private void updateBidTrend(List<BidTransaction> bids) {
        chartBidTrend.getData().clear();
        if (bids == null || bids.isEmpty()) {
            xAxisBidTrend.setLabel("Time since first bid (minutes)");
            return;
        }

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        List<BidTransaction> orderedBids = bids.stream()
                .sorted(Comparator.comparingLong(BidTransaction::getTimestamp))
                .toList();

        long firstTimestamp = orderedBids.get(0).getTimestamp();
        xAxisBidTrend.setLabel("Time since first bid (minutes)");
        for (BidTransaction bid : orderedBids) {
            double elapsedMinutes = Math.max(0, (bid.getTimestamp() - firstTimestamp) / MILLIS_PER_MINUTE);
            series.getData().add(new XYChart.Data<>(elapsedMinutes, bid.getAmount()));
        }
        chartBidTrend.getData().add(series);
    }

    private void showEmptySelectionState() {
        lblDetailName.setText(UiText.text("Select an auction to view details"));
        AuctionImageUtil.applyAuctionImage(imgDetailAuction, lblDetailImageInitial, null, "A");
        lblDetailDescription.setText(UiText.text("The product description will appear here."));
        lblDetailCurrentBid.setText("0");
        lblDetailStartPrice.setText("-");
        lblDetailLeader.setText("-");
        lblDetailSchedule.setText("-");
        lblDetailCategory.setText("-");
        lblDetailState.getStyleClass().setAll("status-chip", "status-chip-neutral");
        lblDetailState.setText(UiText.text("NO AUCTION SELECTED"));
        lblDetailTimeLeft.getStyleClass().setAll("status-chip", "status-chip-neutral");
        lblDetailTimeLeft.setText(UiText.text("Remaining") + ": -");
        lblLiveBidCount.setText(formatTransactionCount(0));
        bidsLiveContainer.getChildren().clear();
        chartBidTrend.getData().clear();
    }

    private void setBidStatus(String message, boolean error) {
        lblBidStatus.setText(UiText.text(message));
        lblBidStatus.getStyleClass().removeAll("error-text", "success-text");
        if (error) {
            if (!lblBidStatus.getStyleClass().contains("error-text")) {
                lblBidStatus.getStyleClass().add("error-text");
            }
            return;
        }

        if (!lblBidStatus.getStyleClass().contains("success-text")) {
            lblBidStatus.getStyleClass().add("success-text");
        }
    }

    private Label createLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add(styleClass);
        return label;
    }

    private String formatLeader(AuctionItem auction) {
        if (auction.getWinnerId() <= 0) {
            return UiText.text("No leading bidder yet");
        }
        if (currentUser != null && auction.getWinnerId() == currentUser.getId()) {
            return UiText.text("You are leading");
        }
        return UiText.text("Bidder #") + auction.getWinnerId();
    }

    private javafx.stage.Window ownerWindow() {
        return frame == null ? null : frame.getWindow();
    }

    private String formatTransactionCount(int count) {
        return count + " " + UiText.text("transactions");
    }

    private void switchLanguage(AppLanguage language) {
        if (frame == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Language settings are unavailable.");
            return;
        }
        frame.setLanguage(language);
        UiText.refreshTranslatedComboBox(cbStatusFilter);
        tableAuctions.refresh();
        renderSelectedAuction(tableAuctions.getSelectionModel().getSelectedItem(), false);
        NotificationUtil.success(ownerWindow(), "Notification", "Language updated.");
    }

    private void scheduleRefreshData() {
        filterRefreshDebounce.playFromStart();
    }

    private void setBidControlsBusy(boolean busy) {
        if (txtBidAmount != null) {
            txtBidAmount.setDisable(busy);
        }
        if (tableAuctions != null) {
            tableAuctions.setDisable(busy);
        }
    }

    private void populateAutobidForm(AutoBid autoBid) {
        if (autoBid == null) {
            return;
        }
        idAutobid.setText(String.valueOf(autoBid.getId()));
        maxPrice.setText(String.valueOf(autoBid.getMaxPrice()));
        incrementAutobid.setText(String.valueOf(autoBid.getIncrement()));
    }

    private void clearAutobidForm() {
        idAutobid.clear();
        maxPrice.clear();
        incrementAutobid.clear();
    }

    private BidderSnapshot loadBidderSnapshot(String keyword, String statusFilter) {
        List<AuctionItem> allAuctions = auctionController.getAllAuctions();
        Map<Integer, List<BidTransaction>> groupedBids = new HashMap<>();
        for (BidTransaction bid : auctionController.getAllBids()) {
            groupedBids.computeIfAbsent(bid.getAuctionId(), ignored -> new java.util.ArrayList<>()).add(bid);
        }

        List<AuctionItem> filteredAuctions = allAuctions.stream()
                .filter(item -> matchesSearch(item, keyword))
                .filter(item -> matchesStatusFilter(item, statusFilter))
                .sorted(Comparator
                        .comparingInt((AuctionItem item) -> item.getStatus() == AuctionStatus.RUNNING ? 0 : 1)
                        .thenComparingLong(AuctionItem::getEndTime))
                .toList();

        return new BidderSnapshot(allAuctions, filteredAuctions, groupedBids);
    }

    private void applyBidderSnapshot(BidderSnapshot snapshot, int selectedId) {
        bidsByAuction = snapshot.groupedBids();
        updateMetrics(snapshot.allAuctions());

        tableAuctions.setItems(FXCollections.observableArrayList(snapshot.filteredAuctions()));
        reselectAuction(selectedId);
        if (tableAuctions.getSelectionModel().getSelectedItem() == null && !tableAuctions.getItems().isEmpty()) {
            tableAuctions.getSelectionModel().selectFirst();
        }

        AuctionItem selectedAuction = tableAuctions.getSelectionModel().getSelectedItem();
        renderSelectedAuction(selectedAuction, true);
        tableAuctions.refresh();
    }

    //tableAuctions
    //tableAutoBid
    private AutobidSnapshot loadAutobidSnapshot() {
        int bidderId = currentUser.getId();
        List<AutoBid> allAutobid = autobidController.getAutobidByBidder(bidderId);
        return new AutobidSnapshot(allAutobid);
    }

    private void applyAutobidSnapshot(AutobidSnapshot snapshot) {
        tableAutoBid.setItems(FXCollections.observableArrayList(snapshot.allAutobids()));
        tableAutoBid.refresh();
    }

    private void updateWalletBalance() {
        if (walletController == null || currentUser == null) {
            lblWalletBalance.setText(UiText.text("Balance: 0 VNĐ"));
            return;
        }

        double balance = walletController.getWalletBalance(currentUser.getId());
        lblWalletBalance.setText(UiText.text("Balance: ") + AuctionViewFormatter.formatMoney(balance));
    }

    private String resolveDisplayName(User user) {
        String fullName = safeText(user.getFullName(), "");
        if (!fullName.isBlank()) {
            return fullName;
        }
        return safeText(user.getUsername(), UiText.text("Bidder"));
    }

    private String safeText(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private record BidderSnapshot(
            List<AuctionItem> allAuctions,
            List<AuctionItem> filteredAuctions,
            Map<Integer, List<BidTransaction>> groupedBids
    ) {}

    private record AutobidSnapshot(
            List<AutoBid> allAutobids
    ) {}

    private void setBid (double amount, int auctionId, int bidderId){
        bidActionInProgress = true;
        setBidControlsBusy(true);
        setBidStatus("Submitting your bid...", false);
        UiAsync.run(
                () -> auctionController.placeBid(auctionId, bidderId, amount),
                result -> {
                    bidActionInProgress = false;
                    setBidControlsBusy(false);
                    if ("SUCCESS".equals(result)) {
                        txtBidAmount.clear();
                        setBidStatus("Bid placed successfully. Refreshing the selected auction.", false);
                        NotificationUtil.success(ownerWindow(), "Notification", "Bid placed successfully.");
                        refreshData();
                        return;
                    }

                    setBidStatus(result, true);
                    NotificationUtil.error(ownerWindow(), "Error", result);
                },
                error -> {
                    bidActionInProgress = false;
                    setBidControlsBusy(false);
                    setBidStatus("Unable to place a bid right now.", true);
                    NotificationUtil.error(ownerWindow(), "Error", "Unable to place a bid right now.");
                }
        );
    }
    // hiển thị hộp thoại thông báo nạp tiền (Top Up) successfully ;
    public void showFancySuccess(String message) {
        // 1. Tạo giao diện thông báo
        VBox toast = new VBox(5);

        toast.setStyle("-fx-background-color: linear-gradient(to right, #2ecc71, #27ae60);" +
                "-fx-background-radius: 12;" +
                "-fx-padding: 15 25;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.3), 10, 0, 0, 5);");
        toast.setMaxWidth(350);

        Label title = new Label("✨ NẠP TIỀN THÀNH CÔNG!");
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");

        Label msg = new Label(message);
        msg.setStyle("-fx-text-fill: #e8f8f5; -fx-font-size: 13px;");
        msg.setWrapText(true);

        toast.getChildren().addAll(title, msg);

        // 2. Đưa vào một Popup của JavaFX
        javafx.stage.Popup popup = new javafx.stage.Popup();
        popup.getContent().add(toast);
        popup.setAutoHide(true);

        // 3. Hiệu ứng xuất hiện (Fade & Slide)
        toast.setOpacity(0);
        toast.setTranslateY(-20);

        popup.show(frame.getWindow());

        // Animation cho mượt
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toast);
        fadeIn.setToValue(1);
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), toast);
        slideIn.setToY(0);

        fadeIn.play();
        slideIn.play();

        // Tự động đóng sau 3 giây
        PauseTransition delay = new PauseTransition(Duration.seconds(3));
        delay.setOnFinished(e -> popup.hide());
        delay.play();
    }
}
