package userauth.api;

import userauth.exception.UnauthorizedException;
import userauth.exception.ValidationException;
import userauth.model.Role;
import userauth.model.User;

import java.util.List;

public interface AuthApi {
    void register(String username, String password, String fullName, String email, Role role) throws ValidationException;
    User login(String username, String password) throws UnauthorizedException;
    List<User> getAllUsers();
    void changePassword(String username, String oldPassword, String newPassword) throws ValidationException, UnauthorizedException;
    User updateProfile(String username, String fullName, String email) throws ValidationException, UnauthorizedException;
    void toggleUserStatus(String adminUsername, int targetUserId) throws UnauthorizedException, ValidationException;
    void deleteUserAccount(String adminUsername, int targetUserId) throws UnauthorizedException, ValidationException;
}
