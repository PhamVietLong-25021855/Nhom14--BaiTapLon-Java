package userauth.dao;

import userauth.model.User;
import java.util.ArrayList;
import java.util.List;

public class UserDAOImpl implements UserDAO {
    private final List<User> users;
    private int autoId;

    public UserDAOImpl() {
        users = new ArrayList<>();
        autoId = 1;
    }

    @Override
    public void save(User user) {
        user.setId(autoId);
        autoId++;
        users.add(user);
    }

    @Override
    public User findByUsername(String username) {
        for (User user : users) {
            if (user.getUsername().equalsIgnoreCase(username)) {
                return user;
            }
        }
        return null;
    }

    @Override
    public User findByEmail(String email) {
        for (User user : users) {
            if (user.getEmail().equalsIgnoreCase(email)) {
                return user;
            }
        }
        return null;
    }

    @Override
    public List<User> findAll() {
        return users;
    }
}