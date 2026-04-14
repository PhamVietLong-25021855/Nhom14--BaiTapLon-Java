package userauth.dao;

import userauth.model.User;
import userauth.service.FileService;

import java.util.ArrayList;
import java.util.List;

public class UserDAOImpl implements UserDAO {
    private final List<User> users;
    private final FileService fileService;

    public UserDAOImpl() {
        this.fileService = new FileService();
        this.users = new ArrayList<>(fileService.loadUsersFromFile());
    }

    @Override
    public void save(User user) {
        users.add(user);
        fileService.saveUsersToFile(users);
    }

    @Override
    public void update(User user) {
        fileService.saveUsersToFile(users); // The reference object is modified, just flush list.
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
        return new ArrayList<>(users);
    }
}