package userauth.service;

import java.util.List;
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

    public void register(String username, String password, String fullName, String email, Role role) throws ValidationException {
        String normalizedUsername = username == null ? null : username.trim();
        String normalizedEmail = email == null ? null : email.trim().toLowerCase();

        if (!UserValidator.isValidUsername(normalizedUsername)) throw new ValidationException("Username không hợp lệ. Phải từ 3-20 ký tự và không được để trống.");
        if (!UserValidator.isValidPassword(password)) throw new ValidationException("Mật khẩu không hợp lệ. Phải có ít nhất 6 ký tự, gồm chữ và số.");
        if (!UserValidator.isValidEmail(normalizedEmail)) throw new ValidationException("Email không hợp lệ.");
        if (role == null) throw new ValidationException("Role không hợp lệ.");
        if (fullName == null || fullName.trim().isEmpty()) throw new ValidationException("Họ tên không được để trống.");

        if (userDAO.findByUsername(normalizedUsername) != null) throw new ValidationException("Username đã tồn tại.");
        if (userDAO.findByEmail(normalizedEmail) != null) throw new ValidationException("Email đã tồn tại.");

        String hashedPassword = PasswordUtil.hashPassword(password);
        long now = System.currentTimeMillis();

        User user;
        switch (role) {
            case ADMIN:
                user = new Admin(idCounter++, normalizedUsername, hashedPassword, fullName, normalizedEmail, "ACTIVE", now, now);
                break;
            case SELLER:
                user = new Seller(idCounter++, normalizedUsername, hashedPassword, fullName, normalizedEmail, "ACTIVE", now, now);
                break;
            case BIDDER:
                user = new Bidder(idCounter++, normalizedUsername, hashedPassword, fullName, normalizedEmail, "ACTIVE", now, now);
                break;
            default:
                throw new ValidationException("Role không hợp lệ.");
        }

        userDAO.save(user);
    }

    public User login(String username, String password) throws UnauthorizedException {
        String normalizedUsername = username == null ? null : username.trim();

        if (normalizedUsername == null || normalizedUsername.isEmpty() || password == null || password.isBlank()) {
            throw new UnauthorizedException("Vui lòng nhập tài khoản và mật khẩu.");
        }

        User user = userDAO.findByUsername(normalizedUsername);

        if (user == null || !user.checkPassword(password)) {
            throw new UnauthorizedException("Sai tài khoản hoặc mật khẩu.");
        }

        if ("BLOCKED".equals(user.getStatus())) {
            throw new UnauthorizedException("Tài khoản của bạn đã bị khóa.");
        }

        return user;
    }

    public boolean isAdmin(User user) { return user instanceof Admin; }
    public boolean isSeller(User user) { return user instanceof Seller; }
    public boolean isBidder(User user) { return user instanceof Bidder; }

    public List<User> getAllUsers() {
        return userDAO.findAll();
    }

    public void changePassword(String username, String oldPassword, String newPassword) throws ValidationException, UnauthorizedException {
        User user = userDAO.findByUsername(username);
        if (user == null) throw new UnauthorizedException("Không tìm thấy người dùng.");
        if (!user.checkPassword(oldPassword)) throw new UnauthorizedException("Mật khẩu cũ không chính xác.");
        if (!UserValidator.isValidPassword(newPassword)) throw new ValidationException("Mật khẩu mới phải có ít nhất 6 ký tự gồm chữ và số.");
        
        user.setPassword(PasswordUtil.hashPassword(newPassword));
        user.setUpdatedAt(System.currentTimeMillis());
        userDAO.update(user);
    }

    public void updateProfile(String username, String fullName, String email) throws ValidationException {
        User user = userDAO.findByUsername(username);
        if (user == null) throw new ValidationException("Không tìm thấy người dùng.");

        if (fullName == null || fullName.trim().isEmpty()) throw new ValidationException("Họ tên không được để trống.");
        if (!UserValidator.isValidEmail(email)) throw new ValidationException("Email không hợp lệ.");

        user.setFullName(fullName);
        user.setEmail(email);
        user.setUpdatedAt(System.currentTimeMillis());
        userDAO.update(user);
    }

    public void toggleUserStatus(String adminUsername, int targetUserId) throws UnauthorizedException, ValidationException {
        User admin = userDAO.findByUsername(adminUsername);
        if (admin == null || !isAdmin(admin)) {
            throw new UnauthorizedException("Chỉ Admin mới có quyền khóa/mở tài khoản.");
        }

        User target = null;
        for (User u : userDAO.findAll()) {
            if (u.getId() == targetUserId) {
                target = u;
                break;
            }
        }

        if (target == null) throw new ValidationException("Không tìm thấy tài khoản để khóa/mở.");
        if (target.getId() == admin.getId()) throw new ValidationException("Không thể tự khóa tài khoản của chính mình.");

        target.setStatus("ACTIVE".equals(target.getStatus()) ? "BLOCKED" : "ACTIVE");
        target.setUpdatedAt(System.currentTimeMillis());
        userDAO.update(target);
    }
}