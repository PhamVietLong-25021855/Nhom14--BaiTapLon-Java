package userauth.dao;

import userauth.model.User;

import java.util.List;

public interface UserDAO {
    void save(User user);
    void update(User user);
    void delete(int userId);
    User findById(int userId);
    User findByUsername(String username);
    User findByEmail(String email);
    List<User> findAll();
}
