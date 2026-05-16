package uet.auctionsystem.gui.fxml;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import uet.auctionsystem.model.AuctionItem;
import uet.auctionsystem.model.HomepageAnnouncement;

// Ghi chu file: File controller JavaFX; dieu khien hanh vi cua man hinh, hop thoai hoac thanh phan UI.
// Khai bao lop HomeAnnouncementCardController; dieu khien mot man hinh hoac thanh phan JavaFX cu the.
public class HomeAnnouncementCardController {
    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl title.
    private Label lblTitle;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl status chip.
    private Label lblStatusChip;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl summary.
    private Label lblSummary;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl schedule.
    private Label lblSchedule;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho linked auction box.
    private VBox linkedAuctionBox;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl linked auction title.
    private Label lblLinkedAuctionTitle;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl linked auction schedule.
    private Label lblLinkedAuctionSchedule;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl details.
    private Label lblDetails;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho lbl updated at.
    private Label lblUpdatedAt;
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set announcement.
    public void setAnnouncement(HomepageAnnouncement announcement, AuctionItem linkedAuction) {
        if (announcement == null) {
            return;
        }

        lblTitle.setText(safeValue(announcement.getTitle(), UiText.text("Notification")));
        lblSummary.setText(safeValue(announcement.getSummary(), ""));
        lblSchedule.setText(UiText.text("Posted schedule") + ": " + safeValue(announcement.getScheduleText(), "-"));
        lblUpdatedAt.setText(UiText.text("Updated at") + ": " + AuctionViewFormatter.formatDateTime(announcement.getUpdatedAt()));
        setStatusChip("ADMIN", "status-chip-admin");

        String details = safeValue(announcement.getDetails(), "");
        boolean hasDetails = !details.isBlank();
        lblDetails.setText(details);
        setVisibleState(lblDetails, hasDetails);

        boolean hasLinkedAuction = linkedAuction != null;
        setVisibleState(linkedAuctionBox, hasLinkedAuction);
        if (hasLinkedAuction) {
            lblLinkedAuctionTitle.setText(UiText.text("Linked auction") + ": " + safeValue(linkedAuction.getName(), "-"));
            lblLinkedAuctionSchedule.setText(UiText.text("Schedule") + ": " + AuctionViewFormatter.formatScheduleRange(linkedAuction));
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
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set visible state.
    private void setVisibleState(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }
    // Phuong thuc: thuc hien chuc nang safe value trong lop HomeAnnouncementCardController.
    private String safeValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
