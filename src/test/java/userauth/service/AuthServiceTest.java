package userauth.service;

import org.junit.jupiter.api.Test;
import userauth.dao.UserDAO;
import userauth.exception.UnauthorizedException;
import userauth.exception.ValidationException;
import userauth.model.Bidder;
import userauth.model.Role;
import userauth.model.User;
import userauth.util.PasswordUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    @Test
    void registerAllowsThreeCharacterUsernameAndHashesPassword() throws ValidationException {
        InMemoryUserDAO userDAO = new InMemoryUserDAO();
        AuthService service = new AuthService(userDAO);

        service.register("abc", "Pass123", "Alice", "alice@example.com", Role.BIDDER);

        User saved = userDAO.findByUsername("abc");
        assertNotNull(saved);
        assertNotEquals("Pass123", saved.getPassword());
        assertTrue(saved.getPassword().startsWith("pbkdf2_sha256$"));
        assertTrue(saved.checkPassword("Pass123"));
    }

    @Test
    void loginRejectsBlockedAccounts() {
        InMemoryUserDAO userDAO = new InMemoryUserDAO();
        long now = System.currentTimeMillis();
        userDAO.save(new Bidder(
                0,
                "blocked",
                PasswordUtil.hashPassword("Pass123"),
                "Blocked User",
                "blocked@example.com",
                "BLOCKED",
                now,
                now
        ));
        AuthService service = new AuthService(userDAO);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> service.login("blocked", "Pass123"));
        assertEquals("Your account has been locked.", ex.getMessage());
    }

    @Test
    void loginUpgradesLegacySha256HashWithoutBreakingExistingAccount() throws UnauthorizedException {
        InMemoryUserDAO userDAO = new InMemoryUserDAO();
        long now = System.currentTimeMillis();
        userDAO.save(new Bidder(
                0,
                "legacyUser",
                legacySha256("Pass123"),
                "Legacy User",
                "legacy@example.com",
                "ACTIVE",
                now,
                now
        ));
        AuthService service = new AuthService(userDAO);

        User loggedIn = service.login("legacyUser", "Pass123");

        assertNotNull(loggedIn);
        User saved = userDAO.findByUsername("legacyUser");
        assertNotNull(saved);
        assertTrue(saved.getPassword().startsWith("pbkdf2_sha256$"));
        assertTrue(saved.checkPassword("Pass123"));
    }

    @Test
    void changePasswordUpgradesLegacyAccountToModernHash() throws ValidationException, UnauthorizedException {
        InMemoryUserDAO userDAO = new InMemoryUserDAO();
        long now = System.currentTimeMillis();
        userDAO.save(new Bidder(
                0,
                "legacyChange",
                legacySha256("Pass123"),
                "Legacy Change",
                "legacy-change@example.com",
                "ACTIVE",
                now,
                now
        ));
        AuthService service = new AuthService(userDAO);

        service.changePassword("legacyChange", "Pass123", "NewPass123");

        User saved = userDAO.findByUsername("legacyChange");
        assertNotNull(saved);
        assertTrue(saved.getPassword().startsWith("pbkdf2_sha256$"));
        assertTrue(saved.checkPassword("NewPass123"));
        assertFalse(saved.checkPassword("Pass123"));
    }

    private static String legacySha256(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashed = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hashed) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static final class InMemoryUserDAO implements UserDAO {
        private final List<User> users = new ArrayList<>();
        private int nextId = 1;

        @Override
        public void save(User user) {
            if (user.getId() <= 0) {
                user.setId(nextId++);
            }
            users.add(user);
        }

        @Override
        public void update(User user) {
            delete(user.getId());
            users.add(user);
        }

        @Override
        public void delete(int userId) {
            users.removeIf(user -> user.getId() == userId);
        }

        @Override
        public User findById(int userId) {
            return users.stream()
                    .filter(user -> user.getId() == userId)
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public User findByUsername(String username) {
            return users.stream()
                    .filter(user -> user.getUsername().equalsIgnoreCase(username))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public User findByEmail(String email) {
            return users.stream()
                    .filter(user -> user.getEmail().equalsIgnoreCase(email))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public List<User> findAll() {
            return users.stream()
                    .sorted(Comparator.comparingInt(User::getId))
                    .toList();
        }
    }
}
