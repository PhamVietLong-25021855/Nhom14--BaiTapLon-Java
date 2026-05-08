package userauth.gui.fxml.bidder;

import userauth.gui.fxml.shared.*;
import userauth.gui.fxml.shell.AuthFrame;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
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
import javafx.util.Duration;
import javafx.util.StringConverter;
import userauth.controller.AuctionController;
import userauth.controller.AutobidController;
import userauth.event.AuctionEvent;
import userauth.event.AuctionEventBus;
import userauth.event.AuctionEventListener;
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
    private static final double LIVE_REFRESH_INTERVAL_SECONDS = 1.0;
    private static final DateTimeFormatter LIVE_TIME =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());



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
    private Label lblDetailAntiSniping;

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

    private AuthFrame frame;
    private AuctionController auctionController;
    private AutobidController autobidController;
    private User currentUser;
    private Timeline timeline;
    private final PauseTransition filterRefreshDebounce = new PauseTransition(Duration.millis(220));
    private Map<Integer, List<BidTransaction>> bidsByAuction = Map.of();
    private int lastSelectedAuctionId = -1;
    private int lastSelectedWinnerId = -1;
    private double lastSelectedHighestBid = -1;
    private long refreshTicket;
    private boolean bidActionInProgress;
    private final AuctionEventListener auctionEventListener = event -> Platform.runLater(() -> handleAuctionEvent(event));
    private boolean observerRegistered;

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

        chartBidTrend.setAnimated(false);
        xAxisBidTrend.setAutoRanging(true);
        xAxisBidTrend.setTickLabelRotation(45);
        xAxisBidTrend.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number value) {
                return value == null ? "" : AuctionViewFormatter.formatTimeWithSeconds(value.longValue());
            }

            @Override
            public Number fromString(String string) {
                return 0L;
            }
        });
        yAxisBidTrend.setAutoRanging(true);
        idAutobid.setEditable(false);
        tableAutoBid.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
                populateAutobidForm(newValue));

        timeline = new Timeline(new KeyFrame(Duration.seconds(LIVE_REFRESH_INTERVAL_SECONDS), event -> refreshData()));
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

    public void setUser(User user) {
        this.currentUser = user;
        String displayName = user == null ? UiText.text("Bidder") : abbreviate(resolveDisplayName(user), 26);
        String sidebarName = user == null
                ? "@" + UiText.text("Bidder")
                : "@" + abbreviate(safeText(user.getUsername(), UiText.text("Bidder")), 18);
        lblUserName.setText(displayName);
        lblSidebarUser.setText(sidebarName);
        lastSelectedAuctionId = -1;
        lastSelectedWinnerId = -1;
        lastSelectedHighestBid = -1;
        txtBidAmount.clear();
    }

    public void activate() {
        registerAuctionObserver();
        refreshData();
        if (timeline != null && timeline.getStatus() != Animation.Status.RUNNING) {
            timeline.play();
        }
    }

    public void deactivate() {
        refreshTicket++;
        unregisterAuctionObserver();
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
    }

    @FXML
    private void handlePlaceBid() {
        if (auctionController == null || currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "AutoBid is not ready.");
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
        if (tableAuctions.getSelectionModel().getSelectedItem() == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Please select an auction.");
            return;
        }

        String max = maxPrice.getText() == null ? "" : maxPrice.getText().trim();
        String increment = incrementAutobid.getText() == null ? "" : incrementAutobid.getText().trim();
        String id = idAutobid.getText() == null ? "" : idAutobid.getText().trim();
        if (max.isBlank() || increment.isBlank()) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Please enter a bid amount and increment.");
            return;
        }

        try {
            double maxAmount = Double.parseDouble(max);
            int auctionId = tableAuctions.getSelectionModel().getSelectedItem().getId();
            int bidderId = currentUser.getId();
            String result;
            if (id.isBlank()){
                result = autobidController.createAutobid(bidderId, auctionId, maxAmount, Double.parseDouble(increment));
            }else {
                result = autobidController.updateAutobid(bidderId, Integer.parseInt(id), maxAmount, Double.parseDouble(increment));
            }
            if (!"SUCCESS".equals(result)) {
                NotificationUtil.error(ownerWindow(), "Error", result);
                return;
            }
            clearAutobidForm();
            refreshData();
        } catch (NumberFormatException ex) {
            NotificationUtil.error(ownerWindow(), "Error", "Invalid number.");
        }
    }

    @FXML
    private void handleDeleteAutobid() {
        String id = idAutobid.getText() == null ? "" : idAutobid.getText().trim();
        if (id.isBlank()) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Please select an auto-bid rule.");
            return;
        }

        try {
            String result = autobidController.deleteAutoBid(currentUser.getId(), Integer.parseInt(id));
            if (!"SUCCESS".equals(result)) {
                NotificationUtil.error(ownerWindow(), "Error", result);
                return;
            }
            clearAutobidForm();
            refreshData();
        } catch (NumberFormatException ex) {
            NotificationUtil.error(ownerWindow(), "Error", "Invalid auto-bid id.");
        }
    }

    @FXML
    private void handleClearAutobidForm() {
        clearAutobidForm();
        tableAutoBid.getSelectionModel().clearSelection();
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
    private void handleEditProfile() {
        if (currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Current user information is unavailable.");
            return;
        }
        if (frame == null) {
            NotificationUtil.info(ownerWindow(), "Notification", "Profile settings are unavailable.");
            return;
        }

        frame.showProfileDialog(currentUser, this::setUser);
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
        AuctionImageUtil.applyAuctionImage(imgDetailAuction, lblDetailImageInitial, auction.getImageData(), auction.getImageSource(), auction.getName());
        lblDetailDescription.setText(auction.getDescription() == null || auction.getDescription().isBlank()
                ? UiText.text("This product does not have a detailed description yet.")
                : auction.getDescription());
        lblDetailCurrentBid.setText(AuctionViewFormatter.formatMoney(auction.getCurrentHighestBid()));
        lblDetailStartPrice.setText(AuctionViewFormatter.formatMoney(auction.getStartPrice()));
        lblDetailLeader.setText(formatLeader(auction));
        lblDetailSchedule.setText(AuctionViewFormatter.formatScheduleRange(auction));
        lblDetailCategory.setText(auction.getCategory());
        lblDetailAntiSniping.setText(AuctionViewFormatter.formatAntiSnipingSummary(auction));
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
                lblDetailState.setText(auction.getStatus() == AuctionStatus.PAID
                        ? UiText.text("PAID")
                        : UiText.text("FINISHED"));
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
            return;
        }

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        List<BidTransaction> orderedBids = bids.stream()
                .sorted(Comparator.comparingLong(BidTransaction::getTimestamp))
                .toList();

        for (BidTransaction bid : orderedBids) {
            series.getData().add(new XYChart.Data<>(bid.getTimestamp(), bid.getAmount()));
        }
        chartBidTrend.getData().add(series);
    }

    private void showEmptySelectionState() {
        lblDetailName.setText(UiText.text("Select an auction to view details"));
        AuctionImageUtil.applyAuctionImage(imgDetailAuction, lblDetailImageInitial, null, null, "A");
        lblDetailDescription.setText(UiText.text("The product description will appear here."));
        lblDetailCurrentBid.setText("0");
        lblDetailStartPrice.setText("-");
        lblDetailLeader.setText("-");
        lblDetailSchedule.setText("-");
        lblDetailCategory.setText("-");
        lblDetailAntiSniping.setText("0/3 | closes at -");
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
        int selectedId = selectedAutobidId();
        tableAutoBid.setItems(FXCollections.observableArrayList(snapshot.allAutobids()));
        reselectAutobid(selectedId);
        if (tableAutoBid.getSelectionModel().getSelectedItem() == null) {
            clearAutobidForm();
        }
        tableAutoBid.refresh();
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

    private  void setBid (double amount, int auctionId, int bidderId){
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

    private void populateAutobidForm(AutoBid autoBid) {
        if (autoBid == null) {
            clearAutobidForm();
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

    private int selectedAutobidId() {
        AutoBid selected = tableAutoBid.getSelectionModel().getSelectedItem();
        return selected == null ? -1 : selected.getId();
    }

    private void reselectAutobid(int selectedId) {
        if (selectedId < 0) {
            return;
        }

        tableAutoBid.getItems().stream()
                .filter(item -> item.getId() == selectedId)
                .findFirst()
                .ifPresent(item -> tableAutoBid.getSelectionModel().select(item));
    }

    private void registerAuctionObserver() {
        if (observerRegistered) {
            return;
        }
        AuctionEventBus.getInstance().subscribe(auctionEventListener);
        observerRegistered = true;
    }

    private void unregisterAuctionObserver() {
        if (!observerRegistered) {
            return;
        }
        AuctionEventBus.getInstance().unsubscribe(auctionEventListener);
        observerRegistered = false;
    }

    private void handleAuctionEvent(AuctionEvent event) {
        if (event == null || currentUser == null) {
            return;
        }

        if (event.auctionId() == lastSelectedAuctionId) {
            switch (event.type()) {
                case ANTI_SNIPING_EXTENDED ->
                        setBidStatus("Anti-sniping extended the auction until " + AuctionViewFormatter.formatDateTimeWithSeconds(event.endTime()) + ".", false);
                case SETTLED -> setBidStatus(event.summary(), event.status() == AuctionStatus.CANCELED);
                case STATUS_CHANGED -> {
                    if (event.status() == AuctionStatus.FINISHED) {
                        setBidStatus("Auction finished. Waiting for seller settlement.", false);
                    }
                }
                case BID_ACTIVITY -> {
                }
            }
        }

        refreshData();
    }
}
