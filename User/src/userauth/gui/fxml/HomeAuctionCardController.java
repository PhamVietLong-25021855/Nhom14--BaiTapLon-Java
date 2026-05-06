package userauth.gui.fxml;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;

// Ghi chu file: File controller JavaFX; dieu khien hanh vi cua man hinh, hop thoai hoac thanh phan UI.
// Khai bao lop HomeAuctionCardController; dieu khien mot man hinh hoac thanh phan JavaFX cu the.
public class HomeAuctionCardController {
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho ms.
    private static final long ENDING_SOON_THRESHOLD_MS = 5 * 60 * 1000;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl initial.
    private Label lblInitial;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho img auction.
    private ImageView imgAuction;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl title.
    private Label lblTitle;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl status chip.
    private Label lblStatusChip;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl category.
    private Label lblCategory;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl schedule.
    private Label lblSchedule;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl highest bid.
    private Label lblHighestBid;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl time info.
    private Label lblTimeInfo;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl bid count.
    private Label lblBidCount;

    @FXML
    // Phuong thuc: khoi dong hoac khoi tao tien trinh initialize.
    private void initialize() {
        AuctionImageUtil.installRoundedClip(imgAuction, 22, 22);
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set auction.
    public void setAuction(AuctionItem auction) {
        setAuction(auction, 0);
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set auction.
    public void setAuction(AuctionItem auction, int bidCount) {
        if (auction == null) {
            return;
        }

        AuctionImageUtil.applyAuctionImage(imgAuction, lblInitial, auction.getImageData(), auction.getImageSource(), auction.getName());
        lblTitle.setText(safeValue(auction.getName(), UiText.text("Product")));
        lblCategory.setText(UiText.text("Category") + ": " + safeValue(auction.getCategory(), "-"));
        lblSchedule.setText(UiText.text("Schedule") + ": " + AuctionViewFormatter.formatScheduleRange(auction));
        lblHighestBid.setText(AuctionViewFormatter.formatMoney(auction.getCurrentHighestBid()));
        lblBidCount.setText(bidCount + " " + UiText.text("bid"));
        lblTimeInfo.setText(AuctionViewFormatter.formatTimeLeft(auction));

        if (auction.getStatus() == AuctionStatus.RUNNING) {
            boolean endingSoon = (auction.getEndTime() - System.currentTimeMillis()) <= ENDING_SOON_THRESHOLD_MS;
            setStatusChip(endingSoon ? "ENDING SOON" : "LIVE", endingSoon ? "status-chip-danger" : "status-chip-live");
        } else {
            setStatusChip("OPENING SOON", "status-chip-upcoming");
        }
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set status chip.
    private void setStatusChip(String text, String extraStyleClass) {
        lblStatusChip.setText(UiText.text(text));
        lblStatusChip.getStyleClass().removeAll("status-chip-live", "status-chip-upcoming", "status-chip-admin");
        if (!lblStatusChip.getStyleClass().contains("status-chip")) {
            lblStatusChip.getStyleClass().add("status-chip");
        }
        lblStatusChip.getStyleClass().add(extraStyleClass);
    }
    // Phuong thuc: thuc hien chuc nang safe value trong lop HomeAuctionCardController.
    private String safeValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
