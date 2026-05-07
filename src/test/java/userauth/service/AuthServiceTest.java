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
    void registerDuplicateUsernameThrowsValidationException() throws Exception {
        authService.register("seller01", "abc123", "Seller One", "seller01@example.com", Role.SELLER);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> authService.register("seller01", "abc123", "Seller Other", "seller02@example.com", Role.SELLER));

        assertTrue(ex.getMessage().toLowerCase().contains("username"));
    }

    @Test
    void loginWrongPasswordThrowsUnauthorizedException() throws Exception {
        authService.register("admin01", "abc123", "Admin One", "admin01@example.com", Role.ADMIN);

        assertThrows(UnauthorizedException.class, () -> authService.login("admin01", "wrong123"));
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
