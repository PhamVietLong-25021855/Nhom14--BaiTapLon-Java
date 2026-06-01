package userauth.service;

import userauth.api.NotificationApi;
import userauth.dao.NotificationDAO;
import userauth.exception.ValidationException;
import userauth.model.Notification;

import java.util.List;

public class NotificationService implements NotificationApi {
    private  final NotificationDAO notificationDAO;
    public NotificationService (NotificationDAO notificationDAO){
        this.notificationDAO = notificationDAO;
    }

    @Override
    public void createNotification (int user_id, String title, String content)
    throws ValidationException {
        if (user_id < 0){
            throw  new ValidationException("Invalid user_id for notification");
        }
        long now = System.currentTimeMillis();
        Notification notification = new Notification(0, user_id, title, content, now);
        notificationDAO.saveNotification(notification);
    }

    @Override
    public List<Notification> findUserNotification (int user_id){
        return notificationDAO.findNotificationToUser(user_id);
    }

    @Override
    public boolean deleteNotification(int user_id, int notification_id) {
        validateUserId(user_id);
        if (notification_id <= 0) {
            throw new IllegalArgumentException("Invalid notification id");
        }
        return notificationDAO.deleteNotification(user_id, notification_id);
    }

    @Override
    public int deleteUserNotifications(int user_id) {
        validateUserId(user_id);
        return notificationDAO.deleteNotificationsForUser(user_id);
    }

    private void validateUserId(int user_id) {
        if (user_id <= 0) {
            throw new IllegalArgumentException("Invalid user_id for notification");
        }
    }
}
