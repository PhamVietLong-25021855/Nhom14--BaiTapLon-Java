package userauth.service;

import userauth.dao.HomepageAnnouncementDAO;
import userauth.dao.HomepageAnnouncementDAOImpl;
import userauth.exception.ValidationException;
import userauth.model.HomepageAnnouncement;

import java.util.Comparator;
import java.util.List;

public class HomepageContentService {
    private final HomepageAnnouncementDAO announcementDAO;

    public HomepageContentService() {
        this(new HomepageAnnouncementDAOImpl());
    }

    public HomepageContentService(HomepageAnnouncementDAO announcementDAO) {
        this.announcementDAO = announcementDAO;
    }

    public synchronized List<HomepageAnnouncement> getAllAnnouncements() {
        return announcementDAO.findAll().stream()
                .sorted(Comparator.comparingLong(HomepageAnnouncement::getUpdatedAt).reversed())
                .map(this::copyAnnouncement)
                .toList();
    }

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

    public synchronized void deleteAnnouncement(int announcementId) throws ValidationException {
        HomepageAnnouncement existing = findById(announcementId);
        if (existing == null) {
            throw new ValidationException("Homepage announcement not found.");
        }

        announcementDAO.delete(announcementId);
    }

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

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private HomepageAnnouncement findById(int announcementId) {
        return announcementDAO.findById(announcementId);
    }

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
