package userauth.controller;

import userauth.model.Role;
import userauth.model.User;
import userauth.service.AuthService;

import java.util.List;

public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public void register(String username, String email, String password, Role role) {
        try {
            User user = authService.register(username, email, password, role);
            System.out.println("Đăng ký thành công");
            System.out.println("Thông tin user: " + user);
        } catch (IllegalArgumentException e) {
            System.out.println("Đăng ký thất bại: " + e.getMessage());
        }
    }

    public User login(String username, String password) {
        try {
            User user = authService.login(username, password);
            System.out.println("Đăng nhập thành công");
            System.out.println("Xin chào " + user.getUsername() + " - Vai trò: " + user.getRole());
            return user;
        } catch (IllegalArgumentException e) {
            System.out.println("Đăng nhập thất bại: " + e.getMessage());
            return null;
        }
    }

    public void showAllUsers() {
        List<User> users = authService.getAllUsers();

        if (users.isEmpty()) {
            System.out.println("Danh sách user đang trống");
            return;
        }

        System.out.println("===== DANH SÁCH USER =====");
        for (User user : users) {
            System.out.println(user);
        }
    }

    public void checkRole(User user) {
        if (user == null) {
            System.out.println("Không có user để kiểm tra vai trò");
            return;
        }

        if (authService.isAdmin(user)) {
            System.out.println(user.getUsername() + " là ADMIN");
        } else if (authService.isSeller(user)) {
            System.out.println(user.getUsername() + " là SELLER");
        } else if (authService.isBidder(user)) {
            System.out.println(user.getUsername() + " là BIDDER");
        } else {
            System.out.println("Vai trò không xác định");
        }
    }
}