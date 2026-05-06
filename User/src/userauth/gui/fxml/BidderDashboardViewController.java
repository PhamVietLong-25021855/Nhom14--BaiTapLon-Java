package userauth.gui.fxml;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
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
import userauth.controller.AuctionController;
import userauth.controller.AutobidController;
import userauth.controller.WalletController;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;
import userauth.model.AutoBid;
import userauth.model.BidTransaction;
import userauth.model.User;
import userauth.model.Wallet;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Ghi chu file: File controller JavaFX; dieu khien hanh vi cua man hinh, hop thoai hoac thanh phan UI.
// Khai bao lop BidderDashboardViewController; dieu khien mot man hinh hoac thanh phan JavaFX cu the.
public class BidderDashboardViewController {
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho all.
    private static final String FILTER_ALL = "All";
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho running.
    private static final String FILTER_RUNNING = "Running";
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho open.
    private static final String FILTER_OPEN = "Opening Soon";
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho finished.
    private static final String FILTER_FINISHED = "Finished";
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho ms.
    private static final long ENDING_SOON_THRESHOLD_MS = 5 * 60 * 1000;
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho time.
    private static final DateTimeFormatter LIVE_TIME =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho table auctions.
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
    // Thuoc tinh: luu trang thai hoac du lieu tam cho table auto bid.
    private TableView<AutoBid> tableAutoBid;

    @FXML
    private TableColumn<AutoBid, Integer> colIdAB;

    @FXML
    private TableColumn<AutoBid, Integer> colItemAB;

    @FXML
    private TableColumn<AutoBid, Double> colMaxPriceAB;

    @FXML
    private TableColumn<AutoBid, Double> colIncrementAB;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho txt search.
    private TextField txtSearch;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho cb status filter.
    private ComboBox<String> cbStatusFilter;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl sidebar user.
    private Label lblSidebarUser;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl wallet balance.
    private Label lblWalletBalance;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl user name.
    private Label lblUserName;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl running count.
    private Label lblRunningCount;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl ending soon count.
    private Label lblEndingSoonCount;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl leading count.
    private Label lblLeadingCount;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl detail name.
    private Label lblDetailName;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho img detail auction.
    private ImageView imgDetailAuction;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl detail image initial.
    private Label lblDetailImageInitial;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl detail description.
    private Label lblDetailDescription;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl detail current bid.
    private Label lblDetailCurrentBid;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl detail state.
    private Label lblDetailState;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl detail time left.
    private Label lblDetailTimeLeft;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl detail start price.
    private Label lblDetailStartPrice;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl detail leader.
    private Label lblDetailLeader;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl detail schedule.
    private Label lblDetailSchedule;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl detail category.
    private Label lblDetailCategory;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl bid status.
    private Label lblBidStatus;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl live bid count.
    private Label lblLiveBidCount;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho txt bid amount.
    private TextField txtBidAmount;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho bids live container.
    private VBox bidsLiveContainer;

    @FXML
    private LineChart<Number, Number> chartBidTrend;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho x axis bid trend.
    private NumberAxis xAxisBidTrend;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho y axis bid trend.
    private NumberAxis yAxisBidTrend;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho id autobid.
    private TextField idAutobid;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho max price.
    private TextField maxPrice;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho increment autobid.
    private TextField incrementAutobid;
    // Thuoc tinh: giu tham chieu den AuthFrame de phoi hop xu ly.
    private AuthFrame frame;
    // Thuoc tinh: giu tham chieu den AuctionController de phoi hop xu ly.
    private AuctionController auctionController;
    // Thuoc tinh: giu tham chieu den AutobidController de phoi hop xu ly.
    private AutobidController autobidController;
    // Thuoc tinh: giu tham chieu den WalletController de phoi hop xu ly.
    private WalletController walletController;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho current user.
    private User currentUser;
    // Polling Ä‘á»‹nh ká»³ Ä‘á»ƒ mÃ n hÃ¬nh bidder luÃ´n bÃ¡m sÃ¡t thay Ä‘á»•i cá»§a auction.
    // Thuoc tinh: luu trang thai hoac du lieu tam cho timeline.
    private Timeline timeline;
    private final PauseTransition filterRefreshDebounce = new PauseTransition(Duration.millis(220));
    // Cache bid history theo auction Ä‘á»ƒ render panel chi tiáº¿t nhanh hÆ¡n.
    private Map<Integer, List<BidTransaction>> bidsByAuction = Map.of();
    // Thuoc tinh: luu trang thai hoac du lieu tam cho last selected auction id.
    private int lastSelectedAuctionId = -1;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho last selected winner id.
    private int lastSelectedWinnerId = -1;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho last selected highest bid.
    private double lastSelectedHighestBid = -1;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho refresh ticket.
    private long refreshTicket;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho bid action in progress.
    private boolean bidActionInProgress;

