package userauth.gui.fxml.bidder;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.util.StringConverter;
import userauth.controller.AuctionController;
import userauth.controller.AuthController;
import userauth.controller.AutobidController;
import userauth.controller.NotificationController;
import userauth.controller.WalletController;
import userauth.event.AuctionEvent;
import userauth.event.AuctionEventBus;
import userauth.event.AuctionEventListener;
import userauth.exception.ItemNotFoundException;
import userauth.gui.fxml.shared.*;
import userauth.gui.fxml.shell.AuthFrame;
import userauth.model.*;

import java.util.*;

public class BidderDashboardViewController {
    private static final String FILTER_ALL = "All";
    private static final String FILTER_RUNNING = "Running";
    private static final String FILTER_OPEN = "Opening Soon";
    private static final String FILTER_FINISHED = "Finished";
    private static final long ENDING_SOON_THRESHOLD_MS = 5 * 60 * 1000;
    private static final double LIVE_REFRESH_INTERVAL_SECONDS = 2.0;

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
    private Label lblWalletBalance;

    @FXML
    private Label lblWalletAvailable;

    @FXML
    private Label lblWalletReserved;

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
    private TextField txtBidAmount;

    @FXML
    private Button btnPlaceBid;

    @FXML
    private LineChart<Number, Number> chartBidTrend;

    @FXML
    private NumberAxis xAxisBidTrend;

    @FXML
    private NumberAxis yAxisBidTrend;

    @FXML
    private Label lblAutobidMode;

    @FXML
    private TextField maxPrice;

    @FXML
    private TextField incrementAutobid;

    @FXML
    private TableView<AutoBid> tableAutoBid;

    @FXML
    private StackPane winnerOverlay;

    @FXML
    private Label lblWinnerTitle;

    @FXML
    private Label lblWinnerMessage;

    private AuthFrame frame;
    private AuthController authController;
    private AuctionController auctionController;
    private AutobidController autobidController;
    private WalletController walletController;
    private NotificationController notificationController;
    private User currentUser;
    private Timeline timeline;
    private final PauseTransition filterRefreshDebounce = new PauseTransition(Duration.millis(220));
    private List<AuctionItem> allAuctionsSnapshot = List.of();
    private Map<Integer, List<BidTransaction>> bidsByAuction = Map.of();
    private int lastSelectedAuctionId = -1;
    private int lastSelectedWinnerId = -1;
    private double lastSelectedHighestBid = -1;
    private AuctionStatus lastSelectedStatus;
    private long refreshTicket;
    private boolean auctionRefreshInProgress;
    private boolean bidsRefreshInProgress;
    private boolean autobidRefreshInProgress;
    private boolean walletRefreshInProgress;
    private boolean detailImageRefreshInProgress;
    private final Map<Integer, byte[]> detailImageDataCache = new HashMap<>();
    private final Map<Integer, String> detailImageSourceCache = new HashMap<>();
    private final Set<Integer> detailImageLoadAttempts = new HashSet<>();
    private boolean active;
    private int editingAutobidId = -1;
    private boolean bidActionInProgress;
    private boolean suppressAuctionSelectionSync;
    private boolean autobidFormDirty;
    private boolean autobidFormProgrammaticUpdate;
    private boolean suppressAutobidSelectionSync;
    private final Set<Integer> winnerAnnouncementsShown = new HashSet<>();
    private final Map<Integer, AuctionStatus> knownAuctionStatuses = new HashMap<>();
    private PauseTransition winnerOverlayAutoHide;
    private boolean auctionStatusSnapshotInitialized;
    private final AuctionEventListener auctionEventListener = event -> Platform.runLater(() -> handleAuctionEvent(event));
    private boolean observerRegistered;

