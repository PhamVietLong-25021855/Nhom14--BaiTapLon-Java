package userauth.gui.fxml;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.util.Duration;
import userauth.controller.AuctionController;
import userauth.controller.AuthController;
import userauth.controller.HomepageController;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;
import userauth.model.User;
import userauth.util.AdminDefaults;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

// Ghi chu file: File controller JavaFX; dieu khien hanh vi cua man hinh, hop thoai hoac thanh phan UI.
// Khai bao lop AdminDashboardViewController; dieu khien mot man hinh hoac thanh phan JavaFX cu the.
public class AdminDashboardViewController {
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho default.
    private static final String SORT_DEFAULT = "Default";
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho name.
    private static final String SORT_NAME = "Product Name A-Z";
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho price.
    private static final String SORT_PRICE = "Highest Bid Descending";
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho ending.
    private static final String SORT_ENDING = "Ending Soon";
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho category.
    private static final String SORT_CATEGORY = "Category A-Z";
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho ms.
    private static final long ENDING_SOON_THRESHOLD_MS = 5 * 60 * 1000;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho table users.
    private TableView<User> tableUsers;

    @FXML
    private TableColumn<User, Integer> colId;

    @FXML
    private TableColumn<User, String> colUsername;

    @FXML
    private TableColumn<User, String> colFullName;

    @FXML
    private TableColumn<User, String> colEmail;

    @FXML
    private TableColumn<User, String> colRole;

    @FXML
    private TableColumn<User, String> colStatus;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho cb auction sort.
    private ComboBox<String> cbAuctionSort;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho btn promote admin.
    private Button btnPromoteAdmin;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho btn demote admin.
    private Button btnDemoteAdmin;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho table auctions.
    private TableView<AuctionItem> tableAuctions;

    @FXML
    private TableColumn<AuctionItem, Integer> colAuctionId;

    @FXML
    private TableColumn<AuctionItem, String> colAuctionName;

    @FXML
    private TableColumn<AuctionItem, String> colAuctionSeller;

    @FXML
    private TableColumn<AuctionItem, String> colAuctionCategory;

    @FXML
    private TableColumn<AuctionItem, String> colAuctionHighestBid;

    @FXML
    private TableColumn<AuctionItem, String> colAuctionStatus;

    @FXML
    private TableColumn<AuctionItem, String> colAuctionTimeLeft;

    @FXML
    private TableColumn<AuctionItem, String> colAuctionCountdown;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl admin sidebar.
    private Label lblAdminSidebar;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl admin name.
    private Label lblAdminName;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl total users.
    private Label lblTotalUsers;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl running auctions.
    private Label lblRunningAuctions;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl finished auctions.
    private Label lblFinishedAuctions;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl total bids.
    private Label lblTotalBids;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl user selection summary.
    private Label lblUserSelectionSummary;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl auction selection summary.
    private Label lblAuctionSelectionSummary;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho auction status chart.
    private PieChart auctionStatusChart;

    private final Timeline auctionRefreshTimeline = new Timeline(
    // Phuong thuc: thuc hien chuc nang key frame trong lop AdminDashboardViewController.
            new KeyFrame(Duration.seconds(5), event -> refreshData())
    );

    private final Map<Integer, User> userLookup = new HashMap<>();
    private Map<Integer, Integer> countdownSnapshot = Map.of();
    // Thuoc tinh: luu trang thai hoac du lieu tam cho refresh ticket.
    private long refreshTicket;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho action in progress.
    private boolean actionInProgress;
    // Thuoc tinh: giu tham chieu den AuthFrame de phoi hop xu ly.
    private AuthFrame frame;
    // Thuoc tinh: giu tham chieu den AuthController de phoi hop xu ly.
    private AuthController authController;
    // Thuoc tinh: giu tham chieu den AuctionController de phoi hop xu ly.
    private AuctionController auctionController;
    // Thuoc tinh: giu tham chieu den HomepageController de phoi hop xu ly.
    private HomepageController homepageController;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho current user.
    private User currentUser;

