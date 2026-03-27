package userauth.service;

import java.util.List;
import userauth.dao.UserDAO;
import userauth.model.Admin;
import userauth.model.Bidder;
import userauth.model.Role;
import userauth.model.Seller;
import userauth.model.User;
import userauth.util.PasswordUtil;
import userauth.validation.UserValidator;

public class AuthService {
    private final UserDAO userDAO;
    private int idCounter;

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
        this.idCounter = getNextId();
    }

    private int getNextId() {
        int maxId = 0;
        for (User user : userDAO.findAll()) {
            if (user.getId() > maxId) {
                maxId = user.getId();
            }
        }
        return maxId + 1;
    }

    public String register(String username, String password, String email, Role role) {
        String normalizedUsername = username == null ? null : username.trim();
        String normalizedEmail = email == null ? null : email.trim().toLowerCase();

        if (!UserValidator.isValidUsername(normalizedUsername)) {
            return "Username không hợp lệ. Phải từ 3-20 ký tự và không được để trống.";
        }

        if (!UserValidator.isValidPassword(password)) {
            return "Mật khẩu không hợp lệ. Phải có ít nhất 6 ký tự, gồm chữ và số.";
        }

        if (!UserValidator.isValidEmail(normalizedEmail)) {
            return "Email không hợp lệ.";
        }

        if (role == null) {
            return "Role không hợp lệ.";
        }

        if (userDAO.findByUsername(normalizedUsername) != null) {
            return "Username đã tồn tại.";
        }

        if (userDAO.findByEmail(normalizedEmail) != null) {
            return "Email đã tồn tại.";
        }

        String hashedPassword = PasswordUtil.hashPassword(password);

        User user;
        switch (role) {
            case ADMIN:
                user = new Admin(idCounter++, normalizedUsername, hashedPassword, normalizedEmail);
                break;
            case SELLER:
                user = new Seller(idCounter++, normalizedUsername, hashedPassword, normalizedEmail);
                break;
            case BIDDER:
                user = new Bidder(idCounter++, normalizedUsername, hashedPassword, normalizedEmail);
                break;
            default:
                return "Role không hợp lệ.";
        }

        userDAO.save(user);
        return "Đăng ký thành công.";
    }

    public User login(String username, String password) {
        String normalizedUsername = username == null ? null : username.trim();

        if (normalizedUsername == null || normalizedUsername.isEmpty()) {
            return null;
        }

        if (password == null || password.isBlank()) {
            return null;
        }

        User user = userDAO.findByUsername(normalizedUsername);

        if (user == null) {
            return null;
        }

        return user.checkPassword(password) ? user : null;
    }

    public boolean isAdmin(User user) {
        return user instanceof Admin;
    }

    public boolean isSeller(User user) {
        return user instanceof Seller;
    }

    public boolean isBidder(User user) {
        return user instanceof Bidder;
    }

    public List<User> getAllUsers() {
        return userDAO.findAll();
    }
}