package userauth.remote;

import userauth.api.NotificationApi;
import userauth.exception.ItemNotFoundException;
import userauth.exception.UnauthorizedException;
import userauth.exception.ValidationException;
import userauth.model.AutoBid;
import userauth.model.Notification;
import userauth.network.NetworkActions;

import java.util.List;

public class RemoteNotificationService implements NotificationApi {
    private final RemoteAuctionClient client;

    public RemoteNotificationService(RemoteAuctionClient client) {
        this.client = client;
    }

    @Override
    public void createNotification(int user_id, String title, String content) throws ValidationException, UnauthorizedException, ItemNotFoundException {
        try {
            String result = (String) client.call(NetworkActions.NOTIFICATION_CREATE, "user_id",user_id , "title", title, "content", content);
            if (!"SUCCESS".equals(result)) throw new ValidationException(result);
        } catch (RemoteServerException ex) {
            // Map all server-side errors to ValidationException to satisfy interface contract
            throw new ValidationException(ex.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Notification> findUserNotification(int user_id) {
        try {
            return (List<Notification>) client.call(NetworkActions.NOTIFICATION_GET, "user_id", user_id);
        } catch (RemoteServerException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }
}
