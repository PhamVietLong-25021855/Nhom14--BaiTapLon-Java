package uet.auctionsystem.gui.fxml;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
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
import uet.auctionsystem.controller.AuctionController;
import uet.auctionsystem.event.AuctionEvent;
import uet.auctionsystem.event.AuctionEventBus;
import uet.auctionsystem.event.AuctionEventListener;
import uet.auctionsystem.model.AuctionItem;
import uet.auctionsystem.model.AuctionStatus;
import uet.auctionsystem.model.User;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

// Ghi chu file: File controller JavaFX; dieu khien hanh vi cua man hinh, hop thoai hoac thanh phan UI.
// Khai bao lop SellerDashboardViewController; dieu khien mot man hinh hoac thanh phan JavaFX cu the.
public class SellerDashboardViewController {
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho ms.
    private static final long ENDING_SOON_THRESHOLD_MS = 5 * 60 * 1000;

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
    // Thuoc tinh: luu trang thai hoac du lieu tam cho txt name.
    private TextField txtName;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho txt desc.
    private TextArea txtDesc;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho txt price.
    private TextField txtPrice;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho txt category.
    private TextField txtCategory;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho txt image source.
    private TextField txtImageSource;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho spin duration.
    private Spinner<Integer> spinDuration;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho btn create.
    private Button btnCreate;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl seller sidebar.
    private Label lblSellerSidebar;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl seller name.
    private Label lblSellerName;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl total auctions.
    private Label lblTotalAuctions;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl running auctions.
    private Label lblRunningAuctions;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl scheduled auctions.
    private Label lblScheduledAuctions;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl closed auctions.
    private Label lblClosedAuctions;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl preview initial.
    private Label lblPreviewInitial;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho img preview image.
    private ImageView imgPreviewImage;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl preview name.
    private Label lblPreviewName;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl preview description.
    private Label lblPreviewDescription;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl preview category.
    private Label lblPreviewCategory;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl preview duration.
    private Label lblPreviewDuration;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl preview price.
    private Label lblPreviewPrice;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl preview mode.
    private Label lblPreviewMode;
    // Thuoc tinh: giu tham chieu den AuthFrame de phoi hop xu ly.
    private AuthFrame frame;
    // Thuoc tinh: giu tham chieu den AuctionController de phoi hop xu ly.
    private AuctionController auctionController;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho current user.
    private User currentUser;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho editing id.
    private int editingId = -1;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho refresh timeline.
    private Timeline refreshTimeline;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho refresh ticket.
    private long refreshTicket;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho action in progress.
    private boolean actionInProgress;
    // Bá»™ 3 biáº¿n táº¡m Ä‘á»ƒ quáº£n lÃ½ áº£nh seller Ä‘ang thao tÃ¡c trÆ°á»›c khi lÆ°u xuá»‘ng DB.
    // Thuoc tinh: luu trang thai hoac du lieu tam cho working image data.
    private byte[] workingImageData;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho working image source.
    private String workingImageSource;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho working image preview source.
    private String workingImagePreviewSource;
    // Listener dÃ¹ng event bus Ä‘á»ƒ seller dashboard refresh ngay khi auction Ä‘á»•i tráº¡ng thÃ¡i.
    private final AuctionEventListener auctionEventListener = event -> Platform.runLater(() -> handleAuctionEvent(event));
    // Thuoc tinh: luu trang thai hoac du lieu tam cho observer registered.
    private boolean observerRegistered;

