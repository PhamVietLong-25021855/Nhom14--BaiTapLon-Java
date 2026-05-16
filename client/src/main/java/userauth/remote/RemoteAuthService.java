package userauth.client.remote;

import userauth.api.AuthApi;
import userauth.exception.UnauthorizedException;
import userauth.exception.ValidationException;
import userauth.model.Role;
import userauth.model.User;
import userauth.network.NetworkActions;

import java.util.List;

/** AuthService chạy ở client: mọi thao tác được chuyển sang Server qua Socket. */
public class RemoteAuthService implements AuthApi {
    private final RemoteAuctionClient client;

    public RemoteAuthService(RemoteAuctionClient client) {
        this.client = client;
    }

    @Override
    public void register(String username, String password, String fullName, String email, Role role) throws ValidationException {
        String result = (String) client.call(NetworkActions.AUTH_REGISTER,
                "username", username, "password", password, "fullName", fullName, "email", email, "role", role);
        if (!"SUCCESS".equals(result)) {
            throw new ValidationException(result);
        }
    }

    @Override
    public User login(String username, String password) throws UnauthorizedException {
        try {
            return (User) client.call(NetworkActions.AUTH_LOGIN, "username", username, "password", password);
        } catch (RemoteServerException ex) {
            throw new UnauthorizedException(ex.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<User> getAllUsers() {
        return (List<User>) client.call(NetworkActions.AUTH_ALL_USERS);
    }

    @Override
    public void changePassword(String username, String oldPassword, String newPassword)
            throws ValidationException, UnauthorizedException {
        String result = (String) client.call(NetworkActions.AUTH_CHANGE_PASSWORD,
                "username", username, "oldPassword", oldPassword, "newPassword", newPassword);
        if (!"SUCCESS".equals(result)) {
            throw new ValidationException(result);
        }
    }

    @Override
    public User updateProfile(String username, String fullName, String email)
            throws ValidationException, UnauthorizedException {
        try {
            return (User) client.call(NetworkActions.AUTH_UPDATE_PROFILE,
                    "username", username, "fullName", fullName, "email", email);
        } catch (RemoteServerException ex) {
            if ("UnauthorizedException".equals(ex.getErrorType())) {
                throw new UnauthorizedException(ex.getMessage());
            }
            throw new ValidationException(ex.getMessage());
        }
    }

    @Override
    public void toggleUserStatus(String adminUsername, int targetUserId)
            throws UnauthorizedException, ValidationException {
        String result = (String) client.call(NetworkActions.AUTH_TOGGLE_STATUS,
                "adminUsername", adminUsername, "targetUserId", targetUserId);
        if (!"SUCCESS".equals(result)) {
            throw new ValidationException(result);
        }
    }

    @Override
    public void deleteUserAccount(String adminUsername, int targetUserId)
            throws UnauthorizedException, ValidationException {
        String result = (String) client.call(NetworkActions.AUTH_DELETE_USER,
                "adminUsername", adminUsername, "targetUserId", targetUserId);
        if (!"SUCCESS".equals(result)) {
            throw new ValidationException(result);
        }
    }
}
