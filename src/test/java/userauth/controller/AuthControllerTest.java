package userauth.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import userauth.model.Role;
import userauth.model.User;
import userauth.service.AuthService;
import userauth.support.TestDaos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthControllerTest {
    private TestDaos.InMemoryUserDao userDao;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        userDao = new TestDaos.InMemoryUserDao();
        controller = new AuthController(new AuthService(userDao));
    }

    @Test
    void registrationScreenBlocksAdminAccountCreation() {
        String result = controller.registerGUI(
                "admin01",
                "abc123",
                "Admin One",
                "admin01@example.com",
                Role.ADMIN
        );

        assertNotEquals("SUCCESS", result);
        assertTrue(result.toLowerCase().contains("admin"));
        assertNull(userDao.findByUsername("admin01"));
    }

    @Test
    void registrationScreenAllowsBidderAndSellerAccounts() {
        assertEquals("SUCCESS", controller.registerGUI(
                "bidder01",
                "abc123",
                "Bidder One",
                "bidder01@example.com",
                Role.BIDDER
        ));
        assertEquals("SUCCESS", controller.registerGUI(
                "seller01",
                "abc123",
                "Seller One",
                "seller01@example.com",
                Role.SELLER
        ));

        assertEquals(Role.BIDDER, userDao.findByUsername("bidder01").getRole());
        assertEquals(Role.SELLER, userDao.findByUsername("seller01").getRole());
    }

    @Test
    void invalidRegistrationReturnsValidationMessageWithoutSavingUser() {
        String result = controller.registerGUI(
                "abc",
                "abcdef",
                "Invalid User",
                "invalid@example.com",
                Role.BIDDER
        );

        assertNotEquals("SUCCESS", result);
        assertNull(userDao.findByUsername("abc"));
    }

    @Test
    void changePasswordThroughControllerInvalidatesOldPassword() throws Exception {
        controller.registerGUI("bidder01", "abc123", "Bidder One", "bidder01@example.com", Role.BIDDER);

        assertEquals("SUCCESS", controller.changePassword("bidder01", "abc123", "new123"));

        User loggedIn = controller.login("bidder01", "new123");
        assertNotNull(loggedIn);
        assertEquals("bidder01", loggedIn.getUsername());
    }
}
