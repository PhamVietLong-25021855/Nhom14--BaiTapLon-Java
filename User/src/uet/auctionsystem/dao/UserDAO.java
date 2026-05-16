package uet.auctionsystem.dao;

import uet.auctionsystem.model.User;
import java.util.List;

// Ghi chu file: File DAO; dinh nghia hoac trien khai cac thao tac doc ghi du lieu voi database.
// Khai bao giao dien UserDAO; phu trach hop dong hoac truy cap du lieu cho database.
public interface UserDAO {
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac save.
    void save(User user);
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac update.
    void update(User user);
    // Phuong thuc: lay hoac doc du lieu cho thao tac find by username.
    User findByUsername(String username);
    // Phuong thuc: lay hoac doc du lieu cho thao tac find by email.
    User findByEmail(String email);
    // Phuong thuc: lay hoac doc du lieu cho thao tac find all.
    List<User> findAll();
}
