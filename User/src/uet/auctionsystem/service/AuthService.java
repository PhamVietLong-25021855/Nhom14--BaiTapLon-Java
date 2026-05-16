package uet.auctionsystem.service;

import uet.auctionsystem.dao.UserDAO;
import uet.auctionsystem.exception.UnauthorizedException;
import uet.auctionsystem.exception.ValidationException;
import uet.auctionsystem.model.Bidder;
import uet.auctionsystem.model.Role;
import uet.auctionsystem.model.Seller;
import uet.auctionsystem.model.User;
import uet.auctionsystem.util.PasswordUtil;
import uet.auctionsystem.validation.UserValidator;
import java.util.List;

// Ghi chu file: File service; chua nghiep vu chinh va phoi hop giua controller, DAO va event.
// Khai bao lop AuthService; chua xu ly nghiep vu va cac quy tac chinh cua he thong.
public class AuthService {
    // Thuoc tinh: giu tham chieu den UserDAO de phoi hop xu ly.
    private final UserDAO userDAO;
    // Ham tao: khoi tao doi tuong AuthService voi cac phu thuoc can thiet.
    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac register.
    public void register(String username, String password, String fullName, String email, Role role) throws ValidationException {
        String normalizedUsername = username == null ? null : username.trim();
        String normalizedEmail = email == null ? null : email.trim().toLowerCase();

        validateRegistrationInput(normalizedUsername, password, fullName, normalizedEmail, role);
        ensureUniqueUser(normalizedUsername, normalizedEmail);

        long now = System.currentTimeMillis();
        String hashedPassword = PasswordUtil.hashPassword(password);
        User user = createUser(0, normalizedUsername, hashedPassword, fullName, normalizedEmail, role, now);
        userDAO.save(user);
    }
    // Phuong thuc: thuc hien chuc nang login trong lop AuthService.
    public User login(String username, String password) throws UnauthorizedException {
        String normalizedUsername = username == null ? null : username.trim();
        if (normalizedUsername == null || normalizedUsername.isEmpty() || password == null || password.isBlank()) {
            throw new UnauthorizedException("Please enter your username and password.");
        }

        User user = userDAO.findByUsername(normalizedUsername);
        if (user == null || !user.checkPassword(password)) {
            throw new UnauthorizedException("Incorrect username or password.");
        }
        if ("BLOCKED".equals(user.getStatus())) {
            throw new UnauthorizedException("Your account has been locked.");
        }

        return user;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get all users.
    public List<User> getAllUsers() {
        return userDAO.findAll();
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac change password.
    public void changePassword(String username, String oldPassword, String newPassword)
            throws ValidationException, UnauthorizedException {
        User user = requireExistingUser(username);
        if (!user.checkPassword(oldPassword)) {
            throw new UnauthorizedException("Current password is incorrect.");
        }
        if (!UserValidator.isValidPassword(newPassword)) {
            throw new ValidationException("New password must be at least 6 characters and include letters and numbers.");
        }

        user.setPassword(PasswordUtil.hashPassword(newPassword));
        user.setUpdatedAt(System.currentTimeMillis());
        userDAO.update(user);
    }
    // Phuong thuc: kiem tra dieu kien hoac xac thuc cho thao tac validate registration input.
    private void validateRegistrationInput(String username, String password, String fullName, String email, Role role)
            throws ValidationException {
        if (!UserValidator.isValidUsername(username)) {
            throw new ValidationException("Invalid username. It must be 3 to 20 characters long and cannot be empty.");
        }
        if (!UserValidator.isValidPassword(password)) {
            throw new ValidationException("Invalid password. It must be at least 6 characters long and include letters and numbers.");
        }
        if (!UserValidator.isValidEmail(email)) {
            throw new ValidationException("Invalid email.");
        }
        if (role == null) {
            throw new ValidationException("Invalid role.");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new ValidationException("Full name cannot be empty.");
        }
    }
    // Phuong thuc: kiem tra dieu kien hoac xac thuc cho thao tac ensure unique user.
    private void ensureUniqueUser(String username, String email) throws ValidationException {
        if (userDAO.findByUsername(username) != null) {
            throw new ValidationException("Username already exists.");
        }
        if (userDAO.findByEmail(email) != null) {
            throw new ValidationException("Email already exists.");
        }
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac create user.
    private User createUser(int id, String username, String hashedPassword, String fullName, String email, Role role, long timestamp) {
        return switch (role) {
            case SELLER -> new Seller(id, username, hashedPassword, fullName, email, "ACTIVE", timestamp, timestamp);
            case BIDDER -> new Bidder(id, username, hashedPassword, fullName, email, "ACTIVE", timestamp, timestamp);
        };
    }
    // Phuong thuc: thuc hien chuc nang require existing user trong lop AuthService.
    private User requireExistingUser(String username) throws UnauthorizedException {
        User user = userDAO.findByUsername(username);
        if (user == null) {
            throw new UnauthorizedException("User not found.");
        }
        return user;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac find user by id.
    private User findUserById(int userId) {
        for (User user : userDAO.findAll()) {
            if (user.getId() == userId) {
                return user;
            }
        }
        return null;
    }
}
