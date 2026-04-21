package userauth.gui.fxml;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;

public class HomeAuctionCardController {
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

    public void setAuction(AuctionItem auction) {
        if (auction == null) {
            return;
        }

        lblTitle.setText(safeValue(auction.getName(), "San pham"));
        lblCategory.setText("Danh muc: " + safeValue(auction.getCategory(), "-"));
        lblSchedule.setText("Lich: " + AuctionViewFormatter.formatScheduleRange(auction));
        lblHighestBid.setText("Gia hien tai: " + AuctionViewFormatter.formatMoney(auction.getCurrentHighestBid()));
        lblTimeInfo.setText(
                auction.getStatus() == AuctionStatus.RUNNING
                        ? "Con lai: " + AuctionViewFormatter.formatTimeLeft(auction)
                        : AuctionViewFormatter.formatTimeLeft(auction)
        );

        if (auction.getStatus() == AuctionStatus.RUNNING) {
            setStatusChip("DANG DAU GIA", "status-chip-live");
        } else {
            setStatusChip("SAP MO", "status-chip-upcoming");
        }
    }

    private void setStatusChip(String text, String extraStyleClass) {
        lblStatusChip.setText(text);
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
