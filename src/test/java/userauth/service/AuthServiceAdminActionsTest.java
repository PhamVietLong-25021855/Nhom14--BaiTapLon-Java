package userauth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import userauth.exception.UnauthorizedException;
import userauth.exception.ValidationException;
import userauth.model.Role;
import userauth.model.User;
import userauth.support.TestDaos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthServiceAdminActionsTest {
    private TestDaos.InMemoryUserDao userDao;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userDao = new TestDaos.InMemoryUserDao();
        authService = new AuthService(userDao);
    }

    @Test
    void registerNormalizesUsernameAndEmailThenRejectsDuplicateEmail() throws Exception {
        authService.register("  bidder01  ", "abc123", "Bidder One", "  Bidder@Example.COM  ", Role.BIDDER);

        User saved = userDao.findByUsername("bidder01");

        assertNotNull(saved);
        assertEquals("bidder@example.com", saved.getEmail());
        assertThrows(ValidationException.class,
                () -> authService.register("bidder02", "abc123", "Bidder Two", "BIDDER@example.com", Role.BIDDER));
    }

    @Test
    void adminCanBlockAndUnblockUser() throws Exception {
        authService.register("admin01", "abc123", "Admin One", "admin01@example.com", Role.ADMIN);
        authService.register("bidder01", "abc123", "Bidder One", "bidder01@example.com", Role.BIDDER);
        int targetId = userDao.findByUsername("bidder01").getId();

        authService.toggleUserStatus("admin01", targetId);

        assertEquals("BLOCKED", userDao.findByUsername("bidder01").getStatus());
        assertThrows(UnauthorizedException.class, () -> authService.login("bidder01", "abc123"));

        authService.toggleUserStatus("admin01", targetId);

        assertEquals("ACTIVE", userDao.findByUsername("bidder01").getStatus());
        assertEquals("bidder01", authService.login("bidder01", "abc123").getUsername());
    }

    @Test
    void nonAdminCannotToggleOrDeleteAccounts() throws Exception {
        authService.register("seller01", "abc123", "Seller One", "seller01@example.com", Role.SELLER);
        authService.register("bidder01", "abc123", "Bidder One", "bidder01@example.com", Role.BIDDER);
        int targetId = userDao.findByUsername("bidder01").getId();

        assertThrows(UnauthorizedException.class, () -> authService.toggleUserStatus("seller01", targetId));
        assertThrows(UnauthorizedException.class, () -> authService.deleteUserAccount("seller01", targetId));
    }

    @Test
    void changePasswordInvalidatesOldPassword() throws Exception {
        authService.register("bidder01", "abc123", "Bidder One", "bidder01@example.com", Role.BIDDER);

        authService.changePassword("bidder01", "abc123", "new123");

        assertThrows(UnauthorizedException.class, () -> authService.login("bidder01", "abc123"));
        assertEquals("bidder01", authService.login("bidder01", "new123").getUsername());
    }
}
