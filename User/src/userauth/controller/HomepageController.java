package userauth.controller;

import userauth.exception.ValidationException;
import userauth.model.HomepageAnnouncement;
import userauth.model.Role;
import userauth.model.User;
import userauth.service.HomepageContentService;

import java.util.List;

public class HomepageController {
    private final HomepageContentService homepageContentService;

    public HomepageController(HomepageContentService homepageContentService) {
        this.homepageContentService = homepageContentService;
    }

    public List<HomepageAnnouncement> getAllAnnouncements() {
        return homepageContentService.getAllAnnouncements();
    }

    public String saveAnnouncement(User currentUser, Integer announcementId, String title, String summary,
                                   String details, String scheduleText, Integer linkedAuctionId) {
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
            return "Chi admin moi duoc dang bai len trang chu.";
        }

        try {
            homepageContentService.saveAnnouncement(
                    announcementId,
                    title,
                    summary,
                    details,
                    scheduleText,
                    linkedAuctionId,
                    currentUser.getId()
            );
            return "SUCCESS";
        } catch (ValidationException e) {
            return e.getMessage();
        }
    }

    public String deleteAnnouncement(User currentUser, int announcementId) {
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
            return "Chi admin moi duoc xoa bai dang tren trang chu.";
        }

        try {
            homepageContentService.deleteAnnouncement(announcementId);
            return "SUCCESS";
        } catch (ValidationException e) {
            return e.getMessage();
        }
    }
}
