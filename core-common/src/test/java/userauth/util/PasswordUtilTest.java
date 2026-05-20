package userauth.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilTest {
    @Test
    void hashPasswordUsesModernFormatAndVerifiesOriginalPassword() {
        String hash = PasswordUtil.hashPassword("secret123");
        String[] parts = hash.split("\\$");

        assertEquals(4, parts.length);
        assertEquals("pbkdf2_sha256", parts[0]);
        assertEquals("120000", parts[1]);
        assertTrue(PasswordUtil.verifyPassword("secret123", hash));
        assertFalse(PasswordUtil.verifyPassword("wrong-password", hash));
        assertFalse(PasswordUtil.needsRehash(hash));
    }

    @Test
    void hashPasswordUsesDifferentSaltForEachHash() {
        String firstHash = PasswordUtil.hashPassword("secret123");
        String secondHash = PasswordUtil.hashPassword("secret123");

        assertNotEquals(firstHash, secondHash);
        assertTrue(PasswordUtil.verifyPassword("secret123", firstHash));
        assertTrue(PasswordUtil.verifyPassword("secret123", secondHash));
    }

    @Test
    void verifyPasswordSupportsLegacySha256Hashes() throws Exception {
        String legacyHash = sha256Hex("legacy123");

        assertTrue(PasswordUtil.verifyPassword("legacy123", legacyHash));
        assertFalse(PasswordUtil.verifyPassword("different", legacyHash));
        assertTrue(PasswordUtil.needsRehash(legacyHash));
    }

    @Test
    void invalidInputsFailVerificationAndRequireRehash() {
        assertFalse(PasswordUtil.verifyPassword(null, PasswordUtil.hashPassword("secret123")));
        assertFalse(PasswordUtil.verifyPassword("secret123", null));
        assertFalse(PasswordUtil.verifyPassword("secret123", ""));
        assertFalse(PasswordUtil.verifyPassword("secret123", "pbkdf2_sha256$bad$hash"));

        assertTrue(PasswordUtil.needsRehash(null));
        assertTrue(PasswordUtil.needsRehash("pbkdf2_sha256$bad$hash"));
    }

    private static String sha256Hex(String value) throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] hashed = messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hashed) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
