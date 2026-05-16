package uet.auctionsystem.gui.fxml;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import javafx.util.StringConverter;
import uet.auctionsystem.controller.AuctionController;
import uet.auctionsystem.controller.HomepageController;
import uet.auctionsystem.model.AuctionItem;
import uet.auctionsystem.model.AuctionStatus;
import uet.auctionsystem.model.HomepageAnnouncement;
import uet.auctionsystem.model.User;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

// Ghi chu file: File controller JavaFX; dieu khien hanh vi cua man hinh, hop thoai hoac thanh phan UI.
// Khai bao lop AdminHomepageViewController; dieu khien mot man hinh hoac thanh phan JavaFX cu the.
public class AdminHomepageViewController {
    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho txt announcement title.
    private TextField txtAnnouncementTitle;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho txt announcement schedule.
    private TextField txtAnnouncementSchedule;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho txt announcement summary.
    private TextArea txtAnnouncementSummary;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho txt announcement details.
    private TextArea txtAnnouncementDetails;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho cb linked auction.
    private ComboBox<AuctionItem> cbLinkedAuction;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho btn save announcement.
    private Button btnSaveAnnouncement;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho table announcements.
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
    // Thuoc tinh: luu trang thai hoac du lieu tam cho table upcoming auctions.
    private TableView<AuctionItem> tableUpcomingAuctions;

    @FXML
    private TableColumn<AuctionItem, Integer> colUpcomingId;

    @FXML
    private TableColumn<AuctionItem, String> colUpcomingName;

    @FXML
    private TableColumn<AuctionItem, String> colUpcomingSchedule;

    @FXML
    private TableColumn<AuctionItem, String> colUpcomingStatus;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl cms sidebar.
    private Label lblCmsSidebar;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl cms admin name.
    private Label lblCmsAdminName;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl announcement count.
    private Label lblAnnouncementCount;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl linked count.
    private Label lblLinkedCount;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl upcoming count.
    private Label lblUpcomingCount;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl preview title.
    private Label lblPreviewTitle;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl preview schedule.
    private Label lblPreviewSchedule;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl preview summary.
    private Label lblPreviewSummary;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl preview linked auction.
    private Label lblPreviewLinkedAuction;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl preview details.
    private Label lblPreviewDetails;

    private final Timeline refreshTimeline = new Timeline(
    // Phuong thuc: thuc hien chuc nang key frame trong lop AdminHomepageViewController.
            new KeyFrame(Duration.seconds(5), event -> refreshData())
    );

    private final Map<Integer, AuctionItem> auctionLookup = new HashMap<>();
    // Thuoc tinh: giu tham chieu den AuthFrame de phoi hop xu ly.
    private AuthFrame frame;
    // Thuoc tinh: giu tham chieu den AuctionController de phoi hop xu ly.
    private AuctionController auctionController;
    // Thuoc tinh: giu tham chieu den HomepageController de phoi hop xu ly.
    private HomepageController homepageController;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho current user.
    private User currentUser;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho editing announcement id.
    private int editingAnnouncementId = -1;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho refresh ticket.
    private long refreshTicket;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho action in progress.
    private boolean actionInProgress;

