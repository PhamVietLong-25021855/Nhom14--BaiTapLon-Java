package userauth.server;

import org.junit.jupiter.api.Test;
import userauth.exception.UnauthorizedException;
import userauth.model.Bidder;
import userauth.network.AuthenticatedUserResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuctionSessionManagerTest {
    @Test
    void createdSessionAuthenticatesUntilInvalidated() throws Exception {
        AuctionSessionManager sessions = new AuctionSessionManager();
        AuthenticatedUserResponse first = sessions.create(user(7));
        AuthenticatedUserResponse second = sessions.create(user(7));

        assertNotEquals(first.sessionToken(), second.sessionToken());
        assertEquals(7, sessions.require(first.sessionToken()).userId());

        sessions.invalidate(first.sessionToken());

        assertThrows(UnauthorizedException.class, () -> sessions.require(first.sessionToken()));
    }

    @Test
    void invalidatingUserRevokesAllTheirSessions() {
        AuctionSessionManager sessions = new AuctionSessionManager();
        AuthenticatedUserResponse first = sessions.create(user(7));
        AuthenticatedUserResponse second = sessions.create(user(7));

        sessions.invalidateUser(7);

        assertThrows(UnauthorizedException.class, () -> sessions.require(first.sessionToken()));
        assertThrows(UnauthorizedException.class, () -> sessions.require(second.sessionToken()));
    }

    @Test
    void expiredSessionIsRejected() throws Exception {
        AuctionSessionManager sessions = new AuctionSessionManager(1);
        AuthenticatedUserResponse response = sessions.create(user(7));

        Thread.sleep(5);

        assertThrows(UnauthorizedException.class, () -> sessions.require(response.sessionToken()));
    }

    @Test
    void creatingSessionPurgesExpiredSessions() throws Exception {
        AuctionSessionManager sessions = new AuctionSessionManager(1);
        sessions.create(user(7));
        Thread.sleep(5);

        sessions.create(user(8));

        assertEquals(1, sessions.activeSessionCount());
    }

    @Test
    void onlyFiveRecentSessionsAreKeptPerUser() {
        AuctionSessionManager sessions = new AuctionSessionManager();
        AuthenticatedUserResponse oldest = sessions.create(user(7));
        for (int i = 0; i < 5; i++) {
            sessions.create(user(7));
        }

        assertEquals(5, sessions.activeSessionCount());
        assertThrows(UnauthorizedException.class, () -> sessions.require(oldest.sessionToken()));
    }

    private Bidder user(int id) {
        return new Bidder(id, "bidder" + id, "hash", "Bidder", "bidder@example.com", "ACTIVE", 1L, 1L);
    }
}
