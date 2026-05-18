package userauth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import userauth.dao.UserDAO;
import userauth.exception.UnauthorizedException;
import userauth.exception.ValidationException;
import userauth.model.Role;
import userauth.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {
    private InMemoryUserDao userDao;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userDao = new InMemoryUserDao();
        authService = new AuthService(userDao);
    }

    @Test
    void registerValidUserThenLoginReturnsUser() throws Exception {
        authService.register("bidder01", "abc123", "Bidder One", "bidder01@example.com", Role.BIDDER);

        User loggedIn = authService.login("bidder01", "abc123");

        assertEquals("bidder01", loggedIn.getUsername());
        assertEquals(Role.BIDDER, loggedIn.getRole());
    }

    @Test
    void registerNormalizesUsernameAndEmailAndStoresHashedPassword() throws Exception {
        authService.register("  bidder02  ", "abc123", "Bidder Two", "  Bidder02@Example.COM  ", Role.BIDDER);

        User saved = userDao.findByUsername("bidder02");

        assertNotNull(saved);
        assertEquals("bidder02", saved.getUsername());
        assertEquals("bidder02@example.com", saved.getEmail());
        assertNotEquals("abc123", saved.getPassword());
        assertTrue(saved.checkPassword("abc123"));
        assertEquals("bidder02", authService.login("  bidder02  ", "abc123").getUsername());
    }

    @Test
    void registerDuplicateUsernameThrowsValidationException() throws Exception {
        authService.register("seller01", "abc123", "Seller One", "seller01@example.com", Role.SELLER);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> authService.register("seller01", "abc123", "Seller Other", "seller02@example.com", Role.SELLER));

        assertTrue(ex.getMessage().toLowerCase().contains("username"));
    }

    @Test
    void registerDuplicateEmailThrowsValidationExceptionIgnoringCaseAndWhitespace() throws Exception {
        authService.register("seller01", "abc123", "Seller One", "Seller01@Example.COM", Role.SELLER);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> authService.register("seller02", "abc123", "Seller Two", "  seller01@example.com  ", Role.SELLER));

        assertTrue(ex.getMessage().toLowerCase().contains("email"));
        assertEquals(1, userDao.findAll().size());
    }

    @Test
    void loginWrongPasswordThrowsUnauthorizedException() throws Exception {
        authService.register("admin01", "abc123", "Admin One", "admin01@example.com", Role.ADMIN);

        assertThrows(UnauthorizedException.class, () -> authService.login("admin01", "wrong123"));
    }

    @Test
    void loginBlockedUserThrowsUnauthorizedException() throws Exception {
        authService.register("bidder03", "abc123", "Bidder Three", "bidder03@example.com", Role.BIDDER);
        userDao.findByUsername("bidder03").setStatus("BLOCKED");

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.login("bidder03", "abc123"));

        assertTrue(ex.getMessage().toLowerCase().contains("locked"));
    }

    @Test
    void changePasswordRejectsInvalidNewPasswordAndKeepsOldPassword() throws Exception {
        authService.register("bidder04", "abc123", "Bidder Four", "bidder04@example.com", Role.BIDDER);

        assertThrows(ValidationException.class,
                () -> authService.changePassword("bidder04", "abc123", "abcdef"));

        assertEquals("bidder04", authService.login("bidder04", "abc123").getUsername());
        assertThrows(UnauthorizedException.class, () -> authService.login("bidder04", "abcdef"));
    }

    @Test
    void updateProfileRejectsEmailOwnedByAnotherUser() throws Exception {
        authService.register("seller03", "abc123", "Seller Three", "seller03@example.com", Role.SELLER);
        authService.register("bidder05", "abc123", "Bidder Five", "bidder05@example.com", Role.BIDDER);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> authService.updateProfile("bidder05", "Bidder Updated", "  seller03@example.com  "));

        assertTrue(ex.getMessage().toLowerCase().contains("email"));
        assertEquals("bidder05@example.com", userDao.findByUsername("bidder05").getEmail());
    }

    @Test
    void updateProfileRejectsAdminAccount() throws Exception {
        authService.register("admin02", "abc123", "Admin Two", "admin02@example.com", Role.ADMIN);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> authService.updateProfile("admin02", "Admin Updated", "admin.updated@example.com"));

        assertTrue(ex.getMessage().toLowerCase().contains("bidders and sellers"));
    }

    private static final class InMemoryUserDao implements UserDAO {
        private final AtomicInteger ids = new AtomicInteger(1);
        private final List<User> users = new ArrayList<>();

        @Override
        public void save(User user) {
            user.setId(ids.getAndIncrement());
            users.add(user);
        }

        @Override
        public void update(User user) {
            deleteById(user.getId());
            users.add(user);
        }

        @Override
        public void deleteById(int userId) {
            users.removeIf(user -> user.getId() == userId);
        }

        @Override
        public User findByUsername(String username) {
            return users.stream().filter(user -> user.getUsername().equals(username)).findFirst().orElse(null);
        }

        @Override
        public User findByEmail(String email) {
            return users.stream().filter(user -> user.getEmail().equals(email)).findFirst().orElse(null);
        }

        @Override
        public List<User> findAll() {
            return new ArrayList<>(users);
        }
    }
}
