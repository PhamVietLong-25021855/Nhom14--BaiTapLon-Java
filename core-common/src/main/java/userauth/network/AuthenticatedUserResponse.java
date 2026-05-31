package userauth.network;

import userauth.model.User;

import java.io.Serializable;

public record AuthenticatedUserResponse(User user, String sessionToken, long expiresAt) implements Serializable {
    private static final long serialVersionUID = 1L;
}
