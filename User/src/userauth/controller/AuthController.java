package userauth.controller;

import userauth.client.network.RemoteApiClient;
import userauth.common.RemoteAction;
import userauth.common.RemoteResponse;
import userauth.exception.UnauthorizedException;
import userauth.exception.ValidationException;
import userauth.model.Role;
import userauth.model.User;
import userauth.service.AuthService;

import java.util.List;

public class AuthController {
    private final AuthService authService;
    private final RemoteApiClient remoteApiClient;

    public AuthController(AuthService authService) {
        this.authService = authService;
        this.remoteApiClient = null;
    }

    public AuthController(RemoteApiClient remoteApiClient) {
        this.authService = null;
        this.remoteApiClient = remoteApiClient;
    }

    public String registerGUI(String username, String password, String fullName, String email, Role role) {
        if (remoteApiClient != null) {
            return requestString(RemoteAction.AUTH_REGISTER_GUI, username, password, fullName, email, role);
        }

        try {
            authService.register(username, password, fullName, email, role);
            return "SUCCESS";
        } catch (ValidationException e) {
            return e.getMessage();
        }
    }

    public User login(String username, String password) throws UnauthorizedException {
        if (remoteApiClient != null) {
            RemoteResponse response = remoteApiClient.send(RemoteAction.AUTH_LOGIN, username, password);
            if (!response.isSuccess()) {
                throw new UnauthorizedException(response.getMessage());
            }
            return response.payloadAs(User.class);
        }

        return authService.login(username, password);
    }

    @SuppressWarnings("unchecked")
    public List<User> getAllUsersList() {
        if (remoteApiClient != null) {
            try {
                RemoteResponse response = remoteApiClient.send(RemoteAction.AUTH_GET_ALL_USERS);
                if (!response.isSuccess()) {
                    return List.of();
                }
                Object payload = response.getPayload();
                return payload instanceof List<?> users ? (List<User>) users : List.of();
            } catch (RuntimeException ex) {
                return List.of();
            }
        }

        return authService.getAllUsers();
    }

    public String changePassword(String username, String oldPassword, String newPassword) {
        if (remoteApiClient != null) {
            return requestString(RemoteAction.AUTH_CHANGE_PASSWORD, username, oldPassword, newPassword);
        }

        try {
            authService.changePassword(username, oldPassword, newPassword);
            return "SUCCESS";
        } catch (ValidationException | UnauthorizedException e) {
            return e.getMessage();
        }
    }

    public String toggleUserStatus(String adminUsername, int targetUserId) {
        if (remoteApiClient != null) {
            return requestString(RemoteAction.AUTH_TOGGLE_USER_STATUS, adminUsername, targetUserId);
        }

        try {
            authService.toggleUserStatus(adminUsername, targetUserId);
            return "SUCCESS";
        } catch (ValidationException | UnauthorizedException e) {
            return e.getMessage();
        }
    }

    public String deleteUser(String adminUsername, int targetUserId) {
        if (remoteApiClient != null) {
            return requestString(RemoteAction.AUTH_DELETE_USER, adminUsername, targetUserId);
        }

        try {
            authService.deleteUser(adminUsername, targetUserId);
            return "SUCCESS";
        } catch (ValidationException | UnauthorizedException | IllegalStateException e) {
            return e.getMessage();
        }
    }

    private String requestString(RemoteAction action, Object... arguments) {
        try {
            RemoteResponse response = remoteApiClient.send(action, arguments);
            if (!response.isSuccess()) {
                return response.getMessage();
            }
            String payload = response.payloadAsString();
            return payload == null || payload.isBlank() ? "SUCCESS" : payload;
        } catch (RuntimeException ex) {
            return ex.getMessage();
        }
    }
}
