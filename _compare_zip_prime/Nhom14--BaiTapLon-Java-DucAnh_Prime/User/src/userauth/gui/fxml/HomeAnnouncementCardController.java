package userauth.gui.fxml;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import userauth.model.AuctionItem;
import userauth.model.HomepageAnnouncement;

public class HomeAnnouncementCardController {
    @FXML
    private Label lblTitle;

    @FXML
    private Label lblStatusChip;

    @FXML
    private Label lblSummary;

    @FXML
    private Label lblSchedule;

    @FXML
    private VBox linkedAuctionBox;

    @FXML
    private Label lblLinkedAuctionTitle;

    @FXML
    private Label lblLinkedAuctionSchedule;

    @FXML
    private Label lblDetails;

    @FXML
    private Label lblUpdatedAt;

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

    private void setStatusChip(String text, String extraStyleClass) {
        lblStatusChip.setText(UiText.text(text));
        lblStatusChip.getStyleClass().removeAll("status-chip-live", "status-chip-upcoming", "status-chip-admin");
        if (!lblStatusChip.getStyleClass().contains("status-chip")) {
            lblStatusChip.getStyleClass().add("status-chip");
        }
        lblStatusChip.getStyleClass().add(extraStyleClass);
    }

    private void setVisibleState(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private String safeValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
