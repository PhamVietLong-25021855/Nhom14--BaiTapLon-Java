package userauth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import userauth.exception.ValidationException;
import userauth.model.HomepageAnnouncement;
import userauth.dao.TestDaos;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomepageContentServiceTest {
    private TestDaos.InMemoryHomepageAnnouncementDao announcementDao;
    private HomepageContentService service;

    @BeforeEach
    void setUp() {
        announcementDao = new TestDaos.InMemoryHomepageAnnouncementDao();
        service = new HomepageContentService(announcementDao);
    }

    @Test
    void getAllAnnouncementsSortsNewestFirstAndReturnsCopies() {
        announcementDao.save(new HomepageAnnouncement(0, "Old", "Summary", "Details", "08:00", -1, 1, 1000L, 1000L));
        announcementDao.save(new HomepageAnnouncement(0, "New", "Summary", "Details", "09:00", 10, 1, 1000L, 2000L));

        List<HomepageAnnouncement> announcements = service.getAllAnnouncements();

        assertEquals("New", announcements.get(0).getTitle());
        assertTrue(announcements.get(0).hasLinkedAuction());
        assertFalse(announcements.get(1).hasLinkedAuction());

        announcements.get(0).setTitle("Changed outside service");

        assertEquals("New", announcementDao.findById(2).getTitle());
    }

    @Test
    void saveAnnouncementTrimsFieldsAndStoresSafeAuctionId() throws Exception {
        service.saveAnnouncement(null, "  Title  ", "  Summary  ", "  Details  ", "  Tomorrow  ", null, 99);

        HomepageAnnouncement saved = service.getAllAnnouncements().get(0);

        assertEquals("Title", saved.getTitle());
        assertEquals("Summary", saved.getSummary());
        assertEquals("Details", saved.getDetails());
        assertEquals("Tomorrow", saved.getScheduleText());
        assertEquals(-1, saved.getLinkedAuctionId());
    }

    @Test
    void saveAnnouncementRejectsMissingRequiredFields() {
        assertThrows(ValidationException.class,
                () -> service.saveAnnouncement(null, " ", "Summary", "Details", "Tomorrow", null, 99));
        assertThrows(ValidationException.class,
                () -> service.saveAnnouncement(null, "Title", " ", "Details", "Tomorrow", null, 99));
        assertThrows(ValidationException.class,
                () -> service.saveAnnouncement(null, "Title", "Summary", "Details", " ", null, 99));
    }

    @Test
    void updateAndDeleteExistingAnnouncement() throws Exception {
        service.saveAnnouncement(null, "Title", "Summary", "Details", "Tomorrow", null, 99);
        int id = service.getAllAnnouncements().get(0).getId();

        service.saveAnnouncement(id, "Updated", "Updated summary", "Updated details", "Next week", 5, 100);
        service.deleteAnnouncement(id);

        assertTrue(service.getAllAnnouncements().isEmpty());
        assertThrows(ValidationException.class, () -> service.deleteAnnouncement(id));
    }
}
