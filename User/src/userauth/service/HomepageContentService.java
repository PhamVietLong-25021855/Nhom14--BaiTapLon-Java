package userauth.service;

import userauth.dao.HomepageAnnouncementDAO;
import userauth.dao.HomepageAnnouncementDAOImpl;
import userauth.exception.ValidationException;
import userauth.model.HomepageAnnouncement;
import java.util.Comparator;
import java.util.List;

// Ghi chu file: File service; chua nghiep vu chinh va phoi hop giua controller, DAO va event.
// Khai bao lop HomepageContentService; chua xu ly nghiep vu va cac quy tac chinh cua he thong.
public class HomepageContentService {
    // Thuoc tinh: giu tham chieu den HomepageAnnouncementDAO de phoi hop xu ly.
    private final HomepageAnnouncementDAO announcementDAO;
    // Ham tao: khoi tao doi tuong HomepageContentService voi cac phu thuoc can thiet.
    public HomepageContentService() {
        this(new HomepageAnnouncementDAOImpl());
    }
    // Ham tao: khoi tao doi tuong HomepageContentService voi cac phu thuoc can thiet.
    public HomepageContentService(HomepageAnnouncementDAO announcementDAO) {
        this.announcementDAO = announcementDAO;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get all announcements.
    public synchronized List<HomepageAnnouncement> getAllAnnouncements() {
        return announcementDAO.findAll().stream()
                .sorted(Comparator.comparingLong(HomepageAnnouncement::getUpdatedAt).reversed())
                .map(this::copyAnnouncement)
                .toList();
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac save announcement.
    public synchronized void saveAnnouncement(Integer announcementId, String title, String summary, String details,
                                              String scheduleText, Integer linkedAuctionId, int authorId)
            throws ValidationException {
        String normalizedTitle = normalize(title);
        String normalizedSummary = normalize(summary);
        String normalizedDetails = normalize(details);
        String normalizedSchedule = normalize(scheduleText);
        int safeAuctionId = linkedAuctionId == null ? -1 : linkedAuctionId;

        validate(normalizedTitle, normalizedSummary, normalizedSchedule);

        long now = System.currentTimeMillis();
        HomepageAnnouncement existing = findById(announcementId == null ? -1 : announcementId);
        if (existing == null) {
            announcementDAO.save(new HomepageAnnouncement(
                    0,
                    normalizedTitle,
                    normalizedSummary,
                    normalizedDetails,
                    normalizedSchedule,
                    safeAuctionId,
                    authorId,
                    now,
                    now
            ));
        } else {
            existing.setTitle(normalizedTitle);
            existing.setSummary(normalizedSummary);
            existing.setDetails(normalizedDetails);
            existing.setScheduleText(normalizedSchedule);
            existing.setLinkedAuctionId(safeAuctionId);
            existing.setAuthorId(authorId);
            existing.setUpdatedAt(now);
            announcementDAO.update(existing);
        }
    }
    // Phuong thuc: huy, xoa, dong hoac don trang thai cho thao tac delete announcement.
    public synchronized void deleteAnnouncement(int announcementId) throws ValidationException {
        HomepageAnnouncement existing = findById(announcementId);
        if (existing == null) {
            throw new ValidationException("Homepage announcement not found.");
        }

        announcementDAO.delete(announcementId);
    }
    // Phuong thuc: kiem tra dieu kien hoac xac thuc cho thao tac validate.
    private void validate(String title, String summary, String scheduleText) throws ValidationException {
        if (title.isEmpty()) {
            throw new ValidationException("Announcement title cannot be empty.");
        }
        if (summary.isEmpty()) {
            throw new ValidationException("Announcement summary cannot be empty.");
        }
        if (scheduleText.isEmpty()) {
            throw new ValidationException("Auction schedule information cannot be empty.");
        }
    }
    // Phuong thuc: thuc hien chuc nang normalize trong lop HomepageContentService.
    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac find by id.
    private HomepageAnnouncement findById(int announcementId) {
        return announcementDAO.findById(announcementId);
    }
    // Phuong thuc: thuc hien chuc nang copy announcement trong lop HomepageContentService.
    private HomepageAnnouncement copyAnnouncement(HomepageAnnouncement source) {
        return new HomepageAnnouncement(
                source.getId(),
                source.getTitle(),
                source.getSummary(),
                source.getDetails(),
                source.getScheduleText(),
                source.getLinkedAuctionId(),
                source.getAuthorId(),
                source.getCreatedAt(),
                source.getUpdatedAt()
        );
    }
}
