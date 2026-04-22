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

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

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

    public List<User> getAllUsers() {
        return userDAO.findAll();
    }

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

    public void toggleUserStatus(String adminUsername, int targetUserId)
            throws UnauthorizedException, ValidationException {
        User admin = requireExistingUser(adminUsername);
        if (admin.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Only admins can lock or unlock accounts.");
        }

        User target = findUserById(targetUserId);
        if (target == null) {
            throw new ValidationException("Account for lock or unlock was not found.");
        }
        if (target.getId() == admin.getId()) {
            throw new ValidationException("You cannot lock your own account.");
        }

        target.setStatus("ACTIVE".equals(target.getStatus()) ? "BLOCKED" : "ACTIVE");
        target.setUpdatedAt(System.currentTimeMillis());
        userDAO.update(target);
    }

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

    private void ensureUniqueUser(String username, String email) throws ValidationException {
        if (userDAO.findByUsername(username) != null) {
            throw new ValidationException("Username already exists.");
        }
        if (userDAO.findByEmail(email) != null) {
            throw new ValidationException("Email already exists.");
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
            throw new UnauthorizedException("User not found.");
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
