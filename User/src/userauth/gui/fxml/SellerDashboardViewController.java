package userauth.gui.fxml;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import userauth.controller.AuctionController;
import userauth.model.AuctionItem;
import userauth.model.User;

import java.util.List;

public class SellerDashboardViewController {
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
    private Spinner<Integer> spinDuration;

    @FXML
    private Button btnCreate;

    private AuthFrame frame;
    private AuctionController auctionController;
    private User currentUser;
    private int editingId = -1;

    @FXML
    private void initialize() {
        if (spinDuration.getValueFactory() == null) {
            spinDuration.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99999, 30));
        }

        colId.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        colName.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getName()));
        colCategory.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getCategory()));
        colStartPrice.setCellValueFactory(data -> new ReadOnlyStringWrapper(AuctionViewFormatter.formatMoney(data.getValue().getStartPrice())));
        colCurrentBid.setCellValueFactory(data -> new ReadOnlyStringWrapper(AuctionViewFormatter.formatMoney(data.getValue().getCurrentHighestBid())));
        colStatus.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getStatus().name()));
        colDuration.setCellValueFactory(data -> new ReadOnlyStringWrapper(AuctionViewFormatter.formatDuration(data.getValue())));
        colRemaining.setCellValueFactory(data -> new ReadOnlyStringWrapper(AuctionViewFormatter.formatRemaining(data.getValue().getEndTime())));
    }

    public void setFrame(AuthFrame frame) {
        this.frame = frame;
    }

    public void setAuctionController(AuctionController auctionController) {
        this.auctionController = auctionController;
    }

    public void setUser(User user) {
        this.currentUser = user;
        resetForm();
    }

    public void refreshData() {
        if (auctionController == null || currentUser == null) {
            return;
        }

        List<AuctionItem> myAuctions = auctionController.getAuctionsBySeller(currentUser.getId());
        tableAuctions.setItems(FXCollections.observableArrayList(myAuctions));
    }

    @FXML
    private void handleSaveAuction() {
        if (auctionController == null) {
            NotificationUtil.warning(ownerWindow(), "Thong bao", "Chua gan AuctionController cho man hinh nguoi ban.");
            return;
        }
        if (currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Thong bao", "Chua co thong tin nguoi ban hien tai.");
            return;
        }

        try {
            String name = txtName.getText().trim();
            String desc = txtDesc.getText().trim();
            String category = txtCategory.getText().trim();
            double price = Double.parseDouble(txtPrice.getText().trim());
            int durationMinutes = spinDuration.getValue();
            long start = System.currentTimeMillis();
            long end = start + (long) durationMinutes * 60 * 1000;

            String result = editingId == -1
                    ? auctionController.createAuction(name, desc, price, start, end, category, currentUser.getId())
                    : auctionController.updateAuction(editingId, currentUser.getId(), name, desc, price, start, end, category);

            if ("SUCCESS".equals(result)) {
                NotificationUtil.success(ownerWindow(), "Thong bao", "Luu phien dau gia thanh cong.");
                resetForm();
                refreshData();
                return;
            }

            NotificationUtil.error(ownerWindow(), "Loi", result);
        } catch (NumberFormatException ex) {
            NotificationUtil.error(ownerWindow(), "Loi", "Gia khoi diem khong hop le.");
        }
    }

    @FXML
    private void handleClearForm() {
        resetForm();
    }

    @FXML
    private void handleEditSelected() {
        AuctionItem item = tableAuctions.getSelectionModel().getSelectedItem();
        if (item == null) {
            NotificationUtil.warning(ownerWindow(), "Thong bao", "Hay chon mot phien dau gia.");
            return;
        }

        editingId = item.getId();
        txtName.setText(item.getName());
        txtCategory.setText(item.getCategory());
        txtPrice.setText(String.valueOf(item.getStartPrice()));
        txtDesc.setText(item.getDescription());

        long durationMs = item.getEndTime() - item.getStartTime();
        int durationMin = (int) Math.max(1, durationMs / 60000);
        spinDuration.getValueFactory().setValue(durationMin);
        btnCreate.setText("LUU CAP NHAT");
    }

    @FXML
    private void handleDeleteSelected() {
        if (auctionController == null || currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Thong bao", "Chua san sang de xoa phien dau gia.");
            return;
        }

        AuctionItem item = tableAuctions.getSelectionModel().getSelectedItem();
        if (item == null) {
            NotificationUtil.warning(ownerWindow(), "Thong bao", "Hay chon mot phien dau gia.");
            return;
        }

        boolean confirmed = NotificationUtil.confirm(ownerWindow(), "Xac nhan", "Ban co chac muon xoa hoac huy phien nay?");
        if (!confirmed) {
            return;
        }

        String result = auctionController.deleteAuction(item.getId(), currentUser.getId());
        if ("SUCCESS".equals(result)) {
            NotificationUtil.success(ownerWindow(), "Thong bao", "Da xoa hoac huy phien thanh cong.");
            refreshData();
            return;
        }

        NotificationUtil.error(ownerWindow(), "Loi", result);
    }

    @FXML
    private void handleCloseAuction() {
        if (auctionController == null || currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Thong bao", "Chua san sang de ket thuc phien dau gia.");
            return;
        }

        AuctionItem item = tableAuctions.getSelectionModel().getSelectedItem();
        if (item == null) {
            NotificationUtil.warning(ownerWindow(), "Thong bao", "Hay chon mot phien dau gia.");
            return;
        }

        String result = auctionController.closeAuction(item.getId(), currentUser.getId());
        if ("SUCCESS".equals(result)) {
            NotificationUtil.success(ownerWindow(), "Thong bao", "Da ket thuc phien dau gia.");
            refreshData();
            return;
        }

        NotificationUtil.error(ownerWindow(), "Loi", result);
    }

    @FXML
    private void handleLogout() {
        currentUser = null;
        if (frame != null) {
            frame.showLogin();
        } else {
            NotificationUtil.info(ownerWindow(), "Thong bao", "Da gan su kien dang xuat. Hay noi controller voi AuthFrame khi tich hop.");
        }
    }

    private void resetForm() {
        editingId = -1;
        txtName.clear();
        txtDesc.clear();
        txtPrice.clear();
        txtCategory.clear();
        if (spinDuration.getValueFactory() != null) {
            spinDuration.getValueFactory().setValue(30);
        }
        btnCreate.setText("TAO MOI");
    }

    private javafx.stage.Window ownerWindow() {
        return frame == null ? null : frame.getWindow();
    }
}