    @FXML
    // Phuong thuc: khoi dong hoac khoi tao tien trinh initialize.
    private void initialize() {
        initializeUserTable();
        initializeAuctionTable();
        initializeSortOptions();
        registerSelectionListeners();
        auctionRefreshTimeline.setCycleCount(Animation.INDEFINITE);
        auctionStatusChart.setAnimated(false);
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set frame.
    public void setFrame(AuthFrame frame) {
        this.frame = frame;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set auth controller.
    public void setAuthController(AuthController authController) {
        this.authController = authController;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set auction controller.
    public void setAuctionController(AuctionController auctionController) {
        this.auctionController = auctionController;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set homepage controller.
    public void setHomepageController(HomepageController homepageController) {
        this.homepageController = homepageController;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set user.
    public void setUser(User user) {
        this.currentUser = user;
        String name = user == null ? UiText.text("Admin") : user.getFullName() + " (" + user.getUsername() + ")";
        lblAdminName.setText(name);
        lblAdminSidebar.setText(name);
        if (btnPromoteAdmin != null) {
            btnPromoteAdmin.setDisable(!isDefaultAdmin(user));
        }
        if (btnDemoteAdmin != null) {
            btnDemoteAdmin.setDisable(!isDefaultAdmin(user));
        }
    }
    // Phuong thuc: thuc hien chuc nang activate trong lop AdminDashboardViewController.
    public void activate() {
        refreshData();
        if (auctionRefreshTimeline.getStatus() != Animation.Status.RUNNING) {
            auctionRefreshTimeline.play();
        }
    }
    // Phuong thuc: thuc hien chuc nang deactivate trong lop AdminDashboardViewController.
    public void deactivate() {
        refreshTicket++;
        auctionRefreshTimeline.stop();
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac refresh data.
    public void refreshData() {
        if (authController == null || auctionController == null) {
            return;
        }

        long ticket = ++refreshTicket;
        int selectedUserId = selectedUserId();
        int selectedAuctionId = selectedAuctionId();
        String sortOption = cbAuctionSort == null ? SORT_DEFAULT : cbAuctionSort.getValue();

        UiAsync.run(
                () -> loadAdminSnapshot(sortOption),
                snapshot -> {
                    if (ticket != refreshTicket) {
                        return;
                    }
                    applyAdminSnapshot(snapshot, selectedUserId, selectedAuctionId);
                },
                error -> {
                    if (ticket != refreshTicket) {
                        return;
                    }
                    lblUserSelectionSummary.setText(UiText.text("Unable to load admin data."));
                    lblAuctionSelectionSummary.setText(UiText.text("Unable to load auction data."));
                }
        );
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle refresh users.
    private void handleRefreshUsers() {
        refreshData();
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle apply auction sort.
    private void handleApplyAuctionSort() {
        refreshData();
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle refresh auctions.
    private void handleRefreshAuctions() {
        refreshData();
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle open homepage manager.
    private void handleOpenHomepageManager() {
        if (frame == null) {
            NotificationUtil.info(ownerWindow(), "Notification", "This screen has not been connected to AuthFrame yet.");
            return;
        }
        if (currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Current admin information is unavailable.");
            return;
        }
        if (homepageController == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "HomepageController has not been assigned to the admin screen.");
            return;
        }

        deactivate();
        frame.showAdminHomepageManager(currentUser);
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle start early close countdown.
    private void handleStartEarlyCloseCountdown() {
        if (!hasAuctionManagementContext()) {
            return;
        }
        if (actionInProgress) {
            return;
        }

        AuctionItem selected = getSelectedAuction("Please select a running auction.");
        if (selected == null) {
            return;
        }

        int auctionId = selected.getId();
        runActionAsync(
                () -> auctionController.startAdminEarlyCloseCountdown(currentUser, auctionId),
                "The 3-count early close countdown has started. If no new bid arrives, the auction will close early."
        );
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle cancel early close countdown.
    private void handleCancelEarlyCloseCountdown() {
        if (!hasAuctionManagementContext()) {
            return;
        }
        if (actionInProgress) {
            return;
        }

        AuctionItem selected = getSelectedAuction("Please select an auction with an active early-close countdown.");
        if (selected == null) {
            return;
        }

        int auctionId = selected.getId();
        runActionAsync(
                () -> auctionController.cancelAdminEarlyCloseCountdown(currentUser, auctionId),
                "The early-close countdown has been cancelled."
        );
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle toggle status.
    private void handleToggleStatus() {
        if (authController == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "AuthController has not been assigned to the admin screen.");
            return;
        }
        if (currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Current user information is unavailable.");
            return;
        }
        if (actionInProgress) {
            return;
        }

        User selected = getSelectedUser("Please select an account.");
        if (selected == null) {
            return;
        }

        int selectedUserId = selected.getId();
        runActionAsync(
                () -> authController.toggleUserStatus(currentUser.getUsername(), selectedUserId),
                "Account status updated successfully."
        );
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle promote to admin.
    private void handlePromoteToAdmin() {
        if (authController == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "AuthController has not been assigned to the admin screen.");
            return;
        }
        if (currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Current user information is unavailable.");
            return;
        }
        if (actionInProgress) {
            return;
        }

        User selected = getSelectedUser("Please select an account.");
        if (selected == null) {
            return;
        }

        int selectedUserId = selected.getId();
        runActionAsync(
                () -> authController.promoteUserToAdmin(currentUser.getUsername(), selectedUserId),
                "Account promoted to admin successfully."
        );
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle demote admin.
    private void handleDemoteAdmin() {
        if (authController == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "AuthController has not been assigned to the admin screen.");
            return;
        }
        if (currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Current user information is unavailable.");
            return;
        }
        if (actionInProgress) {
            return;
        }

        User selected = getSelectedUser("Please select an account.");
        if (selected == null) {
            return;
        }

        int selectedUserId = selected.getId();
        runActionAsync(
                () -> authController.demoteAdminToBidder(currentUser.getUsername(), selectedUserId),
                "Admin account demoted successfully."
        );
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle change password.
    private void handleChangePassword() {
        if (currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Current user is unavailable.");
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
    // Phuong thuc: lay hoac doc du lieu cho thao tac load admin snapshot.
    private AdminSnapshot loadAdminSnapshot(String sortOption) {
        List<User> users = authController.getAllUsersList();
        Map<Integer, Integer> countdowns = auctionController.getAdminEarlyCloseCountdowns();
        List<AuctionItem> auctions = new ArrayList<>(auctionController.getAllAuctions());
        sortAuctions(auctions, sortOption);
        int totalBids = auctionController.getAllBids().size();
        return new AdminSnapshot(users, auctions, countdowns, totalBids);
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac apply admin snapshot.
    private void applyAdminSnapshot(AdminSnapshot snapshot, int selectedUserId, int selectedAuctionId) {
        userLookup.clear();
        for (User user : snapshot.users()) {
            userLookup.put(user.getId(), user);
        }

        tableUsers.setItems(FXCollections.observableArrayList(snapshot.users()));
        reselectUser(selectedUserId);
        lblTotalUsers.setText(String.valueOf(snapshot.users().size()));
        updateUserSelectionSummary(tableUsers.getSelectionModel().getSelectedItem());

        countdownSnapshot = snapshot.countdowns();
        tableAuctions.setItems(FXCollections.observableArrayList(snapshot.auctions()));
        reselectAuction(selectedAuctionId);
        tableAuctions.refresh();
        updateAuctionMetrics(snapshot.auctions(), snapshot.totalBids());
        updateAuctionSelectionSummary(tableAuctions.getSelectionModel().getSelectedItem());
        updateAuctionStatusChart(snapshot.auctions());
    }
    // Phuong thuc: khoi dong hoac khoi tao tien trinh initialize user table.
    private void initializeUserTable() {
        colId.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        colUsername.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getUsername()));
        colFullName.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getFullName()));
        colEmail.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getEmail()));
        colRole.setCellValueFactory(data -> new ReadOnlyStringWrapper(UiText.text(data.getValue().getRoleName())));
        colStatus.setCellValueFactory(data -> new ReadOnlyStringWrapper(UiText.userStatus(data.getValue().getStatus())));
    }
    // Phuong thuc: khoi dong hoac khoi tao tien trinh initialize auction table.
    private void initializeAuctionTable() {
        colAuctionId.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        colAuctionName.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getName()));
        colAuctionSeller.setCellValueFactory(data -> new ReadOnlyStringWrapper(resolveSellerName(data.getValue().getSellerId())));
        colAuctionCategory.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getCategory()));
        colAuctionHighestBid.setCellValueFactory(data -> new ReadOnlyStringWrapper(AuctionViewFormatter.formatMoney(data.getValue().getCurrentHighestBid())));
        colAuctionStatus.setCellValueFactory(data -> new ReadOnlyStringWrapper(UiText.auctionStatus(data.getValue().getStatus())));
        colAuctionTimeLeft.setCellValueFactory(data -> new ReadOnlyStringWrapper(AuctionViewFormatter.formatTimeLeft(data.getValue())));
        colAuctionCountdown.setCellValueFactory(data -> new ReadOnlyStringWrapper(formatCountdown(data.getValue())));
        tableAuctions.setRowFactory(this::createAuctionRow);
    }
    // Phuong thuc: khoi dong hoac khoi tao tien trinh initialize sort options.
    private void initializeSortOptions() {
        cbAuctionSort.getItems().addAll(
                SORT_DEFAULT,
                SORT_NAME,
                SORT_PRICE,
                SORT_ENDING,
                SORT_CATEGORY
        );
        cbAuctionSort.setValue(SORT_DEFAULT);
        UiText.configureTranslatedComboBox(cbAuctionSort);
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac register selection listeners.
    private void registerSelectionListeners() {
        tableUsers.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
                updateUserSelectionSummary(newValue));
        tableAuctions.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
                updateAuctionSelectionSummary(newValue));
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac update auction metrics.
    private void updateAuctionMetrics(List<AuctionItem> auctions, int totalBids) {
        long running = auctions.stream().filter(item -> item.getStatus() == AuctionStatus.RUNNING).count();
        long finished = auctions.stream()
                .filter(item -> item.getStatus() == AuctionStatus.FINISHED
                        || item.getStatus() == AuctionStatus.PAID
                        || item.getStatus() == AuctionStatus.CANCELED)
                .count();

        lblRunningAuctions.setText(String.valueOf(running));
        lblFinishedAuctions.setText(String.valueOf(finished));
        lblTotalBids.setText(String.valueOf(totalBids));
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac update auction status chart.
    private void updateAuctionStatusChart(List<AuctionItem> auctions) {
        Map<String, Integer> counts = new HashMap<>();
        for (AuctionItem auction : auctions) {
            counts.merge(UiText.auctionStatus(auction.getStatus()), 1, Integer::sum);
        }

        auctionStatusChart.setData(FXCollections.observableArrayList(
                counts.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(entry -> new PieChart.Data(entry.getKey(), entry.getValue()))
                        .toList()
        ));
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac update user selection summary.
    private void updateUserSelectionSummary(User selected) {
        if (selected == null) {
            lblUserSelectionSummary.setText(UiText.text("Select a user to view a summary here."));
            return;
        }

        lblUserSelectionSummary.setText(
                selected.getFullName() + " | " + UiText.text(selected.getRoleName()) + " | " + UiText.text("Status") + ": " + UiText.userStatus(selected.getStatus())
        );
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac update auction selection summary.
    private void updateAuctionSelectionSummary(AuctionItem selected) {
        if (selected == null) {
            lblAuctionSelectionSummary.setText(UiText.text("Select an auction to track its countdown and status here."));
            return;
        }

        lblAuctionSelectionSummary.setText(
                selected.getName()
                        + " | " + UiText.auctionStatus(selected.getStatus())
                        + " | " + AuctionViewFormatter.formatTimeLeft(selected)
                        + " | " + formatCountdown(selected)
        );
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
                        || item.getStatus() == AuctionStatus.PAID
                        || item.getStatus() == AuctionStatus.CANCELED) {
                    getStyleClass().add("table-row-closed");
                }
            }
        };
    }
    // Phuong thuc: thuc hien chuc nang sort auctions trong lop AdminDashboardViewController.
    private void sortAuctions(List<AuctionItem> auctions, String sortOption) {
        switch (sortOption) {
            case SORT_NAME -> auctions.sort(Comparator.comparing(item -> item.getName().toLowerCase()));
            case SORT_PRICE -> auctions.sort(Comparator.comparingDouble(AuctionItem::getCurrentHighestBid).reversed());
            case SORT_ENDING -> auctions.sort(Comparator.comparingLong(AuctionItem::getEndTime));
            case SORT_CATEGORY -> auctions.sort(Comparator.comparing(item -> item.getCategory().toLowerCase()));
            default -> {
            }
        }
    }
    // Phuong thuc: thuc hien chuc nang selected user id trong lop AdminDashboardViewController.
    private int selectedUserId() {
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        return selected == null ? -1 : selected.getId();
    }
    // Phuong thuc: thuc hien chuc nang selected auction id trong lop AdminDashboardViewController.
    private int selectedAuctionId() {
        AuctionItem selected = tableAuctions.getSelectionModel().getSelectedItem();
        return selected == null ? -1 : selected.getId();
    }
    // Phuong thuc: thuc hien chuc nang reselect user trong lop AdminDashboardViewController.
    private void reselectUser(int selectedId) {
        if (selectedId < 0) {
            return;
        }

        tableUsers.getItems().stream()
                .filter(user -> user.getId() == selectedId)
                .findFirst()
                .ifPresent(user -> tableUsers.getSelectionModel().select(user));
    }
    // Phuong thuc: thuc hien chuc nang reselect auction trong lop AdminDashboardViewController.
    private void reselectAuction(int selectedId) {
        if (selectedId < 0) {
            return;
        }

        tableAuctions.getItems().stream()
                .filter(item -> item.getId() == selectedId)
                .findFirst()
                .ifPresent(item -> tableAuctions.getSelectionModel().select(item));
    }
    // Phuong thuc: thuc hien chuc nang resolve seller name trong lop AdminDashboardViewController.
    private String resolveSellerName(int sellerId) {
        User seller = userLookup.get(sellerId);
        if (seller == null) {
            return UiText.text("Seller ID") + " " + sellerId;
        }
        return seller.getFullName() + " (" + seller.getUsername() + ")";
    }
    // Phuong thuc: bien doi du lieu cho thao tac format countdown.
    private String formatCountdown(AuctionItem item) {
        if (item.getStatus() != AuctionStatus.RUNNING) {
            return "-";
        }

        Integer remaining = countdownSnapshot.get(item.getId());
        if (remaining == null) {
            return UiText.text("Not active");
        }
        return remaining + " " + UiText.text("counts left");
    }
    // Phuong thuc: kiem tra dieu kien hoac xac thuc cho thao tac has auction management context.
    private boolean hasAuctionManagementContext() {
        if (auctionController == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "AuctionController has not been assigned to the admin screen.");
            return false;
        }
        if (currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Current admin information is unavailable.");
            return false;
        }
        return true;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get selected auction.
    private AuctionItem getSelectedAuction(String emptyMessage) {
        AuctionItem selected = tableAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", emptyMessage);
        }
        return selected;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get selected user.
    private User getSelectedUser(String emptyMessage) {
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", emptyMessage);
        }
        return selected;
    }
    // Phuong thuc: kiem tra dieu kien hoac xac thuc cho thao tac is default admin.
    private boolean isDefaultAdmin(User user) {
        return user != null
                && user.getRole() == userauth.model.Role.ADMIN
                && AdminDefaults.USERNAME.equalsIgnoreCase(user.getUsername());
    }
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle action result.
    private void handleActionResult(String result, String successMessage, Runnable successAction) {
        if ("SUCCESS".equals(result)) {
            NotificationUtil.success(ownerWindow(), "Notification", successMessage);
            successAction.run();
            return;
        }
        NotificationUtil.error(ownerWindow(), "Error", result);
    }
    // Phuong thuc: thuc hien chuc nang run action async trong lop AdminDashboardViewController.
    private void runActionAsync(Supplier<String> action, String successMessage) {
        actionInProgress = true;
        setActionBusy(true);
        UiAsync.run(
                action::get,
                result -> {
                    actionInProgress = false;
                    setActionBusy(false);
                    handleActionResult(result, successMessage, this::refreshData);
                },
                error -> {
                    actionInProgress = false;
                    setActionBusy(false);
                    NotificationUtil.error(ownerWindow(), "Error", "Unable to complete this action right now.");
                }
        );
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set action busy.
    private void setActionBusy(boolean busy) {
        if (tableUsers != null) {
            tableUsers.setDisable(busy);
        }
        if (tableAuctions != null) {
            tableAuctions.setDisable(busy);
        }
        if (cbAuctionSort != null) {
            cbAuctionSort.setDisable(busy);
        }
    }
    // Phuong thuc: thuc hien chuc nang owner window trong lop AdminDashboardViewController.
    private javafx.stage.Window ownerWindow() {
        return frame == null ? null : frame.getWindow();
    }
    // Phuong thuc: thuc hien chuc nang switch language trong lop AdminDashboardViewController.
    private void switchLanguage(AppLanguage language) {
        if (frame == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Language settings are unavailable.");
            return;
        }
        frame.setLanguage(language);
        UiText.refreshTranslatedComboBox(cbAuctionSort);
        refreshData();
        NotificationUtil.success(ownerWindow(), "Notification", "Language updated.");
    }
    // Phuong thuc: thuc hien chuc nang admin snapshot trong lop AdminDashboardViewController.
    private record AdminSnapshot(
            List<User> users,
            List<AuctionItem> auctions,
            Map<Integer, Integer> countdowns,
            int totalBids
    ) {
    }
}
