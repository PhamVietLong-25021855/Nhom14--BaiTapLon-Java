package userauth.dao;

import userauth.model.User;

import java.util.List;

// File note: Interface DAO mô tả các thao tác truy cập dữ liệu của module này.
public interface UserDAO {
    void save(User user);
    void update(User user);
    User findByUsername(String username);
    User findByEmail(String email);
    List<User> findAll();
}
