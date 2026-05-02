package userauth.gui.fxml;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import userauth.controller.AuctionController;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;
import userauth.model.User;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class SellerDashboardViewController {
    private static final long ENDING_SOON_THRESHOLD_MS = 5 * 60 * 1000;

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
    private Spinner<Integer> spinDuration;

    @FXML
    private Button btnCreate;

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
    private Label lblPreviewMode;

    private AuthFrame frame;
    private AuctionController auctionController;
    private User currentUser;
    private int editingId = -1;
    private Timeline refreshTimeline;
    private long refreshTicket;
    private boolean actionInProgress;

    @FXML
    private void initialize() {
        if (spinDuration.getValueFactory() == null) {
            spinDuration.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99999, 30));
        }

        AuctionImageUtil.installRoundedClip(imgPreviewImage, 32, 32);

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
            int durationMinutes = spinDuration.getValue();
            long start = System.currentTimeMillis();
            long end = start + (long) durationMinutes * 60 * 1000;
            int sellerId = currentUser.getId();
            int currentEditingId = editingId;

            runActionAsync(
                    currentEditingId == -1
                            ? () -> auctionController.createAuction(name, desc, price, start, end, category, imageSource, sellerId)
                            : () -> auctionController.updateAuction(currentEditingId, sellerId, name, desc, price, start, end, category, imageSource),
                    "Auction saved successfully.",
                    () -> {
                        resetForm();
                        refreshData();
                    }
            );
        } catch (NumberFormatException ex) {
            NotificationUtil.error(ownerWindow(), "Error", "Invalid starting price.");
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

        long durationMs = item.getEndTime() - item.getStartTime();
        int durationMin = (int) Math.max(1, durationMs / 60000);
        spinDuration.getValueFactory().setValue(durationMin);
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
        spinDuration.valueProperty().addListener((observable, oldValue, newValue) -> updatePreview());
    }

    private void updatePreview() {
        String name = txtName.getText() == null || txtName.getText().isBlank() ? UiText.text("Product Name") : txtName.getText().trim();
        String description = txtDesc.getText() == null || txtDesc.getText().isBlank()
                ? UiText.text("The description updates instantly as the seller types.")
                : txtDesc.getText().trim();
        String category = txtCategory.getText() == null || txtCategory.getText().isBlank() ? UiText.text("Category") : txtCategory.getText().trim();
        String price = parsePricePreview();
        Integer duration = spinDuration.getValue();

        AuctionImageUtil.applyAuctionImage(imgPreviewImage, lblPreviewInitial, txtImageSource.getText(), name);
        lblPreviewName.setText(name);
        lblPreviewDescription.setText(description);
        lblPreviewCategory.setText(category);
        lblPreviewDuration.setText((duration == null ? 30 : duration) + " " + UiText.text("minutes"));
        lblPreviewPrice.setText(price);
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
        if (spinDuration.getValueFactory() != null) {
            spinDuration.getValueFactory().setValue(30);
        }
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
        if (spinDuration != null) {
            spinDuration.setDisable(busy);
        }
        if (btnCreate != null) {
            btnCreate.setDisable(busy);
        }
    }
}
