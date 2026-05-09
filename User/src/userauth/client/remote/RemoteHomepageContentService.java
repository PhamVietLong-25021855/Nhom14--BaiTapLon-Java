package userauth.client.remote;

import userauth.api.HomepageContentApi;
import userauth.exception.ValidationException;
import userauth.model.HomepageAnnouncement;
import userauth.network.NetworkActions;

import java.util.List;

/** HomepageContentService chạy ở client: gọi Server thay vì DAO. */
public class RemoteHomepageContentService implements HomepageContentApi {
    private final RemoteAuctionClient client;

    public RemoteHomepageContentService(RemoteAuctionClient client) {
        this.client = client;
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized List<HomepageAnnouncement> getAllAnnouncements() {
        return (List<HomepageAnnouncement>) client.call(NetworkActions.HOMEPAGE_ALL);
    }

    @Override
    public synchronized void saveAnnouncement(Integer announcementId, String title, String summary, String details,
                                              String scheduleText, Integer linkedAuctionId, int authorId)
            throws ValidationException {
        String result = (String) client.call(NetworkActions.HOMEPAGE_SAVE,
                "announcementId", announcementId, "title", title, "summary", summary, "details", details,
                "scheduleText", scheduleText, "linkedAuctionId", linkedAuctionId, "authorId", authorId);
        if (!"SUCCESS".equals(result)) {
            throw new ValidationException(result);
        }
    }

    @Override
    public synchronized void deleteAnnouncement(int announcementId) throws ValidationException {
        String result = (String) client.call(NetworkActions.HOMEPAGE_DELETE, "announcementId", announcementId);
        if (!"SUCCESS".equals(result)) {
            throw new ValidationException(result);
        }
    }
}
