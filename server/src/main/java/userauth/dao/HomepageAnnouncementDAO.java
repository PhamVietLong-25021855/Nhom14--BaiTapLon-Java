package userauth.dao;

import userauth.model.HomepageAnnouncement;

import java.util.List;

public interface HomepageAnnouncementDAO {
    void save(HomepageAnnouncement announcement);
    void update(HomepageAnnouncement announcement);
    void delete(int id);
    HomepageAnnouncement findById(int id);
    List<HomepageAnnouncement> findAll();
}
