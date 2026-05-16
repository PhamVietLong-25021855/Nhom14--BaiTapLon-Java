package userauth.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserValidatorTest {

    @Test
    void usernameMustBeBetweenSixAndTwentyCharacters() {
        assertTrue(UserValidator.isValidUsername("user01"));
        assertTrue(UserValidator.isValidUsername("abcdefghijklmnopqrst"));

        assertFalse(UserValidator.isValidUsername(null));
        assertFalse(UserValidator.isValidUsername(""));
        assertFalse(UserValidator.isValidUsername("abc12"));
        assertFalse(UserValidator.isValidUsername("abcdefghijklmnopqrstu"));
    }

    @Test
    void passwordMustContainLettersAndNumbers() {
        assertTrue(UserValidator.isValidPassword("abc123"));
        assertTrue(UserValidator.isValidPassword("123456a"));

        assertFalse(UserValidator.isValidPassword(null));
        assertFalse(UserValidator.isValidPassword("abcde"));
        assertFalse(UserValidator.isValidPassword("abcdef"));
        assertFalse(UserValidator.isValidPassword("123456"));
    }

    @Test
    void emailAllowsTrimmedStandardAddressesOnly() {
        assertTrue(UserValidator.isValidEmail(" user.name+test@example.com "));

        assertFalse(UserValidator.isValidEmail(null));
        assertFalse(UserValidator.isValidEmail(""));
        assertFalse(UserValidator.isValidEmail("missing-at.example.com"));
        assertFalse(UserValidator.isValidEmail("missing-domain@"));
    }
}
