package userauth.api;

import userauth.exception.ItemNotFoundException;
import userauth.exception.UnauthorizedException;
import userauth.exception.ValidationException;
import userauth.model.Notification;

import java.util.List;

public interface NotificationApi {
    public void createNotification(int user_id, String title, String content)
            throws ValidationException, UnauthorizedException, ItemNotFoundException;
    public List<Notification> findUserNotification (int user_id);
}
