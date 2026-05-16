package uet.auctionsystem.controller;

import uet.auctionsystem.exception.ValidationException;
import uet.auctionsystem.model.HomepageAnnouncement;
import uet.auctionsystem.model.Role;
import uet.auctionsystem.model.User;
import uet.auctionsystem.service.HomepageContentService;
import java.util.List;

// Ghi chu file: File controller nam giua giao dien va service; nhan lenh tu UI va goi nghiep vu tuong ung.
// Khai bao lop HomepageController; dieu phoi thao tac UI va chuyen tiep yeu cau xu ly nghiep vu.
public class HomepageController {
    // Thuoc tinh: giu tham chieu den HomepageContentService de phoi hop xu ly.
    private final HomepageContentService homepageContentService;
    // Ham tao: khoi tao doi tuong HomepageController voi cac phu thuoc can thiet.
    public HomepageController(HomepageContentService homepageContentService) {
        this.homepageContentService = homepageContentService;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get all announcements.
    public List<HomepageAnnouncement> getAllAnnouncements() {
        return homepageContentService.getAllAnnouncements();
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac save announcement.
    public String saveAnnouncement(User currentUser, Integer announcementId, String title, String summary,
                                   String details, String scheduleText, Integer linkedAuctionId) {
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
            return "Only admins can publish announcements to the homepage.";
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
    // Phuong thuc: huy, xoa, dong hoac don trang thai cho thao tac delete announcement.
    public String deleteAnnouncement(User currentUser, int announcementId) {
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
            return "Only admins can delete homepage announcements.";
        }

        try {
            homepageContentService.deleteAnnouncement(announcementId);
            return "SUCCESS";
        } catch (ValidationException e) {
            return e.getMessage();
        }
    }
}
