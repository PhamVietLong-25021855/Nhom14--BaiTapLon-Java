package userauth.gui.fxml.seller;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import userauth.common.AuctionRules;
import userauth.controller.AuctionController;
import userauth.controller.AuthController;
import userauth.controller.NotificationController;
import userauth.event.AuctionEvent;
import userauth.event.AuctionEventBus;
import userauth.event.AuctionEventListener;
import userauth.gui.fxml.shared.*;
import userauth.gui.fxml.shell.AuthFrame;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;
import userauth.model.User;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class SellerDashboardViewController {
    private static final long ENDING_SOON_THRESHOLD_MS = 5 * 60 * 1000;
    private static final ZoneId DISPLAY_ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter TIME_INPUT_FORMATTER = DateTimeFormatter.ofPattern("H:mm");
    private static final DateTimeFormatter TIME_DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private TableView<AuctionItem> tableAuctions;

    @FXML
    private TableColumn<AuctionItem, Integer> colId;

    @FXML
    private TableColumn<AuctionItem, String> colName;

    @FXML
    private TableColumn<AuctionItem, String> colCategory;

    @FXML
    private TableColumn<AuctionItem, String> colStartPrice;

    @FXML
    private TableColumn<AuctionItem, String> colBidStep;

    @FXML
    private TableColumn<AuctionItem, String> colCurrentBid;

    @FXML
    private TableColumn<AuctionItem, String> colStatus;

    @FXML
    private TableColumn<AuctionItem, String> colStartTime;

    @FXML
    private TableColumn<AuctionItem, String> colEndTime;

    @FXML
    private TableColumn<AuctionItem, String> colRemaining;

    @FXML
    private TextField txtName;

    @FXML
    private TextArea txtDesc;

    @FXML
    private TextField txtPrice;

    @FXML
    private TextField txtBidStep;

    @FXML
    private TextField txtCategory;

    @FXML
    private TextField txtImageSource;

    @FXML
    private DatePicker dateStart;

    @FXML
    private TextField txtStartTime;

    @FXML
    private DatePicker dateEnd;

    @FXML
    private TextField txtEndTime;

    @FXML
    private Button btnCreate;

    @FXML
    private Label lblSellerSidebar;

    @FXML
    private Label lblSellerName;

    @FXML
    private Label lblSellerMeta;

    @FXML
    private Label lblTotalAuctions;

    @FXML
    private Label lblRunningAuctions;

    @FXML
    private Label lblScheduledAuctions;

    @FXML
    private Label lblClosedAuctions;

    @FXML
    private Label lblPreviewInitial;

    @FXML
    private ImageView imgPreviewImage;

    @FXML
    private Label lblPreviewName;

    @FXML
    private Label lblPreviewDescription;

    @FXML
    private Label lblPreviewCategory;

    @FXML
    private Label lblPreviewStartTime;

    @FXML
    private Label lblPreviewEndTime;

    @FXML
    private Label lblPreviewPrice;

    @FXML
    private Label lblPreviewBidStep;

    @FXML
    private Label lblPreviewMode;

    private AuthFrame frame;
    private AuthController authController;
    private AuctionController auctionController;
    private NotificationController notificationController;
    private User currentUser;
    private int editingId = -1;
    private Timeline refreshTimeline;
    private long refreshTicket;
    private boolean actionInProgress;
    private byte[] workingImageData;
    private String workingImageSource;
    private String workingImagePreviewSource;
    private final AuctionEventListener auctionEventListener = event -> Platform.runLater(() -> handleAuctionEvent(event));
    private boolean observerRegistered;

    @FXML
    private void initialize() {
        UiInput.installMoneyInput(txtPrice);
        UiInput.installMoneyInput(txtBidStep);
        installTimeInput(txtStartTime);
        installTimeInput(txtEndTime);
        setDefaultSchedule();

        AuctionImageUtil.installRoundedClip(imgPreviewImage, 32, 32);

        colId.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        colName.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getName()));
        colCategory.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getCategory()));
        colStartPrice.setCellValueFactory(data -> new ReadOnlyStringWrapper(AuctionViewFormatter.formatMoney(data.getValue().getStartPrice())));
        colBidStep.setCellValueFactory(data -> new ReadOnlyStringWrapper(AuctionViewFormatter.formatBidStep(data.getValue())));
        colCurrentBid.setCellValueFactory(data -> new ReadOnlyStringWrapper(AuctionViewFormatter.formatMoney(data.getValue().getCurrentHighestBid())));
        colStatus.setCellValueFactory(data -> new ReadOnlyStringWrapper(UiText.auctionStatus(data.getValue().getStatus())));
        colStartTime.setCellValueFactory(data -> new ReadOnlyStringWrapper(AuctionViewFormatter.formatDateTime(data.getValue().getStartTime())));
        colEndTime.setCellValueFactory(data -> new ReadOnlyStringWrapper(AuctionViewFormatter.formatDateTime(data.getValue().getEndTime())));
        colRemaining.setCellValueFactory(data -> new ReadOnlyStringWrapper(AuctionViewFormatter.formatRemaining(data.getValue().getEndTime())));

        tableAuctions.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tableAuctions.setFixedCellSize(44);
        tableAuctions.setRowFactory(this::createAuctionRow);
        registerPreviewListeners();
        updatePreview();

        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> refreshData()));
        refreshTimeline.setCycleCount(Animation.INDEFINITE);
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

    public void setNotificationController(NotificationController notificationController) {
        this.notificationController = notificationController;
    }

    public void setUser(User user) {
        this.currentUser = user;
        String displayName = user == null ? UiText.text("Seller") : abbreviate(resolveDisplayName(user), 26);
        String userMeta = formatUserMeta(user, "SELLER");
        String sidebarName = user == null
                ? "@" + UiText.text("Seller")
                : "@" + abbreviate(safeText(user.getUsername(), UiText.text("Seller")), 18) + "\n" + userMeta;
        lblSellerName.setText(displayName);
        lblSellerMeta.setText(userMeta);
        lblSellerSidebar.setText(sidebarName);
        resetForm();
    }

    public void activate() {
        registerAuctionObserver();
        refreshData();
        if (refreshTimeline != null && refreshTimeline.getStatus() != Animation.Status.RUNNING) {
            refreshTimeline.play();
        }
    }

    public void deactivate() {
        refreshTicket++;
        unregisterAuctionObserver();
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
    }

    public void refreshData() {
        if (auctionController == null || currentUser == null) {
            return;
        }

        long ticket = ++refreshTicket;
        int selectedId = selectedAuctionId();
        int sellerId = currentUser.getId();

        UiAsync.run(
                () -> {
                    ensureCurrentAccountActive();
                    return auctionController.getAuctionsBySeller(sellerId).stream()
                            .filter(item -> item.getStatus() != AuctionStatus.CANCELED)
                            .collect(Collectors.toList());
                },
                myAuctions -> {
                    if (ticket != refreshTicket) {
                        return;
                    }
                    tableAuctions.setItems(FXCollections.observableArrayList(myAuctions));
                    reselectAuction(selectedId);
                    updateMetrics(myAuctions);
                    tableAuctions.refresh();
                },
                error -> {
                    handleAccountLockError(error);
                }
        );
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

    @FXML
    private void handleSaveAuction() {
        if (auctionController == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "AuctionController has not been assigned to the seller screen.");
            return;
        }
        if (currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Current seller information is unavailable.");
            return;
        }
        if (actionInProgress) {
            return;
        }

        try {
            String rawImageInput = normalizeOptionalText(txtImageSource.getText());
            if (rawImageInput == null) {
                clearWorkingImage();
            } else {
                syncImageFromInput(true);
            }

            String name = textOf(txtName);
            String desc = textOf(txtDesc);
            String category = textOf(txtCategory);
            String imageSource = resolveImageSourceForSave(rawImageInput);
            byte[] imageData = workingImageData;
            double price = UiInput.parsePositiveDecimal(txtPrice.getText(), "Starting price");
            double bidStep = UiInput.parsePositiveDecimal(txtBidStep.getText(), UiText.text("Bid step"));
            long start = readScheduleTimestamp(dateStart, txtStartTime, "Start date and time");
            long end = readScheduleTimestamp(dateEnd, txtEndTime, "End date and time");
            validateSchedule(start, end);
            int sellerId = currentUser.getId();
            int currentEditingId = editingId;

            runActionAsync(
                    currentEditingId == -1
                            ? () -> auctionController.createAuction(name, desc, price, start, end, category, imageSource, imageData, bidStep, sellerId)
                            : () -> auctionController.updateAuction(currentEditingId, sellerId, name, desc, price, start, end, category, imageSource, imageData, bidStep),
                    "Auction saved successfully.",
                    () -> {
                        resetForm();
                        refreshData();
                    }
            );
        } catch (NumberFormatException ex) {
            NotificationUtil.error(ownerWindow(), "Error", "Invalid starting price.");
        } catch (IllegalArgumentException ex) {
            NotificationUtil.error(ownerWindow(), "Error", ex.getMessage());
        }
    }

    @FXML
    private void handleClearForm() {
        resetForm();
    }

    @FXML
    private void handleChooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(UiText.text("Choose product image"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(UiText.text("Image Files"), "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

        File selectedFile = chooser.showOpenDialog(ownerWindow());
        if (selectedFile == null) {
            return;
        }

        try {
            loadSelectedImage(selectedFile.toPath());
            txtImageSource.setText(selectedFile.getAbsolutePath());
            updatePreview();
        } catch (IOException ex) {
            NotificationUtil.error(ownerWindow(), "Error", "Unable to read the selected image file.");
        }
    }

    @FXML
    private void handleEditSelected() {
        AuctionItem item = tableAuctions.getSelectionModel().getSelectedItem();
        if (item == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Please select an auction.");
            return;
        }

        editingId = item.getId();
        workingImageData = item.getImageData();
        workingImageSource = item.getImageSource();
        workingImagePreviewSource = item.getImageSource();
        txtName.setText(item.getName());
        txtCategory.setText(item.getCategory());
        txtPrice.setText(String.valueOf(item.getStartPrice()));
        txtBidStep.setText(AuctionViewFormatter.formatMoney(item.getBidStep()));
        txtDesc.setText(item.getDescription());
        txtImageSource.setText(item.getImageSource() == null ? "" : item.getImageSource());

        setScheduleControls(item.getStartTime(), item.getEndTime());
        btnCreate.setText(UiText.text("SAVE CHANGES"));
        updatePreview();
    }

    @FXML
    private void handleDeleteSelected() {
        if (auctionController == null || currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Auction deletion is not ready.");
            return;
        }
        if (actionInProgress) {
            return;
        }

        AuctionItem item = tableAuctions.getSelectionModel().getSelectedItem();
        if (item == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Please select an auction.");
            return;
        }

        boolean confirmed = NotificationUtil.confirm(ownerWindow(), "Confirm", "Are you sure you want to delete or cancel this auction?");
        if (!confirmed) {
            return;
        }

        int auctionId = item.getId();
        int sellerId = currentUser.getId();
        runActionAsync(
                () -> auctionController.deleteAuction(auctionId, sellerId),
                "Auction deleted or cancelled successfully.",
                this::refreshData
        );
    }

    @FXML
    private void handleCloseAuction() {
        if (auctionController == null || currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Auction closing is not ready.");
            return;
        }
        if (actionInProgress) {
            return;
        }

        AuctionItem item = tableAuctions.getSelectionModel().getSelectedItem();
        if (item == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Please select an auction.");
            return;
        }

        int auctionId = item.getId();
        int sellerId = currentUser.getId();
        runActionAsync(
                () -> auctionController.closeAuction(auctionId, sellerId),
                "Auction closed successfully.",
                this::refreshData
        );
    }

    @FXML
    private void handleMarkAuctionPaid() {
        if (auctionController == null || currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Auction settlement is not ready.");
            return;
        }
        if (actionInProgress) {
            return;
        }

        AuctionItem item = tableAuctions.getSelectionModel().getSelectedItem();
        if (item == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Please select an auction.");
            return;
        }

        runActionAsync(
                () -> auctionController.markAuctionAsPaid(item.getId(), currentUser.getId()),
                "Auction marked as paid.",
                this::refreshData
        );
    }

    @FXML
    private void handleCancelFinishedAuction() {
        if (auctionController == null || currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Auction settlement is not ready.");
            return;
        }
        if (actionInProgress) {
            return;
        }

        AuctionItem item = tableAuctions.getSelectionModel().getSelectedItem();
        if (item == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Please select an auction.");
            return;
        }

        runActionAsync(
                () -> auctionController.cancelFinishedAuction(item.getId(), currentUser.getId()),
                "Auction result cancelled.",
                this::refreshData
        );
    }

    @FXML
    private void handleSwitchToEnglish() {
        switchLanguage(AppLanguage.ENGLISH);
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
        frame.showInboxDialog(currentUser, notificationController.findUserNotification(currentUser.getId()));
    }

    private void updateMetrics(List<AuctionItem> myAuctions) {
        long running = myAuctions.stream().filter(item -> item.getStatus() == AuctionStatus.RUNNING).count();
        long open = myAuctions.stream().filter(item -> item.getStatus() == AuctionStatus.OPEN).count();
        long closed = myAuctions.stream()
                .filter(item -> item.getStatus() == AuctionStatus.FINISHED
                        || item.getStatus() == AuctionStatus.CANCELED
                        || item.getStatus() == AuctionStatus.PAID)
                .count();

        lblTotalAuctions.setText(String.valueOf(myAuctions.size()));
        lblRunningAuctions.setText(String.valueOf(running));
        lblScheduledAuctions.setText(String.valueOf(open));
        lblClosedAuctions.setText(String.valueOf(closed));
    }

    private void registerPreviewListeners() {
        txtName.textProperty().addListener((observable, oldValue, newValue) -> updatePreview());
        txtDesc.textProperty().addListener((observable, oldValue, newValue) -> updatePreview());
        txtCategory.textProperty().addListener((observable, oldValue, newValue) -> updatePreview());
        txtImageSource.textProperty().addListener((observable, oldValue, newValue) -> updatePreview());
        txtPrice.textProperty().addListener((observable, oldValue, newValue) -> updatePreview());
        txtBidStep.textProperty().addListener((observable, oldValue, newValue) -> updatePreview());
        dateStart.valueProperty().addListener((observable, oldValue, newValue) -> updatePreview());
        txtStartTime.textProperty().addListener((observable, oldValue, newValue) -> updatePreview());
        dateEnd.valueProperty().addListener((observable, oldValue, newValue) -> updatePreview());
        txtEndTime.textProperty().addListener((observable, oldValue, newValue) -> updatePreview());
    }

    private void updatePreview() {
        if (normalizeOptionalText(txtImageSource.getText()) == null) {
            clearWorkingImage();
        } else {
            syncImageFromInput(false);
        }

        updateBidStepPrompt();

        String name = txtName.getText() == null || txtName.getText().isBlank() ? UiText.text("Product Name") : txtName.getText().trim();
        String description = txtDesc.getText() == null || txtDesc.getText().isBlank()
                ? UiText.text("The description updates instantly as the seller types.")
                : txtDesc.getText().trim();
        String category = txtCategory.getText() == null || txtCategory.getText().isBlank() ? UiText.text("Category") : txtCategory.getText().trim();
        String price = parsePricePreview();
        String bidStep = parseBidStepPreview();

        AuctionImageUtil.applyAuctionImage(imgPreviewImage, lblPreviewInitial, workingImageData, workingImagePreviewSource, name);
        lblPreviewName.setText(name);
        lblPreviewDescription.setText(description);
        lblPreviewCategory.setText(category);
        updateSchedulePreview();
        lblPreviewPrice.setText(price);
        lblPreviewBidStep.setText(bidStep);
        lblPreviewMode.setText(editingId == -1
                ? UiText.text("Creating a new auction")
                : UiText.text("Editing auction") + " #" + editingId);
    }

    private String parsePricePreview() {
        try {
            String value = txtPrice.getText() == null ? "" : txtPrice.getText().trim();
            if (value.isBlank()) {
                return "0";
            }
            return AuctionViewFormatter.formatMoney(UiInput.parseDecimal(value));
        } catch (NumberFormatException ex) {
            return UiText.text("Invalid price");
        }
    }

    private String parseBidStepPreview() {
        try {
            String priceValue = txtPrice.getText() == null ? "" : txtPrice.getText().trim();
            String bidStepValue = txtBidStep.getText() == null ? "" : txtBidStep.getText().trim();
            if (priceValue.isBlank() || bidStepValue.isBlank()) {
                return "0 (0%)";
            }
            double price = UiInput.parseDecimal(priceValue);
            double bidStep = UiInput.parseDecimal(bidStepValue);
            if (price <= 0 || bidStep < 0) {
                return UiText.text("Invalid bid step");
            }
            AuctionItem preview = new AuctionItem(0, "", "", price, System.currentTimeMillis(), System.currentTimeMillis() + 1, "", null, null, bidStep, 0);
            return AuctionViewFormatter.formatBidStep(preview);
        } catch (NumberFormatException ex) {
            return UiText.text("Invalid bid step");
        }
    }

    private void updateBidStepPrompt() {
        if (txtBidStep == null) {
            return;
        }

        try {
            String priceValue = txtPrice.getText() == null ? "" : txtPrice.getText().trim();
            if (priceValue.isBlank()) {
                txtBidStep.setPromptText(UiText.text("Enter starting price first"));
                return;
            }

            double startPrice = UiInput.parseDecimal(priceValue);
            if (startPrice <= 0) {
                txtBidStep.setPromptText(UiText.text("Enter starting price first"));
                return;
            }

            double minBidStep = startPrice * AuctionRules.MIN_BID_STEP_PERCENT;
            double maxBidStep = startPrice * AuctionRules.MAX_BID_STEP_PERCENT;
            txtBidStep.setPromptText(UiText.text("Valid: ")
                    + AuctionViewFormatter.formatMoney(minBidStep)
                    + " - "
                    + AuctionViewFormatter.formatMoney(maxBidStep));
        } catch (NumberFormatException ex) {
            txtBidStep.setPromptText(UiText.text("Enter valid starting price first"));
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

    private void resetForm() {
        editingId = -1;
        clearWorkingImage();
        txtName.clear();
        txtDesc.clear();
        txtPrice.clear();
        txtBidStep.clear();
        txtCategory.clear();
        txtImageSource.clear();
        setDefaultSchedule();
        btnCreate.setText(UiText.text("CREATE NEW"));
        updatePreview();
    }

    private String resolveDisplayName(User user) {
        String fullName = safeText(user.getFullName(), "");
        if (!fullName.isBlank()) {
            return fullName;
        }
        return safeText(user.getUsername(), UiText.text("Seller"));
    }

    private String formatUserMeta(User user, String fallbackRole) {
        if (user == null) {
            return "ID: - | Role: " + fallbackRole;
        }
        return "ID: " + user.getId() + " | Role: " + safeText(user.getRoleName(), fallbackRole);
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

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String textOf(javafx.scene.control.TextInputControl control) {
        if (control == null || control.getText() == null) {
            return "";
        }
        return control.getText().trim();
    }

    private void installTimeInput(TextField textField) {
        if (textField == null) {
            return;
        }

        textField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            return newText.length() <= 5 && newText.matches("[0-9:]*") ? change : null;
        }));
        textField.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
            if (!isFocused) {
                normalizeTimeInput(textField);
            }
        });
    }

    private void normalizeTimeInput(TextField textField) {
        try {
            LocalTime time = parseTime(textField, "Time");
            textField.setText(TIME_DISPLAY_FORMATTER.format(time));
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void setDefaultSchedule() {
        LocalDateTime start = LocalDateTime.now(DISPLAY_ZONE).withSecond(0).withNano(0);
        setScheduleControls(start, start.plusMinutes(30));
    }

    private void setScheduleControls(long startTimestamp, long endTimestamp) {
        setScheduleControls(
                LocalDateTime.ofInstant(Instant.ofEpochMilli(startTimestamp), DISPLAY_ZONE),
                LocalDateTime.ofInstant(Instant.ofEpochMilli(endTimestamp), DISPLAY_ZONE)
        );
    }

    private void setScheduleControls(LocalDateTime start, LocalDateTime end) {
        if (dateStart != null) {
            dateStart.setValue(start.toLocalDate());
        }
        if (txtStartTime != null) {
            txtStartTime.setText(TIME_DISPLAY_FORMATTER.format(start.toLocalTime()));
        }
        if (dateEnd != null) {
            dateEnd.setValue(end.toLocalDate());
        }
        if (txtEndTime != null) {
            txtEndTime.setText(TIME_DISPLAY_FORMATTER.format(end.toLocalTime()));
        }
    }

    private long readScheduleTimestamp(DatePicker datePicker, TextField timeField, String fieldName) {
        LocalDate date = datePicker == null ? null : datePicker.getValue();
        if (date == null) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        LocalTime time = parseTime(timeField, fieldName);
        return LocalDateTime.of(date, time).atZone(DISPLAY_ZONE).toInstant().toEpochMilli();
    }

    private LocalTime parseTime(TextField timeField, String fieldName) {
        String value = textOf(timeField);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        if (!value.matches("([01]?\\d|2[0-3]):[0-5]\\d")) {
            throw new IllegalArgumentException(fieldName + " must use HH:mm format.");
        }
        return LocalTime.parse(value, TIME_INPUT_FORMATTER);
    }

    private void validateSchedule(long start, long end) {
        if (start >= end) {
            throw new IllegalArgumentException("Start date and time must be earlier than end date and time.");
        }
        if (end <= System.currentTimeMillis()) {
            throw new IllegalArgumentException("End date and time must be in the future.");
        }
    }

    private void updateSchedulePreview() {
        try {
            long start = readScheduleTimestamp(dateStart, txtStartTime, "Start date and time");
            long end = readScheduleTimestamp(dateEnd, txtEndTime, "End date and time");
            lblPreviewStartTime.setText(AuctionViewFormatter.formatDateTime(start));
            lblPreviewEndTime.setText(start < end
                    ? AuctionViewFormatter.formatDateTime(end)
                    : UiText.text("End must be after start"));
        } catch (IllegalArgumentException ex) {
            lblPreviewStartTime.setText(UiText.text("Invalid schedule"));
            lblPreviewEndTime.setText(UiText.text("Invalid schedule"));
        }
    }

    private String resolveImageSourceForSave(String rawImageInput) {
        if (workingImageData != null && workingImageData.length > 0) {
            return workingImageSource;
        }
        return rawImageInput;
    }

    private void syncImageFromInput(boolean strict) {
        String rawInput = normalizeOptionalText(txtImageSource.getText());
        if (rawInput == null) {
            return;
        }

        Path localImagePath = resolveLocalImagePath(rawInput);
        if (localImagePath == null) {
            return;
        }

        String absolutePath = localImagePath.toAbsolutePath().toString();
        if (absolutePath.equals(workingImagePreviewSource) && workingImageData != null && workingImageData.length > 0) {
            return;
        }

        if (!Files.isRegularFile(localImagePath)) {
            boolean unchangedLegacyReference =
                    editingId != -1
                            && rawInput.equals(workingImageSource)
                            && (workingImageData == null || workingImageData.length == 0);
            if (strict && !unchangedLegacyReference) {
                throw new IllegalArgumentException("Selected image file could not be found.");
            }
            return;
        }

        try {
            loadSelectedImage(localImagePath);
        } catch (IOException ex) {
            if (strict) {
                throw new IllegalArgumentException("Unable to read the selected image file.");
            }
        }
    }

    private Path resolveLocalImagePath(String rawInput) {
        try {
            if (rawInput.startsWith("file:/")) {
                return Path.of(java.net.URI.create(rawInput));
            }
            if (rawInput.matches("^[A-Za-z]:\\\\.*") || rawInput.startsWith("\\\\") || rawInput.startsWith("/")) {
                return Path.of(rawInput);
            }
        } catch (RuntimeException ex) {
            return null;
        }
        return null;
    }

    private void loadSelectedImage(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        workingImageData = bytes;
        workingImageSource = path.getFileName() == null ? path.toString() : path.getFileName().toString();
        workingImagePreviewSource = path.toAbsolutePath().toString();
    }

    private void clearWorkingImage() {
        workingImageData = null;
        workingImageSource = null;
        workingImagePreviewSource = null;
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
        tableAuctions.refresh();
        updatePreview();
        NotificationUtil.success(ownerWindow(), "Notification", "Language updated.");
    }

    private void runActionAsync(java.util.function.Supplier<String> action,
                                String successMessage,
                                Runnable successAction) {
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
        if (tableAuctions != null) {
            tableAuctions.setDisable(busy);
        }
        if (txtName != null) {
            txtName.setDisable(busy);
        }
        if (txtDesc != null) {
            txtDesc.setDisable(busy);
        }
        if (txtPrice != null) {
            txtPrice.setDisable(busy);
        }
        if (txtBidStep != null) {
            txtBidStep.setDisable(busy);
        }
        if (txtCategory != null) {
            txtCategory.setDisable(busy);
        }
        if (txtImageSource != null) {
            txtImageSource.setDisable(busy);
        }
        if (dateStart != null) {
            dateStart.setDisable(busy);
        }
        if (txtStartTime != null) {
            txtStartTime.setDisable(busy);
        }
        if (dateEnd != null) {
            dateEnd.setDisable(busy);
        }
        if (txtEndTime != null) {
            txtEndTime.setDisable(busy);
        }
        if (btnCreate != null) {
            btnCreate.setDisable(busy);
        }
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

        refreshData();
    }
}