    @FXML
    // Phuong thuc: khoi dong hoac khoi tao tien trinh initialize.
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
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set frame.
    public void setFrame(AuthFrame frame) {
        this.frame = frame;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set auction controller.
    public void setAuctionController(AuctionController auctionController) {
        this.auctionController = auctionController;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set user.
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

    // Khi mÃ n hÃ¬nh seller má»Ÿ ra thÃ¬ báº­t cáº£ polling vÃ  event-based refresh.
    // Phuong thuc: thuc hien chuc nang activate trong lop SellerDashboardViewController.
    public void activate() {
        registerAuctionObserver();
        refreshData();
        if (refreshTimeline != null && refreshTimeline.getStatus() != Animation.Status.RUNNING) {
            refreshTimeline.play();
        }
    }
    // Phuong thuc: thuc hien chuc nang deactivate trong lop SellerDashboardViewController.
    public void deactivate() {
        refreshTicket++;
        unregisterAuctionObserver();
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac refresh data.
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
    // Save Ä‘i qua controller má»›i Ä‘á»ƒ cÃ³ thá»ƒ ghi kÃ¨m áº£nh binary.
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle save auction.
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

            String name = txtName.getText().trim();
            String desc = txtDesc.getText().trim();
            String category = txtCategory.getText().trim();
            String imageSource = resolveImageSourceForSave(rawImageInput);
            byte[] imageData = workingImageData;
            double price = Double.parseDouble(txtPrice.getText().trim());
            int durationMinutes = spinDuration.getValue();
            long start = System.currentTimeMillis();
            long end = start + (long) durationMinutes * 60 * 1000;
            int sellerId = currentUser.getId();
            int currentEditingId = editingId;

            runActionAsync(
                    currentEditingId == -1
                            ? () -> auctionController.createAuction(name, desc, price, start, end, category, imageSource, imageData, sellerId)
                            : () -> auctionController.updateAuction(currentEditingId, sellerId, name, desc, price, start, end, category, imageSource, imageData),
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
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle clear form.
    private void handleClearForm() {
        resetForm();
    }

    @FXML
    // Cho seller chá»n file local rá»“i náº¡p bytes vÃ o biáº¿n táº¡m.
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle choose image.
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
    // Náº¡p auction Ä‘ang chá»n lÃªn form Ä‘á»ƒ sá»­a.
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle edit selected.
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
        txtDesc.setText(item.getDescription());
        txtImageSource.setText(item.getImageSource() == null ? "" : item.getImageSource());

        long durationMs = item.getEndTime() - item.getStartTime();
        int durationMin = (int) Math.max(1, durationMs / 60000);
        spinDuration.getValueFactory().setValue(durationMin);
        btnCreate.setText(UiText.text("SAVE CHANGES"));
        updatePreview();
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle delete selected.
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
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle close auction.
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
    // Seller chá»‘t káº¿t quáº£ vÃ  xÃ¡c nháº­n Ä‘Ã£ thanh toÃ¡n.
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle mark auction paid.
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
    // Seller há»§y káº¿t quáº£ Ä‘Ã£ chá»‘t, cÃ³ thá»ƒ kÃ©o theo refund/release wallet.
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle cancel finished auction.
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
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac update metrics.
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
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac register preview listeners.
    private void registerPreviewListeners() {
        txtName.textProperty().addListener((observable, oldValue, newValue) -> updatePreview());
        txtDesc.textProperty().addListener((observable, oldValue, newValue) -> updatePreview());
        txtCategory.textProperty().addListener((observable, oldValue, newValue) -> updatePreview());
        txtImageSource.textProperty().addListener((observable, oldValue, newValue) -> updatePreview());
        txtPrice.textProperty().addListener((observable, oldValue, newValue) -> updatePreview());
        spinDuration.valueProperty().addListener((observable, oldValue, newValue) -> updatePreview());
    }

    // Preview luÃ´n Æ°u tiÃªn áº£nh bytes náº¿u cÃ³, giÃºp seller tháº¥y Ä‘Ãºng áº£nh sáº½ lÆ°u.
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac update preview.
    private void updatePreview() {
        if (normalizeOptionalText(txtImageSource.getText()) == null) {
            clearWorkingImage();
        } else {
            syncImageFromInput(false);
        }

        String name = txtName.getText() == null || txtName.getText().isBlank() ? UiText.text("Product Name") : txtName.getText().trim();
        String description = txtDesc.getText() == null || txtDesc.getText().isBlank()
                ? UiText.text("The description updates instantly as the seller types.")
                : txtDesc.getText().trim();
        String category = txtCategory.getText() == null || txtCategory.getText().isBlank() ? UiText.text("Category") : txtCategory.getText().trim();
        String price = parsePricePreview();
        Integer duration = spinDuration.getValue();

        AuctionImageUtil.applyAuctionImage(imgPreviewImage, lblPreviewInitial, workingImageData, workingImagePreviewSource, name);
        lblPreviewName.setText(name);
        lblPreviewDescription.setText(description);
        lblPreviewCategory.setText(category);
        lblPreviewDuration.setText((duration == null ? 30 : duration) + " " + UiText.text("minutes"));
        lblPreviewPrice.setText(price);
        lblPreviewMode.setText(editingId == -1
                ? UiText.text("Creating a new auction")
                : UiText.text("Editing auction") + " #" + editingId);
    }
    // Phuong thuc: bien doi du lieu cho thao tac parse price preview.
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
    // Phuong thuc: thuc hien chuc nang selected auction id trong lop SellerDashboardViewController.
    private int selectedAuctionId() {
        AuctionItem selected = tableAuctions.getSelectionModel().getSelectedItem();
        return selected == null ? -1 : selected.getId();
    }
    // Phuong thuc: thuc hien chuc nang reselect auction trong lop SellerDashboardViewController.
    private void reselectAuction(int selectedId) {
        if (selectedId < 0) {
            return;
        }

        tableAuctions.getItems().stream()
                .filter(item -> item.getId() == selectedId)
                .findFirst()
                .ifPresent(item -> tableAuctions.getSelectionModel().select(item));
    }
    // Phuong thuc: thuc hien chuc nang reset form trong lop SellerDashboardViewController.
    private void resetForm() {
        editingId = -1;
        clearWorkingImage();
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
    // Phuong thuc: thuc hien chuc nang resolve display name trong lop SellerDashboardViewController.
    private String resolveDisplayName(User user) {
        String fullName = safeText(user.getFullName(), "");
        if (!fullName.isBlank()) {
            return fullName;
        }
        return safeText(user.getUsername(), UiText.text("Seller"));
    }
    // Phuong thuc: thuc hien chuc nang safe text trong lop SellerDashboardViewController.
    private String safeText(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }
    // Phuong thuc: thuc hien chuc nang abbreviate trong lop SellerDashboardViewController.
    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
    // Phuong thuc: thuc hien chuc nang normalize optional text trong lop SellerDashboardViewController.
    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // Náº¿u Ä‘Ã£ cÃ³ bytes thÃ¬ source lÆ°u chá»‰ cÃ²n mang Ã½ nghÄ©a tÃªn/nguá»“n tham chiáº¿u.
    // Phuong thuc: thuc hien chuc nang resolve image source for save trong lop SellerDashboardViewController.
    private String resolveImageSourceForSave(String rawImageInput) {
        if (workingImageData != null && workingImageData.length > 0) {
            return workingImageSource;
        }
        return rawImageInput;
    }

    // Äá»“ng bá»™ text path/url ngÆ°á»i dÃ¹ng nháº­p vá»›i bá»™ áº£nh táº¡m Ä‘ang giá»¯ trong form.
    // Phuong thuc: thuc hien chuc nang sync image from input trong lop SellerDashboardViewController.
    private void syncImageFromInput(boolean strict) {
        String rawInput = normalizeOptionalText(txtImageSource.getText());
        if (rawInput == null) {
            return;
        }

        Path localImagePath = resolveLocalImagePath(rawInput);
        if (localImagePath == null) {
            boolean unchangedStoredImage = workingImageData != null && rawInput.equals(workingImageSource);
            if (!unchangedStoredImage) {
                workingImageData = null;
                workingImageSource = rawInput;
                workingImagePreviewSource = rawInput;
            }
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
    // Phuong thuc: thuc hien chuc nang resolve local image path trong lop SellerDashboardViewController.
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

    // Äá»c file áº£nh local thÃ nh bytes Ä‘á»ƒ lÆ°u xuá»‘ng DB vÃ  preview ngay.
    // Phuong thuc: lay hoac doc du lieu cho thao tac load selected image.
    private void loadSelectedImage(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        workingImageData = bytes;
        workingImageSource = path.getFileName() == null ? path.toString() : path.getFileName().toString();
        workingImagePreviewSource = path.toAbsolutePath().toString();
    }
    // Phuong thuc: huy, xoa, dong hoac don trang thai cho thao tac clear working image.
    private void clearWorkingImage() {
        workingImageData = null;
        workingImageSource = null;
        workingImagePreviewSource = null;
    }
    // Phuong thuc: thuc hien chuc nang owner window trong lop SellerDashboardViewController.
    private javafx.stage.Window ownerWindow() {
        return frame == null ? null : frame.getWindow();
    }
    // Phuong thuc: thuc hien chuc nang switch language trong lop SellerDashboardViewController.
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
    // Phuong thuc: thuc hien chuc nang run action async trong lop SellerDashboardViewController.
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
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set action busy.
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

    // ÄÄƒng kÃ½ event bus khi mÃ n hÃ¬nh active Ä‘á»ƒ tá»± refresh khi auction thay Ä‘á»•i.
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac register auction observer.
    private void registerAuctionObserver() {
        if (observerRegistered) {
            return;
        }
        AuctionEventBus.getInstance().subscribe(auctionEventListener);
        observerRegistered = true;
    }
    // Phuong thuc: thuc hien chuc nang unregister auction observer trong lop SellerDashboardViewController.
    private void unregisterAuctionObserver() {
        if (!observerRegistered) {
            return;
        }
        AuctionEventBus.getInstance().unsubscribe(auctionEventListener);
        observerRegistered = false;
    }
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle auction event.
    private void handleAuctionEvent(AuctionEvent event) {
        if (event == null || currentUser == null) {
            return;
        }

        refreshData();
    }
}
