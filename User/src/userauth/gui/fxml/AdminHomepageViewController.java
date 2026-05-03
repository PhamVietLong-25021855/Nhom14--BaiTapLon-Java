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
import javafx.scene.control.Label;
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
import java.util.function.Supplier;

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

    @FXML
    private Label lblCmsSidebar;

    @FXML
    private Label lblCmsAdminName;

    @FXML
    private Label lblAnnouncementCount;

    @FXML
    private Label lblLinkedCount;

    @FXML
    private Label lblUpcomingCount;

    @FXML
    private Label lblPreviewTitle;

    @FXML
    private Label lblPreviewSchedule;

    @FXML
    private Label lblPreviewSummary;

    @FXML
    private Label lblPreviewLinkedAuction;

    @FXML
    private Label lblPreviewDetails;

    private final Timeline refreshTimeline = new Timeline(
            new KeyFrame(Duration.seconds(5), event -> refreshData())
    );

    private final Map<Integer, AuctionItem> auctionLookup = new HashMap<>();

    private AuthFrame frame;
    private AuctionController auctionController;
    private HomepageController homepageController;
    private User currentUser;
    private int editingAnnouncementId = -1;
    private long refreshTicket;
    private boolean actionInProgress;

    @FXML
    private void initialize() {
        initializeAnnouncementTable();
        initializeUpcomingAuctionTable();
        initializeAuctionComboBox();
        registerPreviewListeners();
        refreshTimeline.setCycleCount(Animation.INDEFINITE);
        updatePreview();
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
        String name = user == null ? UiText.text("Admin CMS") : user.getFullName() + " (" + user.getUsername() + ")";
        lblCmsAdminName.setText(name);
        if (lblCmsSidebar != null) {
            lblCmsSidebar.setText(name);
        }
        resetForm();
    }

    public void activate() {
        refreshData();
        if (refreshTimeline.getStatus() != Animation.Status.RUNNING) {
            refreshTimeline.play();
        }
    }

    public void deactivate() {
        refreshTicket++;
        refreshTimeline.stop();
    }

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
        colUpcomingStatus.setCellValueFactory(data -> new ReadOnlyStringWrapper(UiText.auctionStatus(data.getValue().getStatus())));
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
        cbLinkedAuction.setPromptText(UiText.text("No linked auction"));
    }

    private void registerPreviewListeners() {
        txtAnnouncementTitle.textProperty().addListener((observable, oldValue, newValue) -> updatePreview());
        txtAnnouncementSchedule.textProperty().addListener((observable, oldValue, newValue) -> updatePreview());
        txtAnnouncementSummary.textProperty().addListener((observable, oldValue, newValue) -> updatePreview());
        txtAnnouncementDetails.textProperty().addListener((observable, oldValue, newValue) -> updatePreview());
        cbLinkedAuction.valueProperty().addListener((observable, oldValue, newValue) -> updatePreview());
    }

    private void updateMetrics(List<HomepageAnnouncement> announcements, List<AuctionItem> displayAuctions) {
        long linked = announcements.stream().filter(item -> item.getLinkedAuctionId() > 0).count();
        lblAnnouncementCount.setText(String.valueOf(announcements.size()));
        lblLinkedCount.setText(String.valueOf(linked));
        lblUpcomingCount.setText(String.valueOf(displayAuctions.size()));
    }

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

    private String resolveAuctionName(int auctionId) {
        if (auctionId <= 0) {
            return UiText.text("Not linked");
        }

        AuctionItem auction = auctionLookup.get(auctionId);
        return auction == null ? UiText.text("Auction #") + auctionId : auction.getName();
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

    private record HomepageSnapshot(
            List<AuctionItem> allAuctions,
            List<HomepageAnnouncement> announcements,
            List<AuctionItem> displayAuctions
    ) {
    }
}
