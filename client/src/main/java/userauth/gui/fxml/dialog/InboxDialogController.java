package userauth.gui.fxml.dialog;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import userauth.controller.NotificationController;
import userauth.gui.fxml.shared.NotificationUtil;
import userauth.gui.fxml.shared.UiAsync;
import userauth.gui.fxml.shared.UiText;
import userauth.model.Notification;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class InboxDialogController {
    private static final DateTimeFormatter MESSAGE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    @FXML
    private VBox notificationContainer;

    @FXML
    private Label lblInboxSummary;

    @FXML
    private Button btnClearAll;

    private Stage dialogStage;
    private NotificationController notificationController;
    private int userId = -1;
    private List<Notification> notifications = new ArrayList<>();

    @FXML
    void inboxClose(ActionEvent event) {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setNotificationContext(NotificationController notificationController, int userId) {
        this.notificationController = notificationController;
        this.userId = userId;
        updateBulkDeleteButton();
    }

    public void loadNotifications(List<Notification> userNotifications) {
        notifications = userNotifications == null ? new ArrayList<>() : new ArrayList<>(userNotifications);
        renderNotifications();
    }

    private void renderNotifications() {
        notificationContainer.getChildren().clear();

        int count = notifications.size();
        lblInboxSummary.setText(count == 1 ? "1 message" : count + " messages");
        updateBulkDeleteButton();

        if (count == 0) {
            notificationContainer.getChildren().add(createEmptyState());
            return;
        }

        for (Notification note : notifications) {
            notificationContainer.getChildren().add(createNotificationCard(note));
        }
    }

    private HBox createNotificationCard(Notification note) {
        HBox card = new HBox(14);
        card.setAlignment(Pos.TOP_LEFT);
        card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().add("inbox-message-card");

        StackPane icon = new StackPane();
        icon.setMinSize(44, 44);
        icon.setPrefSize(44, 44);
        icon.getStyleClass().add("inbox-message-icon");

        Label iconText = new Label("!");
        iconText.getStyleClass().add("inbox-message-icon-text");
        icon.getChildren().add(iconText);

        VBox contentBox = new VBox(7);
        contentBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(UiText.text(normalize(note == null ? null : note.getTitle(), "Notification")));
        title.setMaxWidth(Double.MAX_VALUE);
        title.setWrapText(true);
        title.getStyleClass().add("inbox-message-title");
        HBox.setHgrow(title, Priority.ALWAYS);

        Label time = new Label(formatTime(note == null ? 0L : note.getCreated_at()));
        time.getStyleClass().add("inbox-message-time");

        titleRow.getChildren().addAll(title, time);

        Label body = new Label(UiText.text(normalize(note == null ? null : note.getContent(), "No message content.")));
        body.setMaxWidth(Double.MAX_VALUE);
        body.setWrapText(true);
        body.getStyleClass().add("inbox-message-body");

        Label meta = new Label("System notification");
        meta.getStyleClass().add("inbox-message-meta");

        Button deleteButton = new Button("DELETE");
        deleteButton.setMinWidth(82);
        deleteButton.getStyleClass().addAll("button", "inbox-delete-button");
        deleteButton.setDisable(!canDelete(note));
        deleteButton.setOnAction(event -> deleteNotification(note));

        contentBox.getChildren().addAll(titleRow, body, meta);
        card.getChildren().addAll(icon, contentBox, deleteButton);
        return card;
    }

    @FXML
    private void handleDeleteAllNotifications(ActionEvent event) {
        if (notifications.isEmpty()) {
            return;
        }
        if (!hasDeleteContext()) {
            NotificationUtil.warning(dialogStage, "Inbox", "Delete service is unavailable.");
            return;
        }
        boolean confirmed = NotificationUtil.confirm(
                dialogStage,
                "Delete all messages",
                "Delete all personal messages in this inbox?");
        if (!confirmed) {
            return;
        }

        btnClearAll.setDisable(true);
        UiAsync.run(
                () -> notificationController.deleteUserNotifications(userId),
                deletedCount -> {
                    notifications.removeIf(this::canDelete);
                    renderNotifications();
                    NotificationUtil.success(dialogStage, "Inbox", deletedCount + " messages deleted.");
                },
                error -> {
                    updateBulkDeleteButton();
                    NotificationUtil.error(dialogStage, "Inbox", error.getMessage());
                }
        );
    }

    private void deleteNotification(Notification note) {
        if (!canDelete(note)) {
            NotificationUtil.warning(dialogStage, "Inbox", "This message cannot be deleted.");
            return;
        }

        UiAsync.run(
                () -> notificationController.deleteNotification(userId, note.getId()),
                deleted -> {
                    if (!deleted) {
                        NotificationUtil.warning(dialogStage, "Inbox", "Message was not found or cannot be deleted.");
                        return;
                    }
                    notifications.removeIf(notification -> notification.getId() == note.getId());
                    renderNotifications();
                    NotificationUtil.success(dialogStage, "Inbox", "Message deleted.");
                },
                error -> NotificationUtil.error(dialogStage, "Inbox", error.getMessage())
        );
    }

    private VBox createEmptyState() {
        VBox empty = new VBox(8);
        empty.setAlignment(Pos.CENTER);
        empty.setMinHeight(360);
        empty.setMaxWidth(Double.MAX_VALUE);
        empty.getStyleClass().add("inbox-empty-card");

        Label title = new Label("No notifications yet");
        title.getStyleClass().add("inbox-empty-title");

        Label body = new Label("New account, wallet, and auction updates will appear here.");
        body.setWrapText(true);
        body.getStyleClass().add("inbox-empty-copy");

        empty.getChildren().addAll(title, body);
        return empty;
    }

    private void updateBulkDeleteButton() {
        if (btnClearAll != null) {
            btnClearAll.setDisable(!hasDeleteContext() || notifications.stream().noneMatch(this::canDelete));
        }
    }

    private boolean canDelete(Notification note) {
        return hasDeleteContext() && note != null && note.getId() > 0 && note.getUser_id() == userId;
    }

    private boolean hasDeleteContext() {
        return notificationController != null && userId > 0;
    }

    private String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String formatTime(long timestamp) {
        if (timestamp <= 0) {
            return "Unknown time";
        }
        long millis = timestamp < 10_000_000_000L ? timestamp * 1000L : timestamp;
        return MESSAGE_TIME_FORMATTER.format(Instant.ofEpochMilli(millis));
    }
}
