package userauth.controller;

import userauth.client.network.RemoteApiClient;
import userauth.common.RemoteAction;
import userauth.common.RemoteResponse;
import userauth.exception.ValidationException;
import userauth.model.HomepageAnnouncement;
import userauth.model.Role;
import userauth.model.User;
import userauth.service.HomepageContentService;

import java.util.List;

public class HomepageController extends RemoteControllerSupport {
    private final HomepageContentService homepageContentService;
    private final RemoteApiClient remoteApiClient;

    public HomepageController(HomepageContentService homepageContentService) {
        this.homepageContentService = homepageContentService;
        this.remoteApiClient = null;
    }

    public HomepageController(RemoteApiClient remoteApiClient) {
        this.homepageContentService = null;
        this.remoteApiClient = remoteApiClient;
    }

    @SuppressWarnings("unchecked")
    public List<HomepageAnnouncement> getAllAnnouncements() {
        if (remoteApiClient != null) {
            return requestList(remoteApiClient, RemoteAction.HOMEPAGE_GET_ALL);
        }

        return homepageContentService.getAllAnnouncements();
    }

    public String saveAnnouncement(User currentUser, Integer announcementId, String title, String summary,
                                   String details, String scheduleText, Integer linkedAuctionId) {
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
            return "Only admins can publish announcements to the homepage.";
        }
        if (remoteApiClient != null) {
            return requestString(
                    RemoteAction.HOMEPAGE_SAVE,
                    currentUser,
                    announcementId,
                    title,
                    summary,
                    details,
                    scheduleText,
                    linkedAuctionId
            );
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
            return "Only admins can delete homepage announcements.";
        }
        if (remoteApiClient != null) {
            return requestString(RemoteAction.HOMEPAGE_DELETE, currentUser, announcementId);
        }

        try {
            homepageContentService.deleteAnnouncement(announcementId);
            return "SUCCESS";
        } catch (ValidationException e) {
            return e.getMessage();
        }
    }

    private String requestString(RemoteAction action, Object... arguments) {
        return requestString(remoteApiClient, action, arguments);
    }
}
