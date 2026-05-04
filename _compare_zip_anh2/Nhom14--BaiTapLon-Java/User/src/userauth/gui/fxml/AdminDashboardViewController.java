package userauth.gui.fxml;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class AdminDashboardViewController {
    private static final String SORT_DEFAULT = "Default";
    private static final String SORT_NAME = "Product Name A-Z";
    private static final String SORT_PRICE = "Highest Bid Descending";
    private static final String SORT_ENDING = "Ending Soon";
    private static final String SORT_CATEGORY = "Category A-Z";
    private static final long ENDING_SOON_THRESHOLD_MS = 5 * 60 * 1000;

    @FXML
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
    private ComboBox<String> cbAuctionSort;

    @FXML
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
    private Label lblAdminSidebar;

    @FXML
    private Label lblAdminName;

    @FXML
    private Label lblTotalUsers;

    @FXML
    private Label lblRunningAuctions;

    @FXML
    private Label lblFinishedAuctions;

    @FXML
    private Label lblTotalBids;

    @FXML
    private Label lblUserSelectionSummary;

    @FXML
    private Label lblAuctionSelectionSummary;

    @FXML
    private PieChart auctionStatusChart;

    private final Timeline auctionRefreshTimeline = new Timeline(
            new KeyFrame(Duration.seconds(5), event -> refreshData())
    );

    private final Map<Integer, User> userLookup = new HashMap<>();
    private Map<Integer, Integer> countdownSnapshot = Map.of();
    private long refreshTicket;
    private boolean actionInProgress;

    private AuthFrame frame;
    private AuthController authController;
    private AuctionController auctionController;
    private HomepageController homepageController;
    private User currentUser;

    @FXML
    private void initialize() {
        initializeUserTable();
        initializeAuctionTable();
        initializeSortOptions();
        registerSelectionListeners();
        auctionRefreshTimeline.setCycleCount(Animation.INDEFINITE);
        auctionStatusChart.setAnimated(false);
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

    public void setHomepageController(HomepageController homepageController) {
        this.homepageController = homepageController;
    }

    public void setUser(User user) {
        this.currentUser = user;
        String name = user == null ? UiText.text("Admin") : user.getFullName() + " (" + user.getUsername() + ")";
        lblAdminName.setText(name);
        lblAdminSidebar.setText(name);
    }

    public void activate() {
        refreshData();
        if (auctionRefreshTimeline.getStatus() != Animation.Status.RUNNING) {
            auctionRefreshTimeline.play();
        }
    }

    public void deactivate() {
        refreshTicket++;
        auctionRefreshTimeline.stop();
    }

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
    private void handleRefreshUsers() {
        refreshData();
    }

    @FXML
    private void handleApplyAuctionSort() {
        refreshData();
    }

    @FXML
    private void handleRefreshAuctions() {
        refreshData();
    }

    @FXML
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

    private AdminSnapshot loadAdminSnapshot(String sortOption) {
        List<User> users = authController.getAllUsersList();
        Map<Integer, Integer> countdowns = auctionController.getAdminEarlyCloseCountdowns();
        List<AuctionItem> auctions = new ArrayList<>(auctionController.getAllAuctions());
        sortAuctions(auctions, sortOption);
        int totalBids = auctionController.getAllBids().size();
        return new AdminSnapshot(users, auctions, countdowns, totalBids);
    }

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

    private void initializeUserTable() {
        colId.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        colUsername.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getUsername()));
        colFullName.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getFullName()));
        colEmail.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getEmail()));
        colRole.setCellValueFactory(data -> new ReadOnlyStringWrapper(UiText.text(data.getValue().getRoleName())));
        colStatus.setCellValueFactory(data -> new ReadOnlyStringWrapper(UiText.userStatus(data.getValue().getStatus())));
    }

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

    private void registerSelectionListeners() {
        tableUsers.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
                updateUserSelectionSummary(newValue));
        tableAuctions.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
                updateAuctionSelectionSummary(newValue));
    }

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

    private void updateUserSelectionSummary(User selected) {
        if (selected == null) {
            lblUserSelectionSummary.setText(UiText.text("Select a user to view a summary here."));
            return;
        }

        lblUserSelectionSummary.setText(
                selected.getFullName() + " | " + UiText.text(selected.getRoleName()) + " | " + UiText.text("Status") + ": " + UiText.userStatus(selected.getStatus())
        );
    }

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

    private int selectedUserId() {
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        return selected == null ? -1 : selected.getId();
    }

    private int selectedAuctionId() {
        AuctionItem selected = tableAuctions.getSelectionModel().getSelectedItem();
        return selected == null ? -1 : selected.getId();
    }

    private void reselectUser(int selectedId) {
        if (selectedId < 0) {
            return;
        }

        tableUsers.getItems().stream()
                .filter(user -> user.getId() == selectedId)
                .findFirst()
                .ifPresent(user -> tableUsers.getSelectionModel().select(user));
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

    private String resolveSellerName(int sellerId) {
        User seller = userLookup.get(sellerId);
        if (seller == null) {
            return UiText.text("Seller ID") + " " + sellerId;
        }
        return seller.getFullName() + " (" + seller.getUsername() + ")";
    }

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

    private AuctionItem getSelectedAuction(String emptyMessage) {
        AuctionItem selected = tableAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", emptyMessage);
        }
        return selected;
    }

    private User getSelectedUser(String emptyMessage) {
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", emptyMessage);
        }
        return selected;
    }

    private void handleActionResult(String result, String successMessage, Runnable successAction) {
        if ("SUCCESS".equals(result)) {
            NotificationUtil.success(ownerWindow(), "Notification", successMessage);
            successAction.run();
            return;
        }
        NotificationUtil.error(ownerWindow(), "Error", result);
    }

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

    private javafx.stage.Window ownerWindow() {
        return frame == null ? null : frame.getWindow();
    }

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

    private record AdminSnapshot(
            List<User> users,
            List<AuctionItem> auctions,
            Map<Integer, Integer> countdowns,
            int totalBids
    ) {
    }
}
