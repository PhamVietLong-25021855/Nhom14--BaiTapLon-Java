package userauth.server;

import userauth.exception.UnauthorizedException;
import userauth.model.Role;
import userauth.model.User;
import userauth.network.AuthenticatedUserResponse;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class AuctionSessionManager {
    private static final long DEFAULT_SESSION_DURATION_MS = Duration.ofHours(8).toMillis();
    private static final int MAX_SESSIONS_PER_USER = 5;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final AtomicLong sessionSequence = new AtomicLong();
    private final long sessionDurationMs;

    public AuctionSessionManager() {
        this(DEFAULT_SESSION_DURATION_MS);
    }

    AuctionSessionManager(long sessionDurationMs) {
        if (sessionDurationMs <= 0) {
            throw new IllegalArgumentException("Session duration must be greater than 0.");
        }
        this.sessionDurationMs = sessionDurationMs;
    }

    public synchronized AuthenticatedUserResponse create(User user) {
        if (user == null || user.getRole() == null) {
            throw new IllegalArgumentException("Cannot create a session without an authenticated user.");
        }
        removeExpiredSessions();
        removeOldestSessionsForUser(user.getId());
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        long expiresAt = System.currentTimeMillis() + sessionDurationMs;
        sessions.put(token, new Session(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                expiresAt,
                sessionSequence.incrementAndGet()
        ));
        return new AuthenticatedUserResponse(user, token, expiresAt);
    }

    public Session require(String token) throws UnauthorizedException {
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("Authentication is required.");
        }
        Session session = sessions.get(token);
        if (session == null) {
            throw new UnauthorizedException("The login session is invalid. Please log in again.");
        }
        if (session.expiresAt() <= System.currentTimeMillis()) {
            sessions.remove(token, session);
            throw new UnauthorizedException("The login session has expired. Please log in again.");
        }
        return session;
    }

    public void invalidate(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    public void invalidateUser(int userId) {
        sessions.entrySet().removeIf(entry -> entry.getValue().userId() == userId);
    }

    int activeSessionCount() {
        return sessions.size();
    }

    private void removeExpiredSessions() {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
    }

    private void removeOldestSessionsForUser(int userId) {
        while (sessions.values().stream().filter(session -> session.userId() == userId).count() >= MAX_SESSIONS_PER_USER) {
            sessions.entrySet().stream()
                    .filter(entry -> entry.getValue().userId() == userId)
                    .min(Map.Entry.comparingByValue((left, right) -> Long.compare(left.sequence(), right.sequence())))
                    .ifPresent(entry -> sessions.remove(entry.getKey(), entry.getValue()));
        }
    }

    public record Session(int userId, String username, Role role, long expiresAt, long sequence) {
    }
}
