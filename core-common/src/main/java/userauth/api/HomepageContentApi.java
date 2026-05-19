package userauth.api;

import userauth.exception.ValidationException;
import userauth.model.HomepageAnnouncement;

import java.util.List;

public interface HomepageContentApi {
    List<HomepageAnnouncement> getAllAnnouncements();
    void saveAnnouncement(Integer announcementId, String title, String summary, String details,
                          String scheduleText, Integer linkedAuctionId, int authorId) throws ValidationException;
    void deleteAnnouncement(int announcementId) throws ValidationException;
}