    @FXML
    private void initialize() {
        colId.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        colName.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getName()));
        colCategory.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getCategory()));
        colHighestBid.setCellValueFactory(data -> new ReadOnlyStringWrapper(AuctionViewFormatter.formatMoney(data.getValue().getCurrentHighestBid())));
        colHighestBid.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    AuctionItem auction = getTableView().getItems().get(getIndex());
                    if (auction != null) {
                        setText(AuctionViewFormatter.formatMoney(auction.getCurrentHighestBid()));
                    } else {
                        setText(item);
                    }
                }
            }
        });
        colStatus.setCellValueFactory(data -> new ReadOnlyStringWrapper(UiText.auctionStatus(data.getValue().getStatus())));
        colTimeLeft.setCellValueFactory(data -> new ReadOnlyStringWrapper(AuctionViewFormatter.formatTimeLeft(data.getValue())));

        colIdAB.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        colItemAB.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getAuctionId()));
        colIncrementAB.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getIncrement()));
        colMaxPriceAB.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getMaxPrice()));

        AuctionImageUtil.installRoundedClip(imgDetailAuction, 14, 14);

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
                {
                    if (!suppressAuctionSelectionSync) {
                        renderSelectedAuction(newValue, false);
                        if (newValue != null) {
                            refreshSelectedBidsSnapshot(newValue.getId(), refreshTicket);
                        }
                    }
                });
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
        updateAutobidModeLabel();
        UiInput.installMoneyInput(txtBidAmount);
        UiInput.installMoneyInput(maxPrice);
        UiInput.installMoneyInput(incrementAutobid);
        registerAutobidFormListeners();
        tableAutoBid.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
                {
                    if (!suppressAutobidSelectionSync) {
                        populateAutobidForm(newValue);
                    }
                });

        timeline = new Timeline(new KeyFrame(Duration.seconds(LIVE_REFRESH_INTERVAL_SECONDS), event -> refreshDataFromTimer()));
        timeline.setCycleCount(Animation.INDEFINITE);

        setBidStatus("Select an auction to view details.", false);
        updateWalletSummary(null);
        showEmptySelectionState();
    }

    public void setFrame(AuthFrame frame) {
        this.frame = frame;
    }

    public void setAuthController(AuthController authController) {
        this.authController = authController;
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

    public void setNotificationController(NotificationController notificationController) {
        this.notificationController = notificationController;
    }

    public void setUser(User user) {
        this.currentUser = user;
        String displayName = user == null ? UiText.text("Bidder") : abbreviate(resolveDisplayName(user), 26);
        String sidebarName = user == null
                ? "@" + UiText.text("Bidder")
                : "@" + abbreviate(safeText(user.getUsername(), UiText.text("Bidder")), 18);
        lblUserName.setText(displayName);
        lblSidebarUser.setText(sidebarName);
        refreshTicket++;
        auctionRefreshInProgress = false;
        bidsRefreshInProgress = false;
        autobidRefreshInProgress = false;
        walletRefreshInProgress = false;
        detailImageRefreshInProgress = false;
        detailImageDataCache.clear();
        detailImageSourceCache.clear();
        detailImageLoadAttempts.clear();
        lastSelectedAuctionId = -1;
        lastSelectedWinnerId = -1;
        lastSelectedHighestBid = -1;
        lastSelectedStatus = null;
        winnerAnnouncementsShown.clear();
        knownAuctionStatuses.clear();
        auctionStatusSnapshotInitialized = false;
        txtBidAmount.clear();
        updateWalletSummary(null);
    }

    public void activate() {
        active = true;
        registerAuctionObserver();
        refreshData();
        if (timeline != null && timeline.getStatus() != Animation.Status.RUNNING) {
            timeline.play();
        }
    }

    public void deactivate() {
        active = false;
        refreshTicket++;
        auctionRefreshInProgress = false;
        bidsRefreshInProgress = false;
        autobidRefreshInProgress = false;
        walletRefreshInProgress = false;
        detailImageRefreshInProgress = false;
        detailImageDataCache.clear();
        detailImageSourceCache.clear();
        detailImageLoadAttempts.clear();
        unregisterAuctionObserver();
        if (timeline != null) {
            timeline.stop();
        }
        filterRefreshDebounce.stop();
    }

    public void refreshData() {
        if (!active || auctionController == null || autobidController == null || currentUser == null) {
            return;
        }

        long ticket = refreshTicket;
        int requestedSelectedId = selectedAuctionId();
        String keyword = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase(Locale.ROOT);
        String statusFilter = cbStatusFilter.getValue();

        refreshAuctionSnapshot(keyword, statusFilter, requestedSelectedId, ticket);
        refreshSelectedBidsSnapshot(requestedSelectedId, ticket);
        refreshAutobidSnapshot(ticket);
        refreshWalletSnapshot(ticket);
    }

    private void refreshAuctionSnapshot(String keyword, String statusFilter, int requestedSelectedId, long ticket) {
        if (auctionRefreshInProgress) {
            return;
        }
        auctionRefreshInProgress = true;
        UiAsync.run(
                () -> loadBidderSnapshot(keyword, statusFilter),
                snapshot -> {
                    auctionRefreshInProgress = false;
                    if (ticket != refreshTicket) {
                        refreshData();
                        return;
                    }
                    applyBidderSnapshot(snapshot, requestedSelectedId);
                },
                error -> {
                    auctionRefreshInProgress = false;
                    if (handleAccountLockError(error)) {
                        return;
                    }
                    if (ticket != refreshTicket) {
                        refreshData();
                    }
                }
        );
    }

    private void refreshSelectedBidsSnapshot(int auctionId, long ticket) {
        if (auctionId < 0 || bidsRefreshInProgress) {
            return;
        }
        bidsRefreshInProgress = true;
        UiAsync.run(
                () -> loadBidsByAuctionSnapshot(auctionId),
                snapshot -> {
                    bidsRefreshInProgress = false;
                    if (ticket != refreshTicket) {
                        return;
                    }
                    applyBidsByAuctionSnapshot(snapshot);
                },
                error -> {
                    bidsRefreshInProgress = false;
                }
        );
    }

    private void refreshAutobidSnapshot(long ticket) {
        if (autobidRefreshInProgress) {
            return;
        }
        autobidRefreshInProgress = true;
        UiAsync.run(
                () -> loadAutobidSnapshot(),
                snapshot -> {
                    autobidRefreshInProgress = false;
                    if (ticket != refreshTicket) {
                        return;
                    }
                    applyAutobidSnapshot(snapshot);
                },
                error -> {
                    autobidRefreshInProgress = false;
                }
        );
    }

    private void refreshWalletSnapshot(long ticket) {
        if (walletRefreshInProgress) {
            return;
        }
        walletRefreshInProgress = true;
        UiAsync.run(
                this::loadWalletSnapshot,
                snapshot -> {
                    walletRefreshInProgress = false;
                    if (ticket != refreshTicket) {
                        return;
                    }
                    applyWalletSnapshot(snapshot);
                },
                error -> {
                    walletRefreshInProgress = false;
                }
        );
    }

    @FXML
    private void handlePlaceBid() {
        if (auctionController == null || currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "AutoBid is not ready.");
            return;
        }
        if (bidActionInProgress) {
            return;
        }

        AuctionItem selected = tableAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Please select an auction.");
            return;
        }
        if (isCurrentUserLeading(selected)) {
            String message = "You are already the leading bidder for this auction.";
            setBidStatus(message, true);
            NotificationUtil.warning(ownerWindow(), "Notification", message);
            updateBidControlsForAuction(selected);
            return;
        }


        String bidInput = txtBidAmount.getText() == null ? "" : txtBidAmount.getText().trim();
        if (bidInput.isBlank()){
            setBidStatus("Please enter a bid amount before placing a bid.", true);
            NotificationUtil.warning(ownerWindow(), "Notification", "Please enter a bid amount.");
            return;
        }

        try {
            double amount = UiInput.parsePositiveDecimal(bidInput, "Bid amount");
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
            NotificationUtil.warning(ownerWindow(), "Notification", "AutoBid is not ready.");
            return;
        }
        AuctionItem selectedAuction = tableAuctions.getSelectionModel().getSelectedItem();
        if (selectedAuction == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Please select an auction.");
            return;
        }

        String max = maxPrice.getText() == null ? "" : maxPrice.getText().trim();
        String increment = incrementAutobid.getText() == null ? "" : incrementAutobid.getText().trim();
        if (max.isBlank() || increment.isBlank()) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Please enter a bid amount and increment.");
            return;
        }

        try {
            double maxAmount = UiInput.parsePositiveDecimal(max, "Max price");
            double incrementAmount = UiInput.parsePositiveDecimal(increment, "Increment");
            int auctionId = selectedAuction.getId();
            int bidderId = currentUser.getId();
            
            // If user is editing an existing rule, use that rule's ID. Otherwise, create new.
            int targetAutobidId = editingAutobidId >= 0 ? editingAutobidId : -1;
            
            String result;
            if (targetAutobidId < 0){
                result = autobidController.createAutobid(bidderId, auctionId, maxAmount, incrementAmount);
            } else {
                result = autobidController.updateAutobid(bidderId, targetAutobidId, maxAmount, incrementAmount);
                // If update failed because the server-side item no longer exists, try creating a new rule instead.
                if (!"SUCCESS".equals(result) && result != null && result.toLowerCase().contains("not found")) {
                    result = autobidController.createAutobid(bidderId, auctionId, maxAmount, incrementAmount);
                }
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
        if (editingAutobidId < 0) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Please select an auto-bid rule.");
            return;
        }

        try {
            String result = autobidController.deleteAutoBid(currentUser.getId(), editingAutobidId);
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
    private void handleTopUpWallet() {
        if (currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Current user information is unavailable.");
            return;
        }
        if (walletController == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Wallet is not ready.");
            return;
        }
        if (frame == null) {
            NotificationUtil.info(ownerWindow(), "Notification", "Connect this controller to AuthFrame to open wallet top-up.");
            return;
        }
        frame.showTopUpDialog(currentUser, this::refreshData);
    }

    @FXML
    private void handleShowLargeImage() {
        if (imgDetailAuction == null || imgDetailAuction.getImage() == null || !imgDetailAuction.isVisible()) {
            NotificationUtil.info(ownerWindow(), "Notification", "This auction does not have an image to preview.");
            return;
        }

        ImageView enlargedImage = new ImageView(imgDetailAuction.getImage());
        enlargedImage.setPreserveRatio(true);
        enlargedImage.setSmooth(true);
        enlargedImage.setFitWidth(840);
        enlargedImage.setFitHeight(620);

        Rectangle clip = new Rectangle();
        clip.setArcWidth(18);
        clip.setArcHeight(18);
        clip.widthProperty().bind(enlargedImage.fitWidthProperty());
        clip.heightProperty().bind(enlargedImage.fitHeightProperty());
        enlargedImage.setClip(clip);

        Label hint = createLabel(UiText.text("Click anywhere to close"), "image-preview-hint");
        StackPane.setAlignment(hint, Pos.BOTTOM_CENTER);
        StackPane.setMargin(hint, new Insets(0, 0, 18, 0));

        StackPane root = new StackPane(enlargedImage, hint);
        root.getStyleClass().add("image-preview-overlay");
        root.setPadding(new Insets(28));

        Stage dialog = new Stage(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (ownerWindow() != null) {
            dialog.initOwner(ownerWindow());
        }
        dialog.setTitle(lblDetailName == null ? UiText.text("Product image") : lblDetailName.getText());

        Scene scene = new Scene(root, 920, 700);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/userauth/gui/fxml/shared/auth-theme.css").toExternalForm());
        dialog.setScene(scene);
        root.setOnMouseClicked(event -> dialog.close());
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                dialog.close();
            }
        });
        dialog.showAndWait();
    }

    @FXML
    private void handleShowHistory() {
        if (auctionController == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "AuctionController has not been assigned to the bidder screen.");
            return;
        }

        if (currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Current user information is unavailable.");
            return;
        }

        if (frame == null) {
            NotificationUtil.info(ownerWindow(), "Notification", "Connect this controller to AuthFrame to open bid history using FXML.");
            return;
        }

        List<AuctionItem> auctions = allAuctionsSnapshot.isEmpty()
                ? auctionController.getAllAuctionSummaries()
                : allAuctionsSnapshot;
        List<BidTransaction> bids = bidsByAuction.isEmpty()
                ? auctionController.getAllBids()
                : bidsByAuction.values().stream().flatMap(List::stream).toList();
        frame.showBidHistoryDialog(currentUser, auctions, bids);
    }

    @FXML
    private void handleShowInbox() {
        if (auctionController == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "AuctionController has not been assigned to the bidder screen.");
            return;
        }

        if (currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Current user information is unavailable.");
            return;
        }

        if (frame == null) {
            NotificationUtil.info(ownerWindow(), "Notification", "Connect this controller to AuthFrame to open bid history using FXML.");
            return;
        }
        frame.showInboxDialog(notificationController.findUserNotification(currentUser.getId()));
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

    private void syncAutobidFormToSelectedAuction() {
        AuctionItem selected = tableAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            clearAutobidForm();
            return;
        }
        syncAutobidFormToAuction(selected.getId());
    }

    private void syncAutobidFormToAuction(int auctionId) {
        // If user is actively editing (editingAutobidId >= 0), don't auto-sync; preserve the form.
        if (editingAutobidId >= 0) {
            return;
        }
        if (tableAutoBid == null || shouldPreserveAutobidForm()) {
            return;
        }

        AutoBid existing = findAutobidForAuction(auctionId);
        suppressAutobidSelectionSync = true;
        try {
            tableAutoBid.getSelectionModel().clearSelection();
            if (existing != null) {
                tableAutoBid.getSelectionModel().select(existing);
            }
        } finally {
            suppressAutobidSelectionSync = false;
        }

        if (existing == null) {
            clearAutobidForm();
        } else {
            populateAutobidForm(existing);
        }
    }

    private AutoBid findAutobidForAuction(int auctionId) {
        if (tableAutoBid == null || tableAutoBid.getItems() == null) {
            return null;
        }
        return tableAutoBid.getItems().stream()
                .filter(autoBid -> autoBid.getAuctionId() == auctionId)
                .findFirst()
                .orElse(null);
    }

    private AutoBid findAutobidById(int autoBidId) {
        if (autoBidId < 0 || tableAutoBid == null || tableAutoBid.getItems() == null) {
            return null;
        }
        return tableAutoBid.getItems().stream()
                .filter(autoBid -> autoBid.getId() == autoBidId)
                .findFirst()
                .orElse(null);
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

        byte[] imageData = resolveDetailImageData(auction);
        String imageSource = resolveDetailImageSource(auction);
        lblDetailName.setText(auction.getName());
        AuctionImageUtil.applyAuctionImage(imgDetailAuction, lblDetailImageInitial, imageData, imageSource, auction.getName());
        refreshSelectedAuctionImageIfNeeded(auction, imageData);
        lblDetailDescription.setText(auction.getDescription() == null || auction.getDescription().isBlank()
                ? UiText.text("This product does not have a detailed description yet.")
                : auction.getDescription());
        lblDetailCurrentBid.setText(AuctionViewFormatter.formatMoney(auction.getCurrentHighestBid()));
        lblDetailStartPrice.setText(AuctionViewFormatter.formatMoney(auction.getStartPrice()));
        lblDetailLeader.setText(formatLeader(auction));
        lblDetailSchedule.setText(AuctionViewFormatter.formatScheduleRange(auction));
        lblDetailCategory.setText(auction.getCategory());
        lblDetailAntiSniping.setText(AuctionViewFormatter.formatAntiSnipingSummary(auction));

        applyStatusChip(auction);
        applyTimeChip(auction);
        updateBidTrend(bids);
        syncAutobidFormToAuction(auction.getId());
        updateBidControlsForAuction(auction);

        lastSelectedAuctionId = auction.getId();
        lastSelectedWinnerId = auction.getWinnerId();
        lastSelectedHighestBid = auction.getCurrentHighestBid();
        lastSelectedStatus = auction.getStatus();
    }

    private byte[] resolveDetailImageData(AuctionItem auction) {
        if (auction.getImageData() != null && auction.getImageData().length > 0) {
            detailImageDataCache.put(auction.getId(), auction.getImageData());
            detailImageSourceCache.put(auction.getId(), auction.getImageSource());
            return auction.getImageData();
        }
        return detailImageDataCache.get(auction.getId());
    }

    private String resolveDetailImageSource(AuctionItem auction) {
        String cachedSource = detailImageSourceCache.get(auction.getId());
        return cachedSource == null ? auction.getImageSource() : cachedSource;
    }

    private void refreshSelectedAuctionImageIfNeeded(AuctionItem auction, byte[] displayedImageData) {
        if (auctionController == null || auction == null || displayedImageData != null) {
            return;
        }
        if (detailImageRefreshInProgress || detailImageLoadAttempts.contains(auction.getId())) {
            return;
        }
        detailImageRefreshInProgress = true;
        detailImageLoadAttempts.add(auction.getId());
        UiAsync.run(
                () -> loadAuctionDetail(auction.getId()),
                detailedAuction -> {
                    detailImageRefreshInProgress = false;
                    if (detailedAuction == null || selectedAuctionId() != detailedAuction.getId()) {
                        return;
                    }
                    if (detailedAuction.getImageData() != null && detailedAuction.getImageData().length > 0) {
                        detailImageDataCache.put(detailedAuction.getId(), detailedAuction.getImageData());
                        detailImageSourceCache.put(detailedAuction.getId(), detailedAuction.getImageSource());
                        AuctionImageUtil.applyAuctionImage(
                                imgDetailAuction,
                                lblDetailImageInitial,
                                detailedAuction.getImageData(),
                                detailedAuction.getImageSource(),
                                detailedAuction.getName()
                        );
                    }
                },
                error -> detailImageRefreshInProgress = false
        );
    }

    private AuctionItem loadAuctionDetail(int auctionId) {
        try {
            return auctionController.getAuctionById(auctionId);
        } catch (ItemNotFoundException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
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

        if (hasJustFinishedWithWinner(auction)) {
            showWinnerAnnouncement(auction.getId(), auction.getName(), auction.getWinnerId(), auction.getCurrentHighestBid());
        }
    }

    private boolean hasJustFinishedWithWinner(AuctionItem auction) {
        return auction != null
                && auction.getWinnerId() > 0
                && isFinishedStatus(auction.getStatus())
                && !isFinishedStatus(lastSelectedStatus);
    }

    private boolean isFinishedStatus(AuctionStatus status) {
        return status == AuctionStatus.FINISHED || status == AuctionStatus.PAID;
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
        lastSelectedAuctionId = -1;
        lastSelectedWinnerId = -1;
        lastSelectedHighestBid = -1;
        lastSelectedStatus = null;
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
        chartBidTrend.getData().clear();
        updateBidControlsForAuction(null);
    }

    private void updateWalletSummary(Wallet wallet) {
        String zero = AuctionViewFormatter.formatMoney(0);
        if (wallet == null) {
            lblWalletBalance.setText(zero);
            lblWalletAvailable.setText(zero);
            lblWalletReserved.setText(zero);
            return;
        }
        lblWalletBalance.setText(AuctionViewFormatter.formatMoney(wallet.getBalance()));
        lblWalletAvailable.setText(AuctionViewFormatter.formatMoney(wallet.getAvailableBalance()));
        lblWalletReserved.setText(AuctionViewFormatter.formatMoney(wallet.getReservedBalance()));
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

    private void showWinnerAnnouncement(int auctionId, String auctionName, int winnerId, double winningBid) {
        if (winnerOverlay == null || lblWinnerTitle == null || lblWinnerMessage == null || winnerId <= 0) {
            return;
        }
        if (!winnerAnnouncementsShown.add(auctionId)) {
            return;
        }

        boolean currentUserWon = currentUser != null && winnerId == currentUser.getId();
        String winnerText = currentUserWon
                ? UiText.text("You won this auction")
                : UiText.text("Winner: ") + UiText.text("Bidder #") + winnerId;

        lblWinnerTitle.setText(UiText.text("Auction finished"));
        lblWinnerMessage.setText(winnerText + "\n"
                + safeText(auctionName, UiText.text("Selected auction")) + " - "
                + AuctionViewFormatter.formatMoney(winningBid));

        if (winnerOverlayAutoHide != null) {
            winnerOverlayAutoHide.stop();
        }
        winnerOverlay.setVisible(true);
        winnerOverlay.toFront();
        UiEffects.playEntrance(winnerOverlay, 0, 0, 10);

        winnerOverlayAutoHide = new PauseTransition(Duration.seconds(5));
        winnerOverlayAutoHide.setOnFinished(event -> {
            winnerOverlay.setVisible(false);
            winnerOverlay.setOpacity(1);
            winnerOverlay.setTranslateX(0);
            winnerOverlay.setTranslateY(0);
        });
        winnerOverlayAutoHide.play();
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
        refreshTicket++;
        filterRefreshDebounce.playFromStart();
    }

    private void refreshDataFromTimer() {
        if (isUserTypingInBidderForm()) {
            refreshAuctionListOnly();
            return;
        }
        refreshData();
    }

    private void refreshAuctionListOnly() {
        if (!active || auctionController == null || currentUser == null) {
            return;
        }
        long ticket = refreshTicket;
        int requestedSelectedId = selectedAuctionId();
        String keyword = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase(Locale.ROOT);
        String statusFilter = cbStatusFilter.getValue();

        refreshAuctionSnapshot(keyword, statusFilter, requestedSelectedId, ticket);
        refreshWalletSnapshot(ticket);
    }

    private boolean isUserTypingInBidderForm() {
        return isFocused(txtSearch)
                || isFocused(txtBidAmount)
                || isFocused(maxPrice)
                || isFocused(incrementAutobid);
    }

    private boolean isFocused(javafx.scene.Node node) {
        return node != null && node.isFocused();
    }

    private void setBidControlsBusy(boolean busy) {
        if (txtBidAmount != null) {
            txtBidAmount.setDisable(busy);
        }
        if (btnPlaceBid != null) {
            btnPlaceBid.setDisable(busy);
        }
        if (tableAuctions != null) {
            tableAuctions.setDisable(busy);
        }
    }

    private void updateBidControlsForAuction(AuctionItem auction) {
        if (bidActionInProgress) {
            return;
        }
        boolean disableBid = auction == null
                || auction.getStatus() != AuctionStatus.RUNNING
                || isCurrentUserLeading(auction);
        if (txtBidAmount != null) {
            txtBidAmount.setDisable(disableBid);
        }
        if (btnPlaceBid != null) {
            btnPlaceBid.setDisable(disableBid);
        }
    }

    private boolean isCurrentUserLeading(AuctionItem auction) {
        return auction != null
                && currentUser != null
                && auction.getWinnerId() == currentUser.getId();
    }

    private BidderSnapshot loadBidderSnapshot(String keyword, String statusFilter) {
        ensureCurrentAccountActive();
        List<AuctionItem> allAuctions = auctionController.getAllAuctionSummaries();
        List<AuctionItem> visibleAuctions = allAuctions.stream()
                .filter(item -> item.getStatus() != AuctionStatus.CANCELED)
                .toList();
        List<AuctionItem> filteredAuctions = visibleAuctions.stream()
                .filter(item -> matchesSearch(item, keyword))
                .filter(item -> matchesStatusFilter(item, statusFilter))
                .sorted(Comparator
                        .comparingInt((AuctionItem item) -> item.getStatus() == AuctionStatus.RUNNING ? 0 : 1)
                        .thenComparingLong(AuctionItem::getEndTime))
                .toList();

        return new BidderSnapshot(visibleAuctions, filteredAuctions);
    }

    private void ensureCurrentAccountActive() {
        if (authController == null || currentUser == null) {
            return;
        }
        User latest = authController.getUserById(currentUser.getId());
        if (latest == null || "BLOCKED".equals(latest.getStatus())) {
            throw new IllegalStateException("Your account has been locked.");
        }
        currentUser.setStatus(latest.getStatus());
        currentUser.setUpdatedAt(latest.getUpdatedAt());
    }

    private boolean handleAccountLockError(Throwable error) {
        if (!isAccountLockedError(error)) {
            return false;
        }
        currentUser = null;
        deactivate();
        NotificationUtil.warning(ownerWindow(), "Notification", "Your account has been locked. Please contact an admin.");
        if (frame != null) {
            frame.showLogin();
        }
        return true;
    }

    private boolean isAccountLockedError(Throwable error) {
        String message = error == null ? "" : String.valueOf(error.getMessage());
        return message.contains("account has been locked") || message.contains("User not found");
    }

    private void applyBidderSnapshot(BidderSnapshot snapshot, int selectedId) {
        notifyWonAuctionCompletions(snapshot.allAuctions());
        allAuctionsSnapshot = snapshot.allAuctions();
        updateMetrics(snapshot.allAuctions());

        int liveSelectedId = selectedAuctionId();
        int selectionToRestore = liveSelectedId >= 0 ? liveSelectedId : selectedId;

        suppressAuctionSelectionSync = true;
        try {
            tableAuctions.setItems(FXCollections.observableArrayList(snapshot.filteredAuctions()));
            reselectAuction(selectionToRestore);
            if (tableAuctions.getSelectionModel().getSelectedItem() == null && !tableAuctions.getItems().isEmpty()) {
                tableAuctions.getSelectionModel().selectFirst();
            }
        } finally {
            suppressAuctionSelectionSync = false;
        }

        AuctionItem selectedAuction = tableAuctions.getSelectionModel().getSelectedItem();
        renderSelectedAuction(selectedAuction, true);
        tableAuctions.refresh();
        if (selectedAuction != null) {
            refreshSelectedBidsSnapshot(selectedAuction.getId(), refreshTicket);
        }
    }

    private void notifyWonAuctionCompletions(List<AuctionItem> auctions) {
        if (auctions == null || currentUser == null) {
            return;
        }

        if (!auctionStatusSnapshotInitialized) {
            rememberAuctionStatuses(auctions);
            auctionStatusSnapshotInitialized = true;
            return;
        }

        for (AuctionItem auction : auctions) {
            if (auction == null || auction.getWinnerId() != currentUser.getId()) {
                continue;
            }

            AuctionStatus previousStatus = knownAuctionStatuses.get(auction.getId());
            if (isFinishedStatus(auction.getStatus()) && !isFinishedStatus(previousStatus)) {
                showWinnerAnnouncement(
                        auction.getId(),
                        auction.getName(),
                        auction.getWinnerId(),
                        auction.getCurrentHighestBid()
                );
            }
        }

        rememberAuctionStatuses(auctions);
    }

    private void rememberAuctionStatuses(List<AuctionItem> auctions) {
        knownAuctionStatuses.clear();
        for (AuctionItem auction : auctions) {
            if (auction != null) {
                knownAuctionStatuses.put(auction.getId(), auction.getStatus());
            }
        }
    }

    private Map<Integer, List<BidTransaction>> loadBidsByAuctionSnapshot(int auctionId) {
        Map<Integer, List<BidTransaction>> groupedBids = new HashMap<>();
        for (BidTransaction bid : auctionController.getBidsForAuction(auctionId)) {
            groupedBids.computeIfAbsent(bid.getAuctionId(), ignored -> new java.util.ArrayList<>()).add(bid);
        }
        return groupedBids;
    }

    private void applyBidsByAuctionSnapshot(Map<Integer, List<BidTransaction>> groupedBids) {
        bidsByAuction = groupedBids;
        renderSelectedAuction(tableAuctions.getSelectionModel().getSelectedItem(), false);
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
        boolean preserveDraft = shouldPreserveAutobidForm();
        AutobidFormDraft draft = preserveDraft ? captureAutobidDraft() : null;

        suppressAutobidSelectionSync = preserveDraft;
        try {
            tableAutoBid.setItems(FXCollections.observableArrayList(snapshot.allAutobids()));
            reselectAutobid(selectedId);
        } finally {
            suppressAutobidSelectionSync = false;
        }

        if (preserveDraft) {
            restoreAutobidDraft(draft);
        } else {
            syncAutobidFormToSelectedAuction();
        }
        tableAutoBid.refresh();
    }

    private WalletSnapshot loadWalletSnapshot() {
        if (walletController == null || currentUser == null) {
            return new WalletSnapshot(null);
        }
        return new WalletSnapshot(walletController.getWallet(currentUser.getId()));
    }

    private void applyWalletSnapshot(WalletSnapshot snapshot) {
        updateWalletSummary(snapshot.wallet());
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
            List<AuctionItem> filteredAuctions
    ) {}

    private record AutobidSnapshot(
            List<AutoBid> allAutobids
    ) {}

    private record WalletSnapshot(
            Wallet wallet
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
        runAutobidFormProgrammatically(() -> {
            editingAutobidId = autoBid.getId();
            updateAutobidModeLabel();
            maxPrice.setText(AuctionViewFormatter.formatMoney(autoBid.getMaxPrice()));
            incrementAutobid.setText(AuctionViewFormatter.formatMoney(autoBid.getIncrement()));
        });
        autobidFormDirty = false;
    }

    private void clearAutobidForm() {
        runAutobidFormProgrammatically(() -> {
            editingAutobidId = -1;
            updateAutobidModeLabel();
            maxPrice.clear();
            incrementAutobid.clear();
        });
        autobidFormDirty = false;
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

        if (shouldAnnounceWinner(event)) {
            showWinnerAnnouncement(
                    event.auctionId(),
                    auctionNameById(event.auctionId()),
                    event.winnerId(),
                    event.currentHighestBid()
            );
        }

        if (event.auctionId() == lastSelectedAuctionId) {
            switch (event.type()) {
                case ANTI_SNIPING_EXTENDED ->
                        setBidStatus("Anti-sniping extended the auction until " + AuctionViewFormatter.formatDateTimeWithSeconds(event.endTime()) + ".", false);
                case SETTLED -> setBidStatus(event.summary(), event.status() == AuctionStatus.CANCELED);
                case STATUS_CHANGED -> {
                    if (isFinishedStatus(event.status())) {
                        setBidStatus("Auction finished. Waiting for seller settlement.", false);
                    }
                }
                case BID_ACTIVITY -> {
                }
            }
        }

        refreshData();
    }

    private boolean shouldAnnounceWinner(AuctionEvent event) {
        return event.winnerId() > 0
                && isFinishedStatus(event.status())
                && (event.type() == AuctionEvent.AuctionEventType.SETTLED
                || event.type() == AuctionEvent.AuctionEventType.STATUS_CHANGED);
    }

    private String auctionNameById(int auctionId) {
        AuctionItem selected = tableAuctions == null ? null : tableAuctions.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getId() == auctionId) {
            return selected.getName();
        }

        if (tableAuctions != null) {
            for (AuctionItem auction : tableAuctions.getItems()) {
                if (auction != null && auction.getId() == auctionId) {
                    return auction.getName();
                }
            }
        }

        for (AuctionItem auction : allAuctionsSnapshot) {
            if (auction != null && auction.getId() == auctionId) {
                return auction.getName();
            }
        }

        return UiText.text("Auction #") + auctionId;
    }

    private void registerAutobidFormListeners() {
        maxPrice.textProperty().addListener((observable, oldValue, newValue) -> markAutobidFormDirty());
        incrementAutobid.textProperty().addListener((observable, oldValue, newValue) -> markAutobidFormDirty());
    }

    private void markAutobidFormDirty() {
        if (!autobidFormProgrammaticUpdate) {
            autobidFormDirty = true;
        }
    }

    private boolean shouldPreserveAutobidForm() {
        return autobidFormDirty || maxPrice.isFocused() || incrementAutobid.isFocused();
    }

    private AutobidFormDraft captureAutobidDraft() {
        return new AutobidFormDraft(
                editingAutobidId,
                maxPrice.getText(),
                incrementAutobid.getText(),
                autobidFormDirty
        );
    }

    private void restoreAutobidDraft(AutobidFormDraft draft) {
        if (draft == null) {
            return;
        }
        runAutobidFormProgrammatically(() -> {
            editingAutobidId = draft.id();
            updateAutobidModeLabel();
            maxPrice.setText(draft.maxPrice());
            incrementAutobid.setText(draft.increment());
        });
        autobidFormDirty = draft.dirty();
    }

    private void runAutobidFormProgrammatically(Runnable update) {
        autobidFormProgrammaticUpdate = true;
        try {
            update.run();
        } finally {
            autobidFormProgrammaticUpdate = false;
        }
    }

    private void updateAutobidModeLabel() {
        if (lblAutobidMode == null) {
            return;
        }
        lblAutobidMode.setText(editingAutobidId < 0
                ? UiText.text("New auto-bid rule")
                : UiText.text("Editing auto-bid") + " #" + editingAutobidId);
    }

    private record AutobidFormDraft(
            int id,
            String maxPrice,
            String increment,
            boolean dirty
    ) {}
}
