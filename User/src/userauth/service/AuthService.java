package userauth.service;

import userauth.dao.UserDAO;
import userauth.model.Role;
import userauth.model.User;
import userauth.validation.UserValidator;

import java.util.List;

public class AuthService {
    private final UserDAO userDAO;

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public User register(String username, String email, String password, Role role) {
        UserValidator.validateRegister(username, email, password, role);

        if (userDAO.findByUsername(username) != null) {
            throw new IllegalArgumentException("Username đã tồn tại");
        }

        if (userDAO.findByEmail(email) != null) {
            throw new IllegalArgumentException("Email đã tồn tại");
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setEmail(email.trim());
        user.setPassword(password);
        user.setRole(role);

        userDAO.save(user);
        return user;
    }

    public User login(String username, String password) {
        UserValidator.validateLogin(username, password);

        User user = userDAO.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("Tài khoản không tồn tại");
        }

        if (!user.checkPassword(password)) {
            throw new IllegalArgumentException("Sai mật khẩu");
        }

        return user;
    }

    public List<User> getAllUsers() {
        return userDAO.findAll();
    }

    public boolean isAdmin(User user) {
        return user.getRole() == Role.ADMIN;
    }

    public boolean isSeller(User user) {
        return user.getRole() == Role.SELLER;
    }

    public boolean isBidder(User user) {
        return user.getRole() == Role.BIDDER;
    }
}