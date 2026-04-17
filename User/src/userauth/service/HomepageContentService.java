package userauth.service;

import userauth.exception.ValidationException;
import userauth.model.HomepageAnnouncement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HomepageContentService {
    private final HomepageFileService fileService;
    private final List<HomepageAnnouncement> announcements;
    private int nextId;

    public HomepageContentService() {
        this.fileService = new HomepageFileService();
        this.announcements = new ArrayList<>(fileService.loadAnnouncementsFromFile());
        this.nextId = findNextId();
    }

    public synchronized List<HomepageAnnouncement> getAllAnnouncements() {
        return announcements.stream()
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
            announcements.add(new HomepageAnnouncement(
                    nextId++,
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
        }

        persist();
    }

    public synchronized void deleteAnnouncement(int announcementId) throws ValidationException {
        HomepageAnnouncement existing = findById(announcementId);
        if (existing == null) {
            throw new ValidationException("Khong tim thay bai dang tren trang chu.");
        }

        announcements.remove(existing);
        persist();
    }

    private void validate(String title, String summary, String scheduleText) throws ValidationException {
        if (title.isEmpty()) {
            throw new ValidationException("Tieu de bai dang khong duoc rong.");
        }
        if (summary.isEmpty()) {
            throw new ValidationException("Tom tat bai dang khong duoc rong.");
        }
        if (scheduleText.isEmpty()) {
            throw new ValidationException("Thong tin lich dau gia khong duoc rong.");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private HomepageAnnouncement findById(int announcementId) {
        for (HomepageAnnouncement announcement : announcements) {
            if (announcement.getId() == announcementId) {
                return announcement;
            }
        }
        return null;
    }

    private int findNextId() {
        int maxId = 0;
        for (HomepageAnnouncement announcement : announcements) {
            if (announcement.getId() > maxId) {
                maxId = announcement.getId();
            }
        }
        return maxId + 1;
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

    private void persist() {
        fileService.saveAnnouncementsToFile(announcements);
    }
}
