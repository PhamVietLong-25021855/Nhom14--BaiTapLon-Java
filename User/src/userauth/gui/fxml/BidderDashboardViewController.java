package userauth.gui.fxml;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Duration;
import userauth.controller.AuctionController;
import userauth.model.AuctionItem;
import userauth.model.BidTransaction;
import userauth.model.User;

import java.util.List;

public class BidderDashboardViewController {
    @FXML
    private TableView<AuctionItem> tableAuctions;

    @FXML
    private TableColumn<AuctionItem, Integer> colId;

    @FXML
    private TableColumn<AuctionItem, String> colName;

    @FXML
    private TableColumn<AuctionItem, String> colCategory;

    @FXML
    private TableColumn<AuctionItem, String> colHighestBid;

    @FXML
    private TableColumn<AuctionItem, String> colStatus;

    @FXML
    private TableColumn<AuctionItem, String> colTimeLeft;

    private AuthFrame frame;
    private AuctionController auctionController;
    private User currentUser;
    private Timeline timeline;

    @FXML
    private void initialize() {
        colId.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        colName.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getName()));
        colCategory.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getCategory()));
        colHighestBid.setCellValueFactory(data -> new ReadOnlyStringWrapper(AuctionViewFormatter.formatMoney(data.getValue().getCurrentHighestBid())));
        colStatus.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getStatus().name()));
        colTimeLeft.setCellValueFactory(data -> new ReadOnlyStringWrapper(AuctionViewFormatter.formatTimeLeft(data.getValue())));

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> refreshData()));
        timeline.setCycleCount(Animation.INDEFINITE);
    }

    public void setFrame(AuthFrame frame) {
        this.frame = frame;
    }

    public void setAuctionController(AuctionController auctionController) {
        this.auctionController = auctionController;
    }

    public void setUser(User user) {
        this.currentUser = user;
    }

    public void activate() {
        if (timeline != null && timeline.getStatus() != Animation.Status.RUNNING) {
            timeline.play();
        }
    }

    public void deactivate() {
        if (timeline != null) {
            timeline.stop();
        }
    }

    public void refreshData() {
        if (auctionController == null || currentUser == null) {
            return;
        }

        AuctionItem selected = tableAuctions.getSelectionModel().getSelectedItem();
        int selectedId = selected == null ? -1 : selected.getId();

        List<AuctionItem> allAuctions = auctionController.getAllAuctions();
        tableAuctions.setItems(FXCollections.observableArrayList(allAuctions));

        if (selectedId < 0) {
            return;
        }

        tableAuctions.getItems().stream()
                .filter(item -> item.getId() == selectedId)
                .findFirst()
                .ifPresent(item -> tableAuctions.getSelectionModel().select(item));
    }

    @FXML
    private void handlePlaceBid() {
        if (auctionController == null || currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Thong bao", "Chua san sang de dat gia.");
            return;
        }

        AuctionItem selected = tableAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationUtil.warning(ownerWindow(), "Thong bao", "Hay chon mot phien dau gia.");
            return;
        }

        String bidInput = NotificationUtil.input(
                ownerWindow(),
                "Dat gia",
                "San pham: " + selected.getName() + "\nGia hien tai: " + AuctionViewFormatter.formatMoney(selected.getCurrentHighestBid()) + "\nNhap muc gia cua ban:",
                ""
        );
        if (bidInput == null || bidInput.isBlank()) {
            return;
        }

        try {
            double amount = Double.parseDouble(bidInput.trim());
            String result = auctionController.placeBid(selected.getId(), currentUser.getId(), amount);
            if ("SUCCESS".equals(result)) {
                NotificationUtil.success(ownerWindow(), "Thong bao", "Dat gia thanh cong.");
            } else {
                NotificationUtil.error(ownerWindow(), "Loi", result);
            }
            refreshData();
        } catch (NumberFormatException ex) {
            NotificationUtil.error(ownerWindow(), "Loi", "So tien khong hop le.");
        }
    }

    @FXML
    private void handleShowHistory() {
        if (auctionController == null) {
            NotificationUtil.warning(ownerWindow(), "Thong bao", "Chua gan AuctionController cho man hinh nguoi mua.");
            return;
        }

        AuctionItem selected = tableAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationUtil.warning(ownerWindow(), "Thong bao", "Hay chon mot phien dau gia.");
            return;
        }

        if (frame == null) {
            NotificationUtil.info(ownerWindow(), "Thong bao", "Hay noi controller voi AuthFrame de mo lich su bid bang FXML.");
            return;
        }

        List<BidTransaction> bids = auctionController.getBidsForAuction(selected.getId());
        frame.showBidHistoryDialog(selected, bids);
    }

    @FXML
    private void handleChangePassword() {
        if (currentUser == null) {
            NotificationUtil.warning(ownerWindow(), "Thong bao", "Chua co thong tin nguoi dung hien tai.");
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

    private javafx.stage.Window ownerWindow() {
        return frame == null ? null : frame.getWindow();
    }
}
