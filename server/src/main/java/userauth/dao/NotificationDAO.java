package userauth.dao;


import userauth.model.Notification;

import java.util.List;

public interface NotificationDAO {
    void saveNotification(Notification  item);
    List<Notification> findNotificationToUser(int user_id);
}
