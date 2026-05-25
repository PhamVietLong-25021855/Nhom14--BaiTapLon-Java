package userauth.controller;

import userauth.api.AutobidApi;
import userauth.api.NotificationApi;
import userauth.exception.ValidationException;
import userauth.model.Notification;

import java.util.ArrayList;
import java.util.List;

public class NotificationController {
    private final NotificationApi notificationService;

    public NotificationController(NotificationApi notificationService) {
        this.notificationService = notificationService;
    }

    public String createNotification(int user_id, String title, String content) {
        try {
            notificationService.createNotification(user_id, title, content);
            return "SUCCESS";
        } catch (ValidationException e) {
            return e.getMessage();
        } catch (Exception e) {
            return "An unexpected error occurred while saving the notification rule: " + e.getMessage();
        }
    }

    public List<Notification> findUserNotification (int user_id){return notificationService.findUserNotification(user_id);}

}
