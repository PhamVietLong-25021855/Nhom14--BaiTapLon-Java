package userauth.service;

import org.junit.jupiter.api.Test;
import userauth.dao.UserDAO;
import userauth.exception.UnauthorizedException;
import userauth.exception.ValidationException;
import userauth.model.Bidder;
import userauth.model.Role;
import userauth.model.User;
import userauth.util.PasswordUtil;

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
