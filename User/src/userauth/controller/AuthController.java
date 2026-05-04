package userauth.controller;

import userauth.model.Role;
import userauth.model.User;
import userauth.service.AuthService;
import userauth.exception.UnauthorizedException;
import userauth.exception.ValidationException;

import java.util.List;

public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public String registerGUI(String username, String password, String fullName, String email, Role role) {
        try {
            authService.register(username, password, fullName, email, role);
            return "SUCCESS";
        } catch (ValidationException e) {
            return e.getMessage();
        }
    }

    public User login(String username, String password) throws UnauthorizedException {
        return authService.login(username, password); // Will throw UnauthorizedException if fails
    }

    public List<User> getAllUsersList() {
        return authService.getAllUsers();
    }

    public String changePassword(String username, String oldPassword, String newPassword) {
        try {
            authService.changePassword(username, oldPassword, newPassword);
            return "SUCCESS";
        } catch (ValidationException | UnauthorizedException e) {
            return e.getMessage();
        }
    }

    public String promoteUserToAdmin(String adminUsername, int targetUserId) {
        try {
            authService.promoteUserToAdmin(adminUsername, targetUserId);
            return "SUCCESS";
        } catch (ValidationException | UnauthorizedException e) {
            return e.getMessage();
        }
    }

    public String demoteAdminToBidder(String adminUsername, int targetUserId) {
        try {
            authService.demoteAdminToBidder(adminUsername, targetUserId);
            return "SUCCESS";
        } catch (ValidationException | UnauthorizedException e) {
            return e.getMessage();
        }
    }

    public String toggleUserStatus(String adminUsername, int targetUserId) {
        try {
            authService.toggleUserStatus(adminUsername, targetUserId);
            return "SUCCESS";
        } catch (ValidationException | UnauthorizedException e) {
            return e.getMessage();
        }
    }
}
