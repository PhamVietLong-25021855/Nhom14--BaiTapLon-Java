package userauth.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserValidatorTest {
    @Test
    void usernameMustStayWithinConfiguredLengthRange() {
        assertFalse(UserValidator.isValidUsername(null));
        assertFalse(UserValidator.isValidUsername(""));
        assertFalse(UserValidator.isValidUsername("abcde"));
        assertTrue(UserValidator.isValidUsername("abcdef"));
        assertTrue(UserValidator.isValidUsername("a".repeat(20)));
        assertFalse(UserValidator.isValidUsername("a".repeat(21)));
    }

    @Test
    void passwordRequiresAtLeastSixCharactersWithLettersAndDigits() {
        assertFalse(UserValidator.isValidPassword(null));
        assertFalse(UserValidator.isValidPassword("abc12"));
        assertFalse(UserValidator.isValidPassword("abcdef"));
        assertFalse(UserValidator.isValidPassword("123456"));
        assertTrue(UserValidator.isValidPassword("abc123"));
        assertTrue(UserValidator.isValidPassword("123456a"));
    }

    @Test
    void emailAcceptsTrimmedValidAddressesAndRejectsMalformedOnes() {
        assertTrue(UserValidator.isValidEmail(" user.name+tag@example.com "));
        assertTrue(UserValidator.isValidEmail("auction.user@example.co"));

        assertFalse(UserValidator.isValidEmail(null));
        assertFalse(UserValidator.isValidEmail(""));
        assertFalse(UserValidator.isValidEmail("user@example"));
        assertFalse(UserValidator.isValidEmail("user@"));
        assertFalse(UserValidator.isValidEmail("@example.com"));
    }
}
