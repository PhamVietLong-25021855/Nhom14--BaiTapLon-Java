package userauth.service;

import userauth.dao.UserDAO;
import userauth.exception.UnauthorizedException;
import userauth.exception.ValidationException;
import userauth.model.Admin;
import userauth.model.Bidder;
import userauth.model.Role;
import userauth.model.Seller;
import userauth.model.User;
import userauth.util.PasswordUtil;
import userauth.validation.UserValidator;

import java.util.List;

public class AuthService {
    private final UserDAO userDAO;
    private int idCounter;

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
        this.idCounter = findNextId();
    }

    public void register(String username, String password, String fullName, String email, Role role) throws ValidationException {
        String normalizedUsername = username == null ? null : username.trim();
        String normalizedEmail = email == null ? null : email.trim().toLowerCase();

        validateRegistrationInput(normalizedUsername, password, fullName, normalizedEmail, role);
        ensureUniqueUser(normalizedUsername, normalizedEmail);

        long now = System.currentTimeMillis();
        String hashedPassword = PasswordUtil.hashPassword(password);
        User user = createUser(idCounter++, normalizedUsername, hashedPassword, fullName, normalizedEmail, role, now);
        userDAO.save(user);
    }

    public User login(String username, String password) throws UnauthorizedException {
        String normalizedUsername = username == null ? null : username.trim();
        if (normalizedUsername == null || normalizedUsername.isEmpty() || password == null || password.isBlank()) {
            throw new UnauthorizedException("Vui long nhap tai khoan va mat khau.");
        }

        User user = userDAO.findByUsername(normalizedUsername);
        if (user == null || !user.checkPassword(password)) {
            throw new UnauthorizedException("Sai tai khoan hoac mat khau.");
        }
        if ("BLOCKED".equals(user.getStatus())) {
            throw new UnauthorizedException("Tai khoan cua ban da bi khoa.");
        }

        return user;
    }

    public List<User> getAllUsers() {
        return userDAO.findAll();
    }

    public void changePassword(String username, String oldPassword, String newPassword)
            throws ValidationException, UnauthorizedException {
        User user = requireExistingUser(username);
        if (!user.checkPassword(oldPassword)) {
            throw new UnauthorizedException("Mat khau cu khong chinh xac.");
        }
        if (!UserValidator.isValidPassword(newPassword)) {
            throw new ValidationException("Mat khau moi phai co it nhat 6 ky tu gom chu va so.");
        }

        user.setPassword(PasswordUtil.hashPassword(newPassword));
        user.setUpdatedAt(System.currentTimeMillis());
        userDAO.update(user);
    }

    public void toggleUserStatus(String adminUsername, int targetUserId)
            throws UnauthorizedException, ValidationException {
        User admin = requireExistingUser(adminUsername);
        if (admin.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Chi admin moi co quyen khoa/mo tai khoan.");
        }

        User target = findUserById(targetUserId);
        if (target == null) {
            throw new ValidationException("Khong tim thay tai khoan de khoa/mo.");
        }
        if (target.getId() == admin.getId()) {
            throw new ValidationException("Khong the tu khoa tai khoan cua chinh minh.");
        }

        target.setStatus("ACTIVE".equals(target.getStatus()) ? "BLOCKED" : "ACTIVE");
        target.setUpdatedAt(System.currentTimeMillis());
        userDAO.update(target);
    }

    private int findNextId() {
        int maxId = 0;
        for (User user : userDAO.findAll()) {
            if (user.getId() > maxId) {
                maxId = user.getId();
            }
        }
        return maxId + 1;
    }

    private void validateRegistrationInput(String username, String password, String fullName, String email, Role role)
            throws ValidationException {
        if (!UserValidator.isValidUsername(username)) {
            throw new ValidationException("Username khong hop le. Phai tu 3-20 ky tu va khong duoc de trong.");
        }
        if (!UserValidator.isValidPassword(password)) {
            throw new ValidationException("Mat khau khong hop le. Phai co it nhat 6 ky tu, gom chu va so.");
        }
        if (!UserValidator.isValidEmail(email)) {
            throw new ValidationException("Email khong hop le.");
        }
        if (role == null) {
            throw new ValidationException("Role khong hop le.");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new ValidationException("Ho ten khong duoc de trong.");
        }
    }

    private void ensureUniqueUser(String username, String email) throws ValidationException {
        if (userDAO.findByUsername(username) != null) {
            throw new ValidationException("Username da ton tai.");
        }
        if (userDAO.findByEmail(email) != null) {
            throw new ValidationException("Email da ton tai.");
        }
    }

    private User createUser(int id, String username, String hashedPassword, String fullName, String email, Role role, long timestamp) {
        return switch (role) {
            case ADMIN -> new Admin(id, username, hashedPassword, fullName, email, "ACTIVE", timestamp, timestamp);
            case SELLER -> new Seller(id, username, hashedPassword, fullName, email, "ACTIVE", timestamp, timestamp);
            case BIDDER -> new Bidder(id, username, hashedPassword, fullName, email, "ACTIVE", timestamp, timestamp);
        };
    }

    private User requireExistingUser(String username) throws UnauthorizedException {
        User user = userDAO.findByUsername(username);
        if (user == null) {
            throw new UnauthorizedException("Khong tim thay nguoi dung.");
        }
        return user;
    }

    private User findUserById(int userId) {
        for (User user : userDAO.findAll()) {
            if (user.getId() == userId) {
                return user;
            }
        }
        return null;
    }
}
