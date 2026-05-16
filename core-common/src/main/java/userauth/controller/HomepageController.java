package userauth.controller;

import userauth.api.HomepageContentApi;
import userauth.exception.ValidationException;
import userauth.model.HomepageAnnouncement;
import userauth.model.User;

import java.util.List;

public class HomepageController {
    private final HomepageContentApi homepageContentService;

    public HomepageController(HomepageContentApi homepageContentService) {
        this.homepageContentService = homepageContentService;
    }

    public List<HomepageAnnouncement> getAllAnnouncements() {
        return homepageContentService.getAllAnnouncements();
    }

    public String saveAnnouncement(User currentUser, Integer announcementId, String title, String summary,
                                  String details, String scheduleText, Integer linkedAuctionId) {
        try {
            homepageContentService.saveAnnouncement(announcementId, title, summary, details, scheduleText, linkedAuctionId, currentUser.getId());
            return "SUCCESS";
        } catch (ValidationException e) {
            return e.getMessage();
        }
    }

    public String deleteAnnouncement(User currentUser, int announcementId) {
        try {
            homepageContentService.deleteAnnouncement(announcementId);
            return "SUCCESS";
        } catch (ValidationException e) {
            return e.getMessage();
        }
    }
}