    @FXML
    // Phuong thuc: khoi dong hoac khoi tao tien trinh initialize.
    private void initialize() {
        colId.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        colName.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getName()));
        colCategory.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getCategory()));
        colHighestBid.setCellValueFactory(data -> new ReadOnlyStringWrapper(AuctionViewFormatter.formatMoney(data.getValue().getCurrentHighestBid())));
        colStatus.setCellValueFactory(data -> new ReadOnlyStringWrapper(UiText.auctionStatus(data.getValue().getStatus())));
        colTimeLeft.setCellValueFactory(data -> new ReadOnlyStringWrapper(AuctionViewFormatter.formatTimeLeft(data.getValue())));

        if (colIdAB != null) {
            colIdAB.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        }
        if (colItemAB != null) {
            colItemAB.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getAuctionId()));
        }
        if (colMaxPriceAB != null) {
            colMaxPriceAB.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getMaxPrice()));
        }
        if (colIncrementAB != null) {
            colIncrementAB.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getIncrement()));
        }

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

        if (tableAutoBid != null) {
            tableAutoBid.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
                    populateAutobidEditor(newValue));
        }

        chartBidTrend.setAnimated(false);
        xAxisBidTrend.setAutoRanging(true);
        yAxisBidTrend.setAutoRanging(true);

        timeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> refreshData()));
        timeline.setCycleCount(Animation.INDEFINITE);

        setBidStatus("Select an auction to view details.", false);
        showEmptySelectionState();
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set frame.
    public void setFrame(AuthFrame frame) {
        this.frame = frame;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set auction controller.
    public void setAuctionController(AuctionController auctionController) {
        this.auctionController = auctionController;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set autobid controller.
    public void setAutobidController(AutobidController autobidController) {
        this.autobidController = autobidController;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set wallet controller.
    public void setWalletController(WalletController walletController) {
        this.walletController = walletController;
        updateWalletBalanceAsync();
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set user.
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
        clearAutobidEditor();
        applyWalletBalance(null);
        updateWalletBalanceAsync();
    }
    // Phuong thuc: thuc hien chuc nang activate trong lop BidderDashboardViewController.
    public void activate() {
        refreshData();
        if (timeline != null && timeline.getStatus() != Animation.Status.RUNNING) {
            timeline.play();
        }
    }
    // Phuong thuc: thuc hien chuc nang deactivate trong lop BidderDashboardViewController.
    public void deactivate() {
        refreshTicket++;
        if (timeline != null) {
            timeline.stop();
        }
        filterRefreshDebounce.stop();
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac refresh data.
    public void refreshData() {
        if (auctionController == null || currentUser == null) {
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

        if (autobidController != null && tableAutoBid != null) {
            UiAsync.run(
                    this::loadAutobidSnapshot,
                    this::applyAutobidSnapshot,
                    error -> {
                    }
            );
        }

        updateWalletBalanceAsync();
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle place bid.
    private void handlePlaceBid() {
        if (auctionController == null || currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Bid placement is not ready.");
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

        String bidInput = txtBidAmount.getText() == null ? "" : txtBidAmount.getText().trim();
        if (bidInput.isBlank()) {
            setBidStatus("Please enter a bid amount before placing a bid.", true);
            NotificationUtil.warning(ownerWindow(), "Notification", "Please enter a bid amount.");
            return;
        }

        try {
            double amount = Double.parseDouble(bidInput);
            submitBid(amount, selected.getId(), currentUser.getId());
        } catch (NumberFormatException ex) {
            setBidStatus("Invalid amount.", true);
            NotificationUtil.error(ownerWindow(), "Error", "Invalid amount.");
        }
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle autobid.
    private void handleAutobid() {
        if (autobidController == null || currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Auto bid is not ready.");
            return;
        }

        AuctionItem selectedAuction = tableAuctions.getSelectionModel().getSelectedItem();
        if (selectedAuction == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Please select an auction first.");
            return;
        }

        String idInput = safeInput(idAutobid);
        String maxInput = safeInput(maxPrice);
        String incrementInput = safeInput(incrementAutobid);

        if (idInput.isBlank()) {
            if (maxInput.isBlank() || incrementInput.isBlank()) {
                NotificationUtil.warning(ownerWindow(), "Notification", "Enter max price and increment to create an auto bid.");
                return;
            }
            try {
                String result = autobidController.createAutobid(
                        currentUser.getId(),
                        selectedAuction.getId(),
                        Double.parseDouble(maxInput),
                        Double.parseDouble(incrementInput)
                );
                handleAutobidResult(result, "Auto bid created successfully.");
            } catch (NumberFormatException ex) {
                NotificationUtil.error(ownerWindow(), "Error", "Invalid auto bid number.");
            }
            return;
        }

        try {
            int autobidId = Integer.parseInt(idInput);
            if (maxInput.isBlank() && incrementInput.isBlank()) {
                String result = autobidController.deleteAutoBid(currentUser.getId(), autobidId);
                handleAutobidResult(result, "Auto bid deleted successfully.");
                return;
            }
            if (maxInput.isBlank() || incrementInput.isBlank()) {
                NotificationUtil.warning(ownerWindow(), "Notification", "Provide both max price and increment, or leave both blank to delete.");
                return;
            }

            String result = autobidController.updateAutobid(
                    currentUser.getId(),
                    autobidId,
                    Double.parseDouble(maxInput),
                    Double.parseDouble(incrementInput)
            );
            handleAutobidResult(result, "Auto bid updated successfully.");
        } catch (NumberFormatException ex) {
            NotificationUtil.error(ownerWindow(), "Error", "Invalid auto bid number.");
        }
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle show history.
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
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle top up.
    private void handleTopUp() {
        if (walletController == null || currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Wallet top-up is not ready.");
            return;
        }

        LoadedView<TopUpDialogController> view = FxmlRuntime.loadView(
                BidderDashboardViewController.class,
                "top-up-dialog.fxml",
                "dialog"
        );
        javafx.stage.Stage dialog = FxmlRuntime.createModalDialog(ownerWindow(), "TOP UP WALLET", view.root(), 480, 380);
        view.controller().setDialogStage(dialog);
        view.controller().setWalletController(walletController);
        view.controller().setUser(currentUser);
        view.controller().setSuccessHandler(message -> {
            updateWalletBalanceAsync();
            NotificationUtil.success(ownerWindow(), "Notification", message);
        });
        dialog.showAndWait();
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle change password.
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
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle switch to english.
    private void handleSwitchToEnglish() {
        switchLanguage(AppLanguage.ENGLISH);
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle switch to vietnamese.
    private void handleSwitchToVietnamese() {
        switchLanguage(AppLanguage.VIETNAMESE);
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle logout.
    private void handleLogout() {
        currentUser = null;
        deactivate();
        if (frame != null) {
            frame.showLogin();
        } else {
            NotificationUtil.info(ownerWindow(), "Notification", "The logout action is prepared. Connect this controller to AuthFrame when integrating.");
        }
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac create auction row.
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
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac update metrics.
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
    // Phuong thuc: thuc hien chuc nang matches search trong lop BidderDashboardViewController.
    private boolean matchesSearch(AuctionItem item, String keyword) {
        if (keyword.isBlank()) {
            return true;
        }

        return item.getName().toLowerCase(Locale.ROOT).contains(keyword)
                || item.getCategory().toLowerCase(Locale.ROOT).contains(keyword);
    }
    // Phuong thuc: thuc hien chuc nang matches status filter trong lop BidderDashboardViewController.
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
    // Phuong thuc: thuc hien chuc nang selected auction id trong lop BidderDashboardViewController.
    private int selectedAuctionId() {
        AuctionItem selected = tableAuctions.getSelectionModel().getSelectedItem();
        return selected == null ? -1 : selected.getId();
    }
    // Phuong thuc: thuc hien chuc nang reselect auction trong lop BidderDashboardViewController.
    private void reselectAuction(int selectedId) {
        if (selectedId < 0) {
            return;
        }

        tableAuctions.getItems().stream()
                .filter(item -> item.getId() == selectedId)
                .findFirst()
                .ifPresent(item -> tableAuctions.getSelectionModel().select(item));
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac render selected auction.
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
        lblLiveBidCount.setText(formatTransactionCount(bids.size()));

        applyStatusChip(auction);
        applyTimeChip(auction);
        populateBidFeed(bids);
        updateBidTrend(bids);

        lastSelectedAuctionId = auction.getId();
        lastSelectedWinnerId = auction.getWinnerId();
        lastSelectedHighestBid = auction.getCurrentHighestBid();
    }
    // Phuong thuc: thuc hien chuc nang notify selected auction state changes trong lop BidderDashboardViewController.
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
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac apply status chip.
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
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac apply time chip.
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
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac populate bid feed.
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
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac update bid trend.
    private void updateBidTrend(List<BidTransaction> bids) {
        chartBidTrend.getData().clear();
        if (bids == null || bids.isEmpty()) {
            return;
        }

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        List<BidTransaction> orderedBids = bids.stream()
                .sorted(Comparator.comparingLong(BidTransaction::getTimestamp))
                .toList();

        int index = 1;
        for (BidTransaction bid : orderedBids) {
            series.getData().add(new XYChart.Data<>(index++, bid.getAmount()));
        }
        chartBidTrend.getData().add(series);
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac show empty selection state.
    private void showEmptySelectionState() {
        lblDetailName.setText(UiText.text("Select an auction to view details"));
        AuctionImageUtil.applyAuctionImage(imgDetailAuction, lblDetailImageInitial, null, null, "A");
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
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set bid status.
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
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac create label.
    private Label createLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add(styleClass);
        return label;
    }
    // Phuong thuc: bien doi du lieu cho thao tac format leader.
    private String formatLeader(AuctionItem auction) {
        if (auction.getWinnerId() <= 0) {
            return UiText.text("No leading bidder yet");
        }
        if (currentUser != null && auction.getWinnerId() == currentUser.getId()) {
            return UiText.text("You are leading");
        }
        return UiText.text("Bidder #") + auction.getWinnerId();
    }
    // Phuong thuc: thuc hien chuc nang owner window trong lop BidderDashboardViewController.
    private javafx.stage.Window ownerWindow() {
        return frame == null ? null : frame.getWindow();
    }
    // Phuong thuc: bien doi du lieu cho thao tac format transaction count.
    private String formatTransactionCount(int count) {
        return count + " " + UiText.text("transactions");
    }
    // Phuong thuc: thuc hien chuc nang switch language trong lop BidderDashboardViewController.
    private void switchLanguage(AppLanguage language) {
        if (frame == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Language settings are unavailable.");
            return;
        }
        frame.setLanguage(language);
        UiText.refreshTranslatedComboBox(cbStatusFilter);
        tableAuctions.refresh();
        if (tableAutoBid != null) {
            tableAutoBid.refresh();
        }
        renderSelectedAuction(tableAuctions.getSelectionModel().getSelectedItem(), false);
        NotificationUtil.success(ownerWindow(), "Notification", "Language updated.");
    }
    // Phuong thuc: thuc hien chuc nang schedule refresh data trong lop BidderDashboardViewController.
    private void scheduleRefreshData() {
        filterRefreshDebounce.playFromStart();
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set bid controls busy.
    private void setBidControlsBusy(boolean busy) {
        if (txtBidAmount != null) {
            txtBidAmount.setDisable(busy);
        }
        if (tableAuctions != null) {
            tableAuctions.setDisable(busy);
        }
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac load bidder snapshot.
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
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac apply bidder snapshot.
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
    // Phuong thuc: lay hoac doc du lieu cho thao tac load autobid snapshot.
    private AutobidSnapshot loadAutobidSnapshot() {
        return new AutobidSnapshot(autobidController.getAutobidByBidder(currentUser.getId()));
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac apply autobid snapshot.
    private void applyAutobidSnapshot(AutobidSnapshot snapshot) {
        if (tableAutoBid == null) {
            return;
        }

        int selectedId = tableAutoBid.getSelectionModel().getSelectedItem() == null
                ? -1
                : tableAutoBid.getSelectionModel().getSelectedItem().getId();
        tableAutoBid.setItems(FXCollections.observableArrayList(snapshot.allAutobids()));
        if (selectedId >= 0) {
            tableAutoBid.getItems().stream()
                    .filter(item -> item.getId() == selectedId)
                    .findFirst()
                    .ifPresent(item -> tableAutoBid.getSelectionModel().select(item));
        }
        tableAutoBid.refresh();
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac populate autobid editor.
    private void populateAutobidEditor(AutoBid autobid) {
        if (autobid == null) {
            clearAutobidEditor();
            return;
        }

        idAutobid.setText(String.valueOf(autobid.getId()));
        maxPrice.setText(String.valueOf(autobid.getMaxPrice()));
        incrementAutobid.setText(String.valueOf(autobid.getIncrement()));
    }
    // Phuong thuc: huy, xoa, dong hoac don trang thai cho thao tac clear autobid editor.
    private void clearAutobidEditor() {
        if (idAutobid != null) {
            idAutobid.clear();
        }
        if (maxPrice != null) {
            maxPrice.clear();
        }
        if (incrementAutobid != null) {
            incrementAutobid.clear();
        }
    }
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac submit bid.
    private void submitBid(double amount, int auctionId, int bidderId) {
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
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle autobid result.
    private void handleAutobidResult(String result, String successMessage) {
        if ("SUCCESS".equals(result)) {
            NotificationUtil.success(ownerWindow(), "Notification", successMessage);
            clearAutobidEditor();
            refreshData();
            return;
        }
        NotificationUtil.error(ownerWindow(), "Error", result);
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac update wallet balance async.
    private void updateWalletBalanceAsync() {
        if (walletController == null || currentUser == null || lblWalletBalance == null) {
            return;
        }

        int userId = currentUser.getId();
        UiAsync.run(
                () -> walletController.getWallet(userId),
                wallet -> {
                    if (currentUser != null && currentUser.getId() == userId) {
                        applyWalletBalance(wallet);
                    }
                },
                error -> {
                    if (currentUser != null && currentUser.getId() == userId) {
                        applyWalletBalance(null);
                    }
                }
        );
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac apply wallet balance.
    private void applyWalletBalance(Wallet wallet) {
        if (lblWalletBalance == null) {
            return;
        }
        if (wallet == null) {
            lblWalletBalance.setText(
                    UiText.text("Available: ") + AuctionViewFormatter.formatMoney(0.0) + "\n" +
                            UiText.text("Reserved: ") + AuctionViewFormatter.formatMoney(0.0) + "\n" +
                            UiText.text("Total: ") + AuctionViewFormatter.formatMoney(0.0)
            );
            return;
        }

        lblWalletBalance.setText(
                UiText.text("Available: ") + AuctionViewFormatter.formatMoney(wallet.getAvailableBalance()) + "\n" +
                        UiText.text("Reserved: ") + AuctionViewFormatter.formatMoney(wallet.getReservedBalance()) + "\n" +
                        UiText.text("Total: ") + AuctionViewFormatter.formatMoney(wallet.getBalance())
        );
    }
    // Phuong thuc: thuc hien chuc nang safe input trong lop BidderDashboardViewController.
    private String safeInput(TextField field) {
        if (field == null || field.getText() == null) {
            return "";
        }
        return field.getText().trim();
    }
    // Phuong thuc: thuc hien chuc nang resolve display name trong lop BidderDashboardViewController.
    private String resolveDisplayName(User user) {
        String fullName = safeText(user.getFullName(), "");
        if (!fullName.isBlank()) {
            return fullName;
        }
        return safeText(user.getUsername(), UiText.text("Bidder"));
    }
    // Phuong thuc: thuc hien chuc nang safe text trong lop BidderDashboardViewController.
    private String safeText(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }
    // Phuong thuc: thuc hien chuc nang abbreviate trong lop BidderDashboardViewController.
    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac bidder snapshot.
    private record BidderSnapshot(
            List<AuctionItem> allAuctions,
            List<AuctionItem> filteredAuctions,
            Map<Integer, List<BidTransaction>> groupedBids
    ) {
    }
    // Phuong thuc: thuc hien chuc nang autobid snapshot trong lop BidderDashboardViewController.
    private record AutobidSnapshot(
            List<AutoBid> allAutobids
    ) {
    }
}
