package userauth.dao;

import userauth.model.HomepageAnnouncement;
import java.util.List;

// Ghi chu file: File DAO; dinh nghia hoac trien khai cac thao tac doc ghi du lieu voi database.
// Khai bao giao dien HomepageAnnouncementDAO; phu trach hop dong hoac truy cap du lieu cho database.
public interface HomepageAnnouncementDAO {
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac save.
    void save(HomepageAnnouncement announcement);
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac update.
    void update(HomepageAnnouncement announcement);
    // Phuong thuc: huy, xoa, dong hoac don trang thai cho thao tac delete.
    void delete(int id);
    // Phuong thuc: lay hoac doc du lieu cho thao tac find by id.
    HomepageAnnouncement findById(int id);
    // Phuong thuc: lay hoac doc du lieu cho thao tac find all.
    List<HomepageAnnouncement> findAll();
}
