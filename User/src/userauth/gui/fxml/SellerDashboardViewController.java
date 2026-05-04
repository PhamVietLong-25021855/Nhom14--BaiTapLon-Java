package userauth.gui.fxml;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import userauth.controller.AuctionController;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;
import userauth.model.User;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

public class SellerDashboardViewController {
    private static final long ENDING_SOON_THRESHOLD_MS = 5 * 60 * 1000;
    private static final DateTimeFormatter TIME_INPUT_FORMAT = DateTimeFormatter.ofPattern("H:mm");

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
    private TableColumn<AuctionItem, String> colCurrentBid;

    @FXML
    private TableColumn<AuctionItem, String> colStatus;

    @FXML
    private TableColumn<AuctionItem, String> colDuration;

    @FXML
    private TableColumn<AuctionItem, String> colRemaining;

    @FXML
    private TextField txtName;

    @FXML
    private TextArea txtDesc;

    @FXML
    private TextField txtPrice;

    @FXML
    private TextField txtCategory;

    @FXML
    private TextField txtImageSource;

    @FXML
    private DatePicker dpStartDate;

    @FXML
    private TextField txtStartTime;

    @FXML
    private DatePicker dpEndDate;

    @FXML
    private TextField txtEndTime;

    @FXML
    private Button btnCreate;

    @FXML
    private TextField txtExtendMinutes;

    @FXML
    private Button btnExtendTime;

    @FXML
    private Label lblSellerSidebar;

    @FXML
    private Label lblSellerName;

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
    private Label lblPreviewDuration;

    @FXML
    private Label lblPreviewPrice;

    @FXML
    private Label lblPreviewExtension;

    @FXML
    private Label lblPreviewMode;

    @FXML
    private ScrollPane mainScrollPane ;

    private AuthFrame frame;
    private AuctionController auctionController;
    private User currentUser;
    private int editingId = -1;
    private Timeline refreshTimeline;
    private long refreshTicket;
    private boolean actionInProgress;

