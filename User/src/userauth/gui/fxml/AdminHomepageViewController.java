package userauth.gui.fxml;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import javafx.util.StringConverter;
import userauth.controller.AuctionController;
import userauth.controller.HomepageController;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;
import userauth.model.HomepageAnnouncement;
import userauth.model.User;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminHomepageViewController {
    @FXML
    private TextField txtAnnouncementTitle;

    @FXML
    private TextField txtAnnouncementSchedule;

    @FXML
    private TextArea txtAnnouncementSummary;

    @FXML
    private TextArea txtAnnouncementDetails;

    @FXML
    private ComboBox<AuctionItem> cbLinkedAuction;

    @FXML
    private Button btnSaveAnnouncement;

    @FXML
    private TableView<HomepageAnnouncement> tableAnnouncements;

    @FXML
    private TableColumn<HomepageAnnouncement, Integer> colAnnouncementId;

    @FXML
    private TableColumn<HomepageAnnouncement, String> colAnnouncementTitle;

    @FXML
    private TableColumn<HomepageAnnouncement, String> colAnnouncementAuction;

    @FXML
    private TableColumn<HomepageAnnouncement, String> colAnnouncementSchedule;

    @FXML
    private TableColumn<HomepageAnnouncement, String> colAnnouncementUpdatedAt;

    @FXML
    private TableView<AuctionItem> tableUpcomingAuctions;

    @FXML
    private TableColumn<AuctionItem, Integer> colUpcomingId;

    @FXML
    private TableColumn<AuctionItem, String> colUpcomingName;

    @FXML
    private TableColumn<AuctionItem, String> colUpcomingSchedule;

    @FXML
    private TableColumn<AuctionItem, String> colUpcomingStatus;

    private final Timeline refreshTimeline = new Timeline(
            new KeyFrame(Duration.seconds(2), event -> refreshData())
    );

    private final Map<Integer, AuctionItem> auctionLookup = new HashMap<>();

    private AuthFrame frame;
    private AuctionController auctionController;
    private HomepageController homepageController;
    private User currentUser;
    private int editingAnnouncementId = -1;

    @FXML
    private void initialize() {
        initializeAnnouncementTable();
        initializeUpcomingAuctionTable();
        initializeAuctionComboBox();
        refreshTimeline.setCycleCount(Animation.INDEFINITE);
    }

    public void setFrame(AuthFrame frame) {
        this.frame = frame;
    }

    public void setAuctionController(AuctionController auctionController) {
        this.auctionController = auctionController;
    }

    public void setHomepageController(HomepageController homepageController) {
        this.homepageController = homepageController;
    }

    public void setUser(User user) {
        this.currentUser = user;
        resetForm();
    }

    public void activate() {
        refreshData();
        if (refreshTimeline.getStatus() != Animation.Status.RUNNING) {
            refreshTimeline.play();
        }
    }

    public void deactivate() {
        refreshTimeline.stop();
    }

    public void refreshData() {
        if (auctionController == null || homepageController == null) {
            return;
        }

        Integer selectedLinkedAuctionId = selectedLinkedAuctionId();
        int selectedAnnouncementId = selectedAnnouncementId();
        int selectedPreviewAuctionId = selectedPreviewAuctionId();

        List<AuctionItem> allAuctions = auctionController.getAllAuctions().stream()
                .sorted(Comparator.comparingLong(AuctionItem::getStartTime))
                .toList();
        auctionLookup.clear();
        for (AuctionItem auction : allAuctions) {
            auctionLookup.put(auction.getId(), auction);
        }

        cbLinkedAuction.setItems(FXCollections.observableArrayList(allAuctions));
        restoreLinkedAuctionSelection(selectedLinkedAuctionId);

        List<HomepageAnnouncement> announcements = homepageController.getAllAnnouncements();
        tableAnnouncements.setItems(FXCollections.observableArrayList(announcements));
        reselectAnnouncement(selectedAnnouncementId);

        List<AuctionItem> displayAuctions = allAuctions.stream()
                .filter(item -> item.getStatus() == AuctionStatus.OPEN || item.getStatus() == AuctionStatus.RUNNING)
                .toList();
        tableUpcomingAuctions.setItems(FXCollections.observableArrayList(displayAuctions));
        reselectPreviewAuction(selectedPreviewAuctionId);
        tableAnnouncements.refresh();
        tableUpcomingAuctions.refresh();
    }

    @FXML
    private void handleSaveAnnouncement() {
        if (!hasManagementContext()) {
            return;
        }

        AuctionItem linkedAuction = cbLinkedAuction.getValue();
        String scheduleText = txtAnnouncementSchedule.getText() == null ? "" : txtAnnouncementSchedule.getText().trim();
        if (scheduleText.isEmpty() && linkedAuction != null) {
            scheduleText = AuctionViewFormatter.formatScheduleRange(linkedAuction);
            txtAnnouncementSchedule.setText(scheduleText);
        }

        String result = homepageController.saveAnnouncement(
                currentUser,
                editingAnnouncementId < 0 ? null : editingAnnouncementId,
                txtAnnouncementTitle.getText(),
                txtAnnouncementSummary.getText(),
                txtAnnouncementDetails.getText(),
                scheduleText,
                linkedAuction == null ? null : linkedAuction.getId()
        );

        if ("SUCCESS".equals(result)) {
            NotificationUtil.success(ownerWindow(), "Thong bao", "Da cap nhat bai dang trang chu.");
            resetForm();
            refreshData();
            return;
        }

        NotificationUtil.error(ownerWindow(), "Loi", result);
    }

    @FXML
    private void handleEditSelectedAnnouncement() {
        HomepageAnnouncement selected = tableAnnouncements.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationUtil.warning(ownerWindow(), "Thong bao", "Hay chon mot bai dang de sua.");
            return;
        }

        editingAnnouncementId = selected.getId();
        txtAnnouncementTitle.setText(selected.getTitle());
        txtAnnouncementSchedule.setText(selected.getScheduleText());
        txtAnnouncementSummary.setText(selected.getSummary());
        txtAnnouncementDetails.setText(selected.getDetails());
        selectLinkedAuction(selected.getLinkedAuctionId());
        btnSaveAnnouncement.setText("LUU CAP NHAT");
    }

    @FXML
    private void handleDeleteSelectedAnnouncement() {
        if (!hasManagementContext()) {
            return;
        }

        HomepageAnnouncement selected = tableAnnouncements.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationUtil.warning(ownerWindow(), "Thong bao", "Hay chon mot bai dang de xoa.");
            return;
        }

        boolean confirmed = NotificationUtil.confirm(ownerWindow(), "Xac nhan", "Ban co chac muon xoa bai dang nay khoi trang chu?");
        if (!confirmed) {
            return;
        }

        String result = homepageController.deleteAnnouncement(currentUser, selected.getId());
        if ("SUCCESS".equals(result)) {
            NotificationUtil.success(ownerWindow(), "Thong bao", "Da xoa bai dang trang chu.");
            if (editingAnnouncementId == selected.getId()) {
                resetForm();
            }
            refreshData();
            return;
        }

        NotificationUtil.error(ownerWindow(), "Loi", result);
    }

    @FXML
    private void handleUseSelectedAuctionSchedule() {
        AuctionItem selected = cbLinkedAuction.getValue();
        if (selected == null) {
            selected = tableUpcomingAuctions.getSelectionModel().getSelectedItem();
        }
        if (selected == null) {
            NotificationUtil.warning(ownerWindow(), "Thong bao", "Hay chon mot phien dau gia de lay lich.");
            return;
        }

        cbLinkedAuction.getSelectionModel().select(selected);
        txtAnnouncementSchedule.setText(AuctionViewFormatter.formatScheduleRange(selected));
        if (txtAnnouncementTitle.getText() == null || txtAnnouncementTitle.getText().isBlank()) {
            txtAnnouncementTitle.setText("Thong bao lich dau gia: " + selected.getName());
        }
    }

    @FXML
    private void handleRefreshData() {
        refreshData();
    }

    @FXML
    private void handleClearForm() {
        resetForm();
    }

    @FXML
    private void handleBackToDashboard() {
        deactivate();
        if (frame != null) {
            frame.showAdminDashboard(currentUser);
        }
    }

    @FXML
    private void handleChangePassword() {
        if (currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Thong bao", "Chua co thong tin admin hien tai.");
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
        }
    }

    private void initializeAnnouncementTable() {
        colAnnouncementId.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        colAnnouncementTitle.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getTitle()));
        colAnnouncementAuction.setCellValueFactory(data -> new ReadOnlyStringWrapper(resolveAuctionName(data.getValue().getLinkedAuctionId())));
        colAnnouncementSchedule.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getScheduleText()));
        colAnnouncementUpdatedAt.setCellValueFactory(data -> new ReadOnlyStringWrapper(AuctionViewFormatter.formatDateTime(data.getValue().getUpdatedAt())));
    }

    private void initializeUpcomingAuctionTable() {
        colUpcomingId.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        colUpcomingName.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getName()));
        colUpcomingSchedule.setCellValueFactory(data -> new ReadOnlyStringWrapper(AuctionViewFormatter.formatScheduleRange(data.getValue())));
        colUpcomingStatus.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getStatus().name()));
    }

    private void initializeAuctionComboBox() {
        cbLinkedAuction.setConverter(new StringConverter<>() {
            @Override
            public String toString(AuctionItem auctionItem) {
                if (auctionItem == null) {
                    return "";
                }
                return auctionItem.getName() + " | " + AuctionViewFormatter.formatDateTime(auctionItem.getStartTime());
            }

            @Override
            public AuctionItem fromString(String string) {
                return null;
            }
        });
        cbLinkedAuction.setPromptText("Khong lien ket phien nao");
    }

    private boolean hasManagementContext() {
        if (homepageController == null || auctionController == null) {
            NotificationUtil.warning(ownerWindow(), "Thong bao", "Chua gan du controller cho man hinh quan ly trang chu.");
            return false;
        }
        if (currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Thong bao", "Chua co thong tin admin hien tai.");
            return false;
        }
        return true;
    }

    private void resetForm() {
        editingAnnouncementId = -1;
        txtAnnouncementTitle.clear();
        txtAnnouncementSchedule.clear();
        txtAnnouncementSummary.clear();
        txtAnnouncementDetails.clear();
        cbLinkedAuction.getSelectionModel().clearSelection();
        btnSaveAnnouncement.setText("DANG BAI LEN TRANG CHU");
    }

    private String resolveAuctionName(int auctionId) {
        if (auctionId <= 0) {
            return "Khong lien ket";
        }

        AuctionItem auction = auctionLookup.get(auctionId);
        return auction == null ? "Phien #" + auctionId : auction.getName();
    }

    private Integer selectedLinkedAuctionId() {
        AuctionItem selected = cbLinkedAuction.getValue();
        return selected == null ? null : selected.getId();
    }

    private int selectedAnnouncementId() {
        HomepageAnnouncement selected = tableAnnouncements.getSelectionModel().getSelectedItem();
        return selected == null ? -1 : selected.getId();
    }

    private int selectedPreviewAuctionId() {
        AuctionItem selected = tableUpcomingAuctions.getSelectionModel().getSelectedItem();
        return selected == null ? -1 : selected.getId();
    }

    private void restoreLinkedAuctionSelection(Integer auctionId) {
        if (auctionId == null || auctionId <= 0) {
            if (editingAnnouncementId < 0) {
                cbLinkedAuction.getSelectionModel().clearSelection();
            }
            return;
        }
        selectLinkedAuction(auctionId);
    }

    private void selectLinkedAuction(int auctionId) {
        if (auctionId <= 0) {
            cbLinkedAuction.getSelectionModel().clearSelection();
            return;
        }

        cbLinkedAuction.getItems().stream()
                .filter(item -> item.getId() == auctionId)
                .findFirst()
                .ifPresent(item -> cbLinkedAuction.getSelectionModel().select(item));
    }

    private void reselectAnnouncement(int announcementId) {
        if (announcementId < 0) {
            return;
        }

        tableAnnouncements.getItems().stream()
                .filter(item -> item.getId() == announcementId)
                .findFirst()
                .ifPresent(item -> tableAnnouncements.getSelectionModel().select(item));
    }

    private void reselectPreviewAuction(int auctionId) {
        if (auctionId < 0) {
            return;
        }

        tableUpcomingAuctions.getItems().stream()
                .filter(item -> item.getId() == auctionId)
                .findFirst()
                .ifPresent(item -> tableUpcomingAuctions.getSelectionModel().select(item));
    }

    private javafx.stage.Window ownerWindow() {
        return frame == null ? null : frame.getWindow();
    }
}
