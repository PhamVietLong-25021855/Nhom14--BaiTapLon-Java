package userauth.gui.fxml;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;

public class HomeAuctionCardController {
    private static final long ENDING_SOON_THRESHOLD_MS = 5 * 60 * 1000;

    @FXML
    private Label lblInitial;

    @FXML
    private ImageView imgAuction;

    @FXML
    private Label lblTitle;

    @FXML
    private Label lblStatusChip;

    @FXML
    private Label lblCategory;

    @FXML
    private Label lblSchedule;

    @FXML
    private Label lblHighestBid;

    @FXML
    private Label lblTimeInfo;

    @FXML
    private Label lblBidCount;

    @FXML
    private void initialize() {
        AuctionImageUtil.installRoundedClip(imgAuction, 22, 22);
    }

    public void setAuction(AuctionItem auction) {
        setAuction(auction, 0);
    }

    public void setAuction(AuctionItem auction, int bidCount) {
        if (auction == null) {
            return;
        }

        AuctionImageUtil.applyAuctionImage(imgAuction, lblInitial, auction.getImageSource(), auction.getName());
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

    private void setStatusChip(String text, String extraStyleClass) {
        lblStatusChip.setText(UiText.text(text));
        lblStatusChip.getStyleClass().removeAll("status-chip-live", "status-chip-upcoming", "status-chip-admin");
        if (!lblStatusChip.getStyleClass().contains("status-chip")) {
            lblStatusChip.getStyleClass().add("status-chip");
        }
        lblStatusChip.getStyleClass().add(extraStyleClass);
    }

    private String safeValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