    @FXML
    private void initialize() {
        AuctionImageUtil.installRoundedClip(imgPreviewImage, 32, 32);
        installTimeInputMask(txtStartTime);
        installTimeInputMask(txtEndTime);
        applyDefaultSchedule();

        colId.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        colName.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getName()));
        colCategory.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getCategory()));
        colStartPrice.setCellValueFactory(data -> new ReadOnlyStringWrapper(AuctionViewFormatter.formatMoney(data.getValue().getStartPrice())));
        colCurrentBid.setCellValueFactory(data -> new ReadOnlyStringWrapper(AuctionViewFormatter.formatMoney(data.getValue().getCurrentHighestBid())));
        colStatus.setCellValueFactory(data -> new ReadOnlyStringWrapper(UiText.auctionStatus(data.getValue().getStatus())));
        colDuration.setCellValueFactory(data -> new ReadOnlyStringWrapper(AuctionViewFormatter.formatDuration(data.getValue())));
        colRemaining.setCellValueFactory(data -> new ReadOnlyStringWrapper(AuctionViewFormatter.formatRemaining(data.getValue().getEndTime())));

        tableAuctions.setRowFactory(this::createAuctionRow);
        registerPreviewListeners();
        updatePreview();

        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> refreshData()));
        refreshTimeline.setCycleCount(Animation.INDEFINITE);
    }

    public void setFrame(AuthFrame frame) {
        this.frame = frame;
    }

    public void setAuctionController(AuctionController auctionController) {
        this.auctionController = auctionController;
    }

    public void setUser(User user) {
        this.currentUser = user;
        String displayName = user == null ? UiText.text("Seller") : abbreviate(resolveDisplayName(user), 26);
        String sidebarName = user == null
                ? "@" + UiText.text("Seller")
                : "@" + abbreviate(safeText(user.getUsername(), UiText.text("Seller")), 18);
        lblSellerName.setText(displayName);
        lblSellerSidebar.setText(sidebarName);
        resetForm();
    }

    public void activate() {
        refreshData();
        if (refreshTimeline != null && refreshTimeline.getStatus() != Animation.Status.RUNNING) {
            refreshTimeline.play();
        }
    }

    public void deactivate() {
        refreshTicket++;
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
                () -> auctionController.getAuctionsBySeller(sellerId).stream()
                        .filter(item -> item.getStatus() != AuctionStatus.CANCELED)
                        .collect(Collectors.toList()),
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
                }
        );
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
            String name = txtName.getText().trim();
            String desc = txtDesc.getText().trim();
            String category = txtCategory.getText().trim();
            String imageSource = normalizeOptionalText(txtImageSource.getText());
            double price = Double.parseDouble(txtPrice.getText().trim());
            long start = parseScheduleValue(dpStartDate, txtStartTime, "Start time");
            long end = parseScheduleValue(dpEndDate, txtEndTime, "End time");
            int sellerId = currentUser.getId();
            int currentEditingId = editingId;

            runActionAsync(
                    currentEditingId == -1
                            ? () -> auctionController.createAuction(
                            name,
                            desc,
                            price,
                            start,
                            end,
                            category,
                            imageSource,
                            sellerId,
                            false,
                            AuctionItem.DEFAULT_EXTENSION_THRESHOLD_SECONDS,
                            AuctionItem.DEFAULT_EXTENSION_DURATION_SECONDS,
                            AuctionItem.DEFAULT_MAX_EXTENSION_COUNT
                    )
                            : () -> auctionController.updateAuction(
                            currentEditingId,
                            sellerId,
                            name,
                            desc,
                            price,
                            start,
                            end,
                            category,
                            imageSource,
                            false,
                            AuctionItem.DEFAULT_EXTENSION_THRESHOLD_SECONDS,
                            AuctionItem.DEFAULT_EXTENSION_DURATION_SECONDS,
                            AuctionItem.DEFAULT_MAX_EXTENSION_COUNT
                    ),
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

        txtImageSource.setText(selectedFile.getAbsolutePath());
        updatePreview();
    }

    @FXML
    private void handleEditSelected() {
        AuctionItem item = tableAuctions.getSelectionModel().getSelectedItem();
        if (item == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Please select an auction.");
            return;
        }

        editingId = item.getId();
        txtName.setText(item.getName());
        txtCategory.setText(item.getCategory());
        txtPrice.setText(String.valueOf(item.getStartPrice()));
        txtDesc.setText(item.getDescription());
        txtImageSource.setText(item.getImageSource());
        populateScheduleFields(item.getStartTime(), item.getEndTime());
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
    private void handleExtendSelectedAuction() {
        if (auctionController == null || currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Notification", "Auction time adjustment is not ready.");
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

        try {
            int additionalMinutes = parsePositiveInteger(
                    txtExtendMinutes,
                    "Additional minutes must be a positive integer."
            );
            int auctionId = item.getId();
            int sellerId = currentUser.getId();

            runActionAsync(
                    () -> auctionController.extendAuctionTime(auctionId, sellerId, additionalMinutes),
                    "Auction time extended successfully.",
                    () -> {
                        txtExtendMinutes.clear();
                        refreshData();
                    }
            );
        } catch (IllegalArgumentException ex) {
            NotificationUtil.error(ownerWindow(), "Error", ex.getMessage());
        }
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
        dpStartDate.valueProperty().addListener((observable, oldValue, newValue) -> updatePreview());
        txtStartTime.textProperty().addListener((observable, oldValue, newValue) -> updatePreview());
        dpEndDate.valueProperty().addListener((observable, oldValue, newValue) -> updatePreview());
        txtEndTime.textProperty().addListener((observable, oldValue, newValue) -> updatePreview());
    }

    private void updatePreview() {
        String name = txtName.getText() == null || txtName.getText().isBlank() ? UiText.text("Product Name") : txtName.getText().trim();
        String description = txtDesc.getText() == null || txtDesc.getText().isBlank()
                ? UiText.text("The description updates instantly as the seller types.")
                : txtDesc.getText().trim();
        String category = txtCategory.getText() == null || txtCategory.getText().isBlank() ? UiText.text("Category") : txtCategory.getText().trim();
        String price = parsePricePreview();

        AuctionImageUtil.applyAuctionImage(imgPreviewImage, lblPreviewInitial, txtImageSource.getText(), name);
        lblPreviewName.setText(name);
        lblPreviewDescription.setText(description);
        lblPreviewCategory.setText(category);
        lblPreviewDuration.setText(parseSchedulePreview());
        lblPreviewPrice.setText(price);
        lblPreviewExtension.setText(UiText.text("Adjust later in My Actions."));
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
            return AuctionViewFormatter.formatMoney(Double.parseDouble(value));
        } catch (NumberFormatException ex) {
            return UiText.text("Invalid price");
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
        txtName.clear();
        txtDesc.clear();
        txtPrice.clear();
        txtCategory.clear();
        txtImageSource.clear();
        applyDefaultSchedule();
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
        if (txtCategory != null) {
            txtCategory.setDisable(busy);
        }
        if (txtImageSource != null) {
            txtImageSource.setDisable(busy);
        }
        if (dpStartDate != null) {
            dpStartDate.setDisable(busy);
        }
        if (txtStartTime != null) {
            txtStartTime.setDisable(busy);
        }
        if (dpEndDate != null) {
            dpEndDate.setDisable(busy);
        }
        if (txtEndTime != null) {
            txtEndTime.setDisable(busy);
        }
        if (txtExtendMinutes != null) {
            txtExtendMinutes.setDisable(busy);
        }
        if (btnCreate != null) {
            btnCreate.setDisable(busy);
        }
        if (btnExtendTime != null) {
            btnExtendTime.setDisable(busy);
        }
    }

    private void applyDefaultSchedule() {
        LocalDateTime start = LocalDateTime.now().plusMinutes(10).withSecond(0).withNano(0);
        int remainder = start.getMinute() % 5;
        if (remainder != 0) {
            start = start.plusMinutes(5 - remainder);
        }
        LocalDateTime end = start.plusMinutes(30);
        populateScheduleFields(start, end);
    }

    private void populateScheduleFields(long startTime, long endTime) {
        populateScheduleFields(
                LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.systemDefault()),
                LocalDateTime.ofInstant(Instant.ofEpochMilli(endTime), ZoneId.systemDefault())
        );
    }

    private void populateScheduleFields(LocalDateTime start, LocalDateTime end) {
        dpStartDate.setValue(start.toLocalDate());
        txtStartTime.setText(start.format(TIME_INPUT_FORMAT));
        dpEndDate.setValue(end.toLocalDate());
        txtEndTime.setText(end.format(TIME_INPUT_FORMAT));
    }

    private long parseScheduleValue(DatePicker datePicker, TextField timeField, String fieldLabel) {
        LocalDate date = datePicker.getValue();
        if (date == null) {
            throw new IllegalArgumentException(fieldLabel + " date is required.");
        }

        String rawTime = timeField.getText() == null ? "" : timeField.getText().trim();
        if (rawTime.isBlank()) {
            throw new IllegalArgumentException(fieldLabel + " time is required.");
        }

        try {
            String formattedTime = formatTimeInput(rawTime);
            if (!formattedTime.matches("\\d{1,2}:\\d{2}")) {
                throw new DateTimeParseException("Incomplete time input.", formattedTime, 0);
            }
            LocalTime time = LocalTime.parse(formattedTime, TIME_INPUT_FORMAT);
            return date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(fieldLabel + " must use HH:mm format.");
        }
    }

    private String parseSchedulePreview() {
        try {
            long start = parseScheduleValue(dpStartDate, txtStartTime, "Start time");
            long end = parseScheduleValue(dpEndDate, txtEndTime, "End time");
            long durationMinutes = Math.max(1, (end - start) / 60_000L);
            return durationMinutes + " " + UiText.text("minutes");
        } catch (IllegalArgumentException ex) {
            return UiText.text("Invalid schedule");
        }
    }

    private int parsePositiveInteger(TextField field, String errorMessage) {
        String rawValue = field.getText() == null ? "" : field.getText().trim();
        if (rawValue.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }

        int value;
        try {
            value = Integer.parseInt(rawValue);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(errorMessage);
        }

        if (value <= 0) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value;
    }

    private void installTimeInputMask(TextField field) {
        if (field == null) {
            return;
        }
        field.textProperty().addListener((observable, oldValue, newValue) -> {
            String formatted = formatTimeInput(newValue);
            if (!formatted.equals(newValue)) {
                field.setText(formatted);
            }
        });
    }

    private String formatTimeInput(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String digitsOnly = value.replaceAll("\\D", "");
        if (digitsOnly.length() > 4) {
            digitsOnly = digitsOnly.substring(0, 4);
        }

        if (digitsOnly.length() <= 2) {
            return digitsOnly;
        }
        if (digitsOnly.length() == 3) {
            int firstTwoDigits = Integer.parseInt(digitsOnly.substring(0, 2));
            if (firstTwoDigits <= 23) {
                return digitsOnly.substring(0, 2) + ":" + digitsOnly.substring(2);
            }
            return digitsOnly.substring(0, 1) + ":" + digitsOnly.substring(1);
        }
        return digitsOnly.substring(0, 2) + ":" + digitsOnly.substring(2);
    }

    @FXML
    private void handleScrollToMyAuctions() {
        if (mainScrollPane != null) {
            mainScrollPane.setVvalue(1.0);
        }
    }
}