    @FXML
    // Phuong thuc: khoi dong hoac khoi tao tien trinh initialize.
    private void initialize() {
        initializeAnnouncementTable();
        initializeUpcomingAuctionTable();
        initializeAuctionComboBox();
        registerPreviewListeners();
        refreshTimeline.setCycleCount(Animation.INDEFINITE);
        updatePreview();
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set frame.
    public void setFrame(AuthFrame frame) {
        this.frame = frame;
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
        String name = user == null ? UiText.text("Admin CMS") : user.getFullName() + " (" + user.getUsername() + ")";
        lblCmsAdminName.setText(name);
        lblCmsSidebar.setText(name);
        resetForm();
    }
    // Phuong thuc: thuc hien chuc nang activate trong lop AdminHomepageViewController.
    public void activate() {
        refreshData();
        if (refreshTimeline.getStatus() != Animation.Status.RUNNING) {
            refreshTimeline.play();
        }
    }
    // Phuong thuc: thuc hien chuc nang deactivate trong lop AdminHomepageViewController.
    public void deactivate() {
        refreshTicket++;
        refreshTimeline.stop();
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac refresh data.
    public void refreshData() {
        if (auctionController == null || homepageController == null) {
            return;
        }

        long ticket = ++refreshTicket;
        Integer selectedLinkedAuctionId = selectedLinkedAuctionId();
        int selectedAnnouncementId = selectedAnnouncementId();
        int selectedPreviewAuctionId = selectedPreviewAuctionId();

        UiAsync.run(
                this::loadHomepageSnapshot,
                snapshot -> {
                    if (ticket != refreshTicket) {
                        return;
                    }
                    applyHomepageSnapshot(snapshot, selectedLinkedAuctionId, selectedAnnouncementId, selectedPreviewAuctionId);
                },
                error -> {
                    if (ticket != refreshTicket) {
                        return;
                    }
                    lblPreviewTitle.setText(UiText.text("Unable to load homepage data"));
                }
        );
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle save announcement.
    private void handleSaveAnnouncement() {
        if (!hasManagementContext()) {
            return;
        }
        if (actionInProgress) {
            return;
        }

        AuctionItem linkedAuction = cbLinkedAuction.getValue();
        String scheduleText = txtAnnouncementSchedule.getText() == null ? "" : txtAnnouncementSchedule.getText().trim();
        if (scheduleText.isEmpty() && linkedAuction != null) {
            scheduleText = AuctionViewFormatter.formatScheduleRange(linkedAuction);
            txtAnnouncementSchedule.setText(scheduleText);
        }
        Integer announcementId = editingAnnouncementId < 0 ? null : editingAnnouncementId;
        Integer linkedAuctionId = linkedAuction == null ? null : linkedAuction.getId();
        String title = txtAnnouncementTitle.getText();
        String summary = txtAnnouncementSummary.getText();
        String details = txtAnnouncementDetails.getText();
        String finalScheduleText = scheduleText;

        runActionAsync(
                () -> homepageController.saveAnnouncement(
                        currentUser,
                        announcementId,
                        title,
                        summary,
                        details,
                        finalScheduleText,
                        linkedAuctionId
                ),
                "Homepage announcement updated successfully.",
                () -> {
                    resetForm();
                    refreshData();
                }
        );
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle edit selected announcement.
    private void handleEditSelectedAnnouncement() {
        HomepageAnnouncement selected = tableAnnouncements.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Please select an announcement to edit.");
            return;
        }

        editingAnnouncementId = selected.getId();
        txtAnnouncementTitle.setText(selected.getTitle());
        txtAnnouncementSchedule.setText(selected.getScheduleText());
        txtAnnouncementSummary.setText(selected.getSummary());
        txtAnnouncementDetails.setText(selected.getDetails());
        selectLinkedAuction(selected.getLinkedAuctionId());
        btnSaveAnnouncement.setText(UiText.text("SAVE CHANGES"));
        updatePreview();
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle delete selected announcement.
    private void handleDeleteSelectedAnnouncement() {
        if (!hasManagementContext()) {
            return;
        }
        if (actionInProgress) {
            return;
        }

        HomepageAnnouncement selected = tableAnnouncements.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Please select an announcement to delete.");
            return;
        }

        boolean confirmed = NotificationUtil.confirm(ownerWindow(), "Confirm", "Are you sure you want to remove this announcement from the homepage?");
        if (!confirmed) {
            return;
        }

        int announcementId = selected.getId();
        runActionAsync(
                () -> homepageController.deleteAnnouncement(currentUser, announcementId),
                "Homepage announcement deleted successfully.",
                () -> {
                    if (editingAnnouncementId == announcementId) {
                        resetForm();
                    }
                    refreshData();
                }
        );
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle use selected auction schedule.
    private void handleUseSelectedAuctionSchedule() {
        AuctionItem selected = cbLinkedAuction.getValue();
        if (selected == null) {
            selected = tableUpcomingAuctions.getSelectionModel().getSelectedItem();
        }
        if (selected == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Please select an auction to use its schedule.");
            return;
        }

        cbLinkedAuction.getSelectionModel().select(selected);
        txtAnnouncementSchedule.setText(AuctionViewFormatter.formatScheduleRange(selected));
        if (txtAnnouncementTitle.getText() == null || txtAnnouncementTitle.getText().isBlank()) {
            txtAnnouncementTitle.setText(UiText.text("Auction schedule update") + ": " + selected.getName());
        }
        updatePreview();
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle refresh data.
    private void handleRefreshData() {
        refreshData();
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle clear form.
    private void handleClearForm() {
        resetForm();
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle back to dashboard.
    private void handleBackToDashboard() {
        deactivate();
        if (frame != null) {
            frame.showAdminDashboard(currentUser);
        }
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle change password.
    private void handleChangePassword() {
        if (currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Current admin information is unavailable.");
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
        }
    }
    // Phuong thuc: khoi dong hoac khoi tao tien trinh initialize announcement table.
    private void initializeAnnouncementTable() {
        colAnnouncementId.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        colAnnouncementTitle.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getTitle()));
        colAnnouncementAuction.setCellValueFactory(data -> new ReadOnlyStringWrapper(resolveAuctionName(data.getValue().getLinkedAuctionId())));
        colAnnouncementSchedule.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getScheduleText()));
        colAnnouncementUpdatedAt.setCellValueFactory(data -> new ReadOnlyStringWrapper(AuctionViewFormatter.formatDateTime(data.getValue().getUpdatedAt())));
    }
    // Phuong thuc: khoi dong hoac khoi tao tien trinh initialize upcoming auction table.
    private void initializeUpcomingAuctionTable() {
        colUpcomingId.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        colUpcomingName.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getName()));
        colUpcomingSchedule.setCellValueFactory(data -> new ReadOnlyStringWrapper(AuctionViewFormatter.formatScheduleRange(data.getValue())));
        colUpcomingStatus.setCellValueFactory(data -> new ReadOnlyStringWrapper(UiText.auctionStatus(data.getValue().getStatus())));
    }
    // Phuong thuc: khoi dong hoac khoi tao tien trinh initialize auction combo box.
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
        cbLinkedAuction.setPromptText(UiText.text("No linked auction"));
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac register preview listeners.
    private void registerPreviewListeners() {
        txtAnnouncementTitle.textProperty().addListener((observable, oldValue, newValue) -> updatePreview());
        txtAnnouncementSchedule.textProperty().addListener((observable, oldValue, newValue) -> updatePreview());
        txtAnnouncementSummary.textProperty().addListener((observable, oldValue, newValue) -> updatePreview());
        txtAnnouncementDetails.textProperty().addListener((observable, oldValue, newValue) -> updatePreview());
        cbLinkedAuction.valueProperty().addListener((observable, oldValue, newValue) -> updatePreview());
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac update metrics.
    private void updateMetrics(List<HomepageAnnouncement> announcements, List<AuctionItem> displayAuctions) {
        long linked = announcements.stream().filter(item -> item.getLinkedAuctionId() > 0).count();
        lblAnnouncementCount.setText(String.valueOf(announcements.size()));
        lblLinkedCount.setText(String.valueOf(linked));
        lblUpcomingCount.setText(String.valueOf(displayAuctions.size()));
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac load homepage snapshot.
    private HomepageSnapshot loadHomepageSnapshot() {
        List<AuctionItem> allAuctions = auctionController.getAllAuctions().stream()
                .sorted(Comparator.comparingLong(AuctionItem::getStartTime))
                .toList();
        List<HomepageAnnouncement> announcements = homepageController.getAllAnnouncements();
        List<AuctionItem> displayAuctions = allAuctions.stream()
                .filter(item -> item.getStatus() == AuctionStatus.OPEN || item.getStatus() == AuctionStatus.RUNNING)
                .toList();
        return new HomepageSnapshot(allAuctions, announcements, displayAuctions);
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac apply homepage snapshot.
    private void applyHomepageSnapshot(
            HomepageSnapshot snapshot,
            Integer selectedLinkedAuctionId,
            int selectedAnnouncementId,
            int selectedPreviewAuctionId
    ) {
        auctionLookup.clear();
        for (AuctionItem auction : snapshot.allAuctions()) {
            auctionLookup.put(auction.getId(), auction);
        }

        cbLinkedAuction.setItems(FXCollections.observableArrayList(snapshot.allAuctions()));
        restoreLinkedAuctionSelection(selectedLinkedAuctionId);

        tableAnnouncements.setItems(FXCollections.observableArrayList(snapshot.announcements()));
        reselectAnnouncement(selectedAnnouncementId);

        tableUpcomingAuctions.setItems(FXCollections.observableArrayList(snapshot.displayAuctions()));
        reselectPreviewAuction(selectedPreviewAuctionId);
        tableAnnouncements.refresh();
        tableUpcomingAuctions.refresh();

        updateMetrics(snapshot.announcements(), snapshot.displayAuctions());
        updatePreview();
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac update preview.
    private void updatePreview() {
        String title = txtAnnouncementTitle.getText() == null || txtAnnouncementTitle.getText().isBlank()
                ? UiText.text("Announcement Title")
                : txtAnnouncementTitle.getText().trim();
        String schedule = txtAnnouncementSchedule.getText() == null || txtAnnouncementSchedule.getText().isBlank()
                ? "-"
                : txtAnnouncementSchedule.getText().trim();
        String summary = txtAnnouncementSummary.getText() == null || txtAnnouncementSummary.getText().isBlank()
                ? UiText.text("A short summary will appear here.")
                : txtAnnouncementSummary.getText().trim();
        String details = txtAnnouncementDetails.getText() == null || txtAnnouncementDetails.getText().isBlank()
                ? UiText.text("Additional details and instructions will appear here.")
                : txtAnnouncementDetails.getText().trim();
        AuctionItem linkedAuction = cbLinkedAuction.getValue();

        lblPreviewTitle.setText(title);
        lblPreviewSchedule.setText(schedule);
        lblPreviewSummary.setText(summary);
        lblPreviewDetails.setText(details);
        lblPreviewLinkedAuction.setText(linkedAuction == null
                ? UiText.text("Not linked")
                : linkedAuction.getName() + " | " + AuctionViewFormatter.formatScheduleRange(linkedAuction));
    }
    // Phuong thuc: kiem tra dieu kien hoac xac thuc cho thao tac has management context.
    private boolean hasManagementContext() {
        if (homepageController == null || auctionController == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Required controllers have not been assigned to the homepage management screen.");
            return false;
        }
        if (currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Current admin information is unavailable.");
            return false;
        }
        return true;
    }
    // Phuong thuc: thuc hien chuc nang reset form trong lop AdminHomepageViewController.
    private void resetForm() {
        editingAnnouncementId = -1;
        txtAnnouncementTitle.clear();
        txtAnnouncementSchedule.clear();
        txtAnnouncementSummary.clear();
        txtAnnouncementDetails.clear();
        cbLinkedAuction.getSelectionModel().clearSelection();
        btnSaveAnnouncement.setText(UiText.text("PUBLISH TO HOMEPAGE"));
        updatePreview();
    }
    // Phuong thuc: thuc hien chuc nang resolve auction name trong lop AdminHomepageViewController.
    private String resolveAuctionName(int auctionId) {
        if (auctionId <= 0) {
            return UiText.text("Not linked");
        }

        AuctionItem auction = auctionLookup.get(auctionId);
        return auction == null ? UiText.text("Auction #") + auctionId : auction.getName();
    }
    // Phuong thuc: thuc hien chuc nang selected linked auction id trong lop AdminHomepageViewController.
    private Integer selectedLinkedAuctionId() {
        AuctionItem selected = cbLinkedAuction.getValue();
        return selected == null ? null : selected.getId();
    }
    // Phuong thuc: thuc hien chuc nang selected announcement id trong lop AdminHomepageViewController.
    private int selectedAnnouncementId() {
        HomepageAnnouncement selected = tableAnnouncements.getSelectionModel().getSelectedItem();
        return selected == null ? -1 : selected.getId();
    }
    // Phuong thuc: thuc hien chuc nang selected preview auction id trong lop AdminHomepageViewController.
    private int selectedPreviewAuctionId() {
        AuctionItem selected = tableUpcomingAuctions.getSelectionModel().getSelectedItem();
        return selected == null ? -1 : selected.getId();
    }
    // Phuong thuc: thuc hien chuc nang restore linked auction selection trong lop AdminHomepageViewController.
    private void restoreLinkedAuctionSelection(Integer auctionId) {
        if (auctionId == null || auctionId <= 0) {
            if (editingAnnouncementId < 0) {
                cbLinkedAuction.getSelectionModel().clearSelection();
            }
            return;
        }
        selectLinkedAuction(auctionId);
    }
    // Phuong thuc: thuc hien chuc nang select linked auction trong lop AdminHomepageViewController.
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
    // Phuong thuc: thuc hien chuc nang reselect announcement trong lop AdminHomepageViewController.
    private void reselectAnnouncement(int announcementId) {
        if (announcementId < 0) {
            return;
        }

        tableAnnouncements.getItems().stream()
                .filter(item -> item.getId() == announcementId)
                .findFirst()
                .ifPresent(item -> tableAnnouncements.getSelectionModel().select(item));
    }
    // Phuong thuc: thuc hien chuc nang reselect preview auction trong lop AdminHomepageViewController.
    private void reselectPreviewAuction(int auctionId) {
        if (auctionId < 0) {
            return;
        }

        tableUpcomingAuctions.getItems().stream()
                .filter(item -> item.getId() == auctionId)
                .findFirst()
                .ifPresent(item -> tableUpcomingAuctions.getSelectionModel().select(item));
    }
    // Phuong thuc: thuc hien chuc nang owner window trong lop AdminHomepageViewController.
    private javafx.stage.Window ownerWindow() {
        return frame == null ? null : frame.getWindow();
    }
    // Phuong thuc: thuc hien chuc nang switch language trong lop AdminHomepageViewController.
    private void switchLanguage(AppLanguage language) {
        if (frame == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Language settings are unavailable.");
            return;
        }
        frame.setLanguage(language);
        cbLinkedAuction.setPromptText(UiText.text("No linked auction"));
        tableAnnouncements.refresh();
        tableUpcomingAuctions.refresh();
        updatePreview();
        NotificationUtil.success(ownerWindow(), "Notification", "Language updated.");
    }
    // Phuong thuc: thuc hien chuc nang run action async trong lop AdminHomepageViewController.
    private void runActionAsync(Supplier<String> action, String successMessage, Runnable successAction) {
        actionInProgress = true;
        setActionBusy(true);
        UiAsync.run(
                action::get,
                result -> {
                    actionInProgress = false;
                    setActionBusy(false);
                    if ("SUCCESS".equals(result)) {
                        NotificationUtil.success(ownerWindow(), "Notification", successMessage);
                        successAction.run();
                        return;
                    }
                    NotificationUtil.error(ownerWindow(), "Error", result);
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
        if (txtAnnouncementTitle != null) {
            txtAnnouncementTitle.setDisable(busy);
        }
        if (txtAnnouncementSchedule != null) {
            txtAnnouncementSchedule.setDisable(busy);
        }
        if (txtAnnouncementSummary != null) {
            txtAnnouncementSummary.setDisable(busy);
        }
        if (txtAnnouncementDetails != null) {
            txtAnnouncementDetails.setDisable(busy);
        }
        if (cbLinkedAuction != null) {
            cbLinkedAuction.setDisable(busy);
        }
        if (btnSaveAnnouncement != null) {
            btnSaveAnnouncement.setDisable(busy);
        }
        if (tableAnnouncements != null) {
            tableAnnouncements.setDisable(busy);
        }
        if (tableUpcomingAuctions != null) {
            tableUpcomingAuctions.setDisable(busy);
        }
    }
    // Phuong thuc: thuc hien chuc nang homepage snapshot trong lop AdminHomepageViewController.
    private record HomepageSnapshot(
            List<AuctionItem> allAuctions,
            List<HomepageAnnouncement> announcements,
            List<AuctionItem> displayAuctions
    ) {
    }
}
