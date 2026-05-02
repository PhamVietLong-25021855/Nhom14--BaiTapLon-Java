package userauth.dao;

import userauth.model.User;

import java.util.List;

public interface UserDAO {
    void save(User user);
    void update(User user);
    User findByUsername(String username);
    User findByEmail(String email);
    List<User> findAll();
}