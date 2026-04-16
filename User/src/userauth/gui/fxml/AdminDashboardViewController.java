package userauth.gui.fxml;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Duration;
import userauth.controller.AuctionController;
import userauth.controller.AuthController;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;
import userauth.model.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminDashboardViewController {
    private static final String SORT_DEFAULT = "Mac dinh";
    private static final String SORT_NAME = "Ten san pham A-Z";
    private static final String SORT_PRICE = "Gia cao nhat giam dan";
    private static final String SORT_ENDING = "Sap het gio";
    private static final String SORT_CATEGORY = "Danh muc A-Z";

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

    private final Timeline auctionRefreshTimeline = new Timeline(
            new KeyFrame(Duration.seconds(1), event -> refreshAuctionData())
    );

    private final Map<Integer, User> userLookup = new HashMap<>();
    private Map<Integer, Integer> countdownSnapshot = Map.of();

    private AuthFrame frame;
    private AuthController authController;
    private AuctionController auctionController;
    private User currentUser;

    @FXML
    private void initialize() {
        initializeUserTable();
        initializeAuctionTable();
        initializeSortOptions();

        auctionRefreshTimeline.setCycleCount(Animation.INDEFINITE);
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

    public void setUser(User user) {
        this.currentUser = user;
    }

    public void activate() {
        refreshData();
        if (auctionRefreshTimeline.getStatus() != Animation.Status.RUNNING) {
            auctionRefreshTimeline.play();
        }
    }

    public void deactivate() {
        auctionRefreshTimeline.stop();
    }

    public void refreshData() {
        refreshUsers();
        refreshAuctionData();
    }

    @FXML
    private void handleRefreshUsers() {
        refreshUsers();
    }

    @FXML
    private void handleApplyAuctionSort() {
        refreshAuctionData();
    }

    @FXML
    private void handleRefreshAuctions() {
        refreshAuctionData();
    }

    @FXML
    private void handleStartEarlyCloseCountdown() {
        if (!hasAuctionManagementContext()) {
            return;
        }

        AuctionItem selected = getSelectedAuction("Hay chon mot phien dau gia dang dien ra.");
        if (selected == null) {
            return;
        }

        String result = auctionController.startAdminEarlyCloseCountdown(currentUser, selected.getId());
        handleActionResult(result, "Da bat dau dem 3 lan. Neu khong co bid moi, he thong se ket thuc som.", this::refreshAuctionData);
    }

    @FXML
    private void handleCancelEarlyCloseCountdown() {
        if (!hasAuctionManagementContext()) {
            return;
        }

        AuctionItem selected = getSelectedAuction("Hay chon mot phien dau gia dang duoc dem som.");
        if (selected == null) {
            return;
        }

        String result = auctionController.cancelAdminEarlyCloseCountdown(currentUser, selected.getId());
        handleActionResult(result, "Da huy lenh dem ket thuc som.", this::refreshAuctionData);
    }

    @FXML
    private void handleToggleStatus() {
        if (authController == null) {
            NotificationUtil.warning(ownerWindow(), "Thong bao", "Chua gan AuthController cho man hinh admin.");
            return;
        }
        if (currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Thong bao", "Chua co thong tin nguoi dung hien tai.");
            return;
        }

        User selected = getSelectedUser("Hay chon mot tai khoan.");
        if (selected == null) {
            return;
        }

        String result = authController.toggleUserStatus(currentUser.getUsername(), selected.getId());
        handleActionResult(result, "Cap nhat trang thai tai khoan thanh cong.", this::refreshUsers);
    }

    @FXML
    private void handleChangePassword() {
        if (currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Thong bao", "Chua co nguoi dung hien tai.");
            return;
        }
        if (frame == null) {
            NotificationUtil.info(ownerWindow(), "Thong bao", "Da gan su kien doi mat khau. Hay noi controller voi AuthFrame khi tich hop.");
            return;
        }

        frame.showChangePasswordDialog(currentUser);
    }

    @FXML
    private void handleLogout() {
        currentUser = null;
        deactivate();
        if (frame != null) {
            frame.showLogin();
        } else {
            NotificationUtil.info(ownerWindow(), "Thong bao", "Da gan su kien dang xuat. Hay noi controller voi AuthFrame khi tich hop.");
        }
    }

    private void refreshUsers() {
        if (authController == null) {
            return;
        }

        int selectedId = selectedUserId();
        try {
            List<User> users = authController.getAllUsersList();
            userLookup.clear();
            for (User user : users) {
                userLookup.put(user.getId(), user);
            }

            tableUsers.setItems(FXCollections.observableArrayList(users));
            reselectUser(selectedId);
        } catch (Exception ex) {
            NotificationUtil.error(ownerWindow(), "Loi", "Khong the tai danh sach khach hang.");
        }
    }

    private void refreshAuctionData() {
        if (auctionController == null) {
            return;
        }

        int selectedAuctionId = selectedAuctionId();
        countdownSnapshot = auctionController.getAdminEarlyCloseCountdowns();

        List<AuctionItem> auctions = new ArrayList<>(auctionController.getAllAuctions());
        sortAuctions(auctions);

        tableAuctions.setItems(FXCollections.observableArrayList(auctions));
        reselectAuction(selectedAuctionId);
        tableAuctions.refresh();
    }

    private void initializeUserTable() {
        colId.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        colUsername.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getUsername()));
        colFullName.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getFullName()));
        colEmail.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getEmail()));
        colRole.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getRoleName()));
        colStatus.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getStatus()));
    }

    private void initializeAuctionTable() {
        colAuctionId.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        colAuctionName.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getName()));
        colAuctionSeller.setCellValueFactory(data -> new ReadOnlyStringWrapper(resolveSellerName(data.getValue().getSellerId())));
        colAuctionCategory.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getCategory()));
        colAuctionHighestBid.setCellValueFactory(data -> new ReadOnlyStringWrapper(AuctionViewFormatter.formatMoney(data.getValue().getCurrentHighestBid())));
        colAuctionStatus.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getStatus().name()));
        colAuctionTimeLeft.setCellValueFactory(data -> new ReadOnlyStringWrapper(AuctionViewFormatter.formatTimeLeft(data.getValue())));
        colAuctionCountdown.setCellValueFactory(data -> new ReadOnlyStringWrapper(formatCountdown(data.getValue())));
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
    }

    private void sortAuctions(List<AuctionItem> auctions) {
        String sortOption = cbAuctionSort == null ? SORT_DEFAULT : cbAuctionSort.getValue();
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
            return "Seller ID " + sellerId;
        }
        return seller.getFullName() + " (" + seller.getUsername() + ")";
    }

    private String formatCountdown(AuctionItem item) {
        if (item.getStatus() != AuctionStatus.RUNNING) {
            return "-";
        }

        Integer remaining = countdownSnapshot.get(item.getId());
        if (remaining == null) {
            return "Chua kich hoat";
        }
        return "Con " + remaining + " lan";
    }

    private boolean hasAuctionManagementContext() {
        if (auctionController == null) {
            NotificationUtil.warning(ownerWindow(), "Thong bao", "Chua gan AuctionController cho man hinh admin.");
            return false;
        }
        if (currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Thong bao", "Chua co thong tin admin hien tai.");
            return false;
        }
        return true;
    }

    private AuctionItem getSelectedAuction(String emptyMessage) {
        AuctionItem selected = tableAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationUtil.warning(ownerWindow(), "Thong bao", emptyMessage);
        }
        return selected;
    }

    private User getSelectedUser(String emptyMessage) {
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationUtil.warning(ownerWindow(), "Thong bao", emptyMessage);
        }
        return selected;
    }

    private void handleActionResult(String result, String successMessage, Runnable successAction) {
        if ("SUCCESS".equals(result)) {
            NotificationUtil.success(ownerWindow(), "Thong bao", successMessage);
            successAction.run();
            return;
        }
        NotificationUtil.error(ownerWindow(), "Loi", result);
    }

    private javafx.stage.Window ownerWindow() {
        return frame == null ? null : frame.getWindow();
    }
}
