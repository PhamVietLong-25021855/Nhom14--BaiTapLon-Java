package userauth.gui.fxml.dialog;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import userauth.controller.NotificationController;
import userauth.model.Notification;
import userauth.model.User;

import java.io.IOException;
import java.util.List;

public class InboxDialogController {

    @FXML
    private VBox notificationContainer;

    private Stage dialogStage;

    @FXML
    void inboxClose(ActionEvent event) {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void loadNotifications(List<Notification> userNotifications) {
        try {
            for (Notification note : userNotifications) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/userauth/gui.fxml/dialog/toast-notification.fxml"));
                Parent notificationView = loader.load();

                ToastNotificationController controller = loader.getController();
                controller.configure(note.getTitle(), note.getContent(), "toast-info");

                notificationContainer.getChildren().add(notificationView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
