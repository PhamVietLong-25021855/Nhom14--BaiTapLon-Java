package userauth.controller;

import userauth.api.AuthApi;
import userauth.exception.UnauthorizedException;
import userauth.exception.ValidationException;
import userauth.model.Role;
import userauth.model.User;

import java.util.List;

public class AuthController {
    private final AuthApi authService;

    public AuthController(AuthApi authService) {
        this.authService = authService;
    }

    public String registerGUI(String username, String password, String fullName, String email, Role role) {
        if (role == Role.ADMIN) {
            return "Admin accounts cannot be created from the registration screen.";
        }
        try {
            authService.register(username, password, fullName, email, role);
            return "SUCCESS";
        } catch (ValidationException e) {
            return e.getMessage();
        }
    }

    public User login(String username, String password) {
        try {
            return authService.login(username, password);
        } catch (UnauthorizedException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public User getUserById(int userId) {
        try {
            return authService.getUserById(userId);
        } catch (UnauthorizedException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
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

    public Object updateProfile(String username, String fullName, String email) {
        try {
            return authService.updateProfile(username, fullName, email);
        } catch (ValidationException | UnauthorizedException e) {
            return e.getMessage();
        }
    }

    public String updateProfileGUI(User currentUser, String fullName, String email) {
        if (currentUser == null) {
            return "Current user information is unavailable.";
        }
        try {
            User updatedUser = authService.updateProfile(currentUser.getUsername(), fullName, email);
            currentUser.setFullName(updatedUser.getFullName());
            currentUser.setEmail(updatedUser.getEmail());
            currentUser.setUpdatedAt(updatedUser.getUpdatedAt());
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

    public String deleteUserAccount(String adminUsername, int targetUserId) {
        try {
            authService.deleteUserAccount(adminUsername, targetUserId);
            return "SUCCESS";
        } catch (ValidationException | UnauthorizedException e) {
            return e.getMessage();
        }
    }
}
