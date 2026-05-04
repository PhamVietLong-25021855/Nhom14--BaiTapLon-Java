package userauth.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordUtil {
    private static final String MODERN_PREFIX = "pbkdf2_sha256";
    private static final int DEFAULT_ITERATIONS = 600_000;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int DERIVED_KEY_LENGTH_BITS = 256;
    private static final String LEGACY_SHA256_PREFIX = "sha256:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String hashPassword(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null.");
        }

        byte[] salt = new byte[SALT_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        byte[] derivedKey = pbkdf2(password.toCharArray(), salt, DEFAULT_ITERATIONS, DERIVED_KEY_LENGTH_BITS);

        return MODERN_PREFIX
                + "$" + DEFAULT_ITERATIONS
                + "$" + Base64.getEncoder().withoutPadding().encodeToString(salt)
                + "$" + Base64.getEncoder().withoutPadding().encodeToString(derivedKey);
    }

    public static boolean verifyPassword(String inputPassword, String storedHash) {
        if (inputPassword == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }

        if (isModernHash(storedHash)) {
            return verifyModernHash(inputPassword, storedHash);
        }

        return verifyLegacySha256(inputPassword, storedHash);
    }

    public static boolean needsRehash(String storedHash) {
        if (storedHash == null || storedHash.isBlank()) {
            return true;
        }

        if (!isModernHash(storedHash)) {
            return true;
        }

        String[] parts = storedHash.split("\\$");
        if (parts.length != 4) {
            return true;
        }

        try {
            int iterations = Integer.parseInt(parts[1]);
            return iterations < DEFAULT_ITERATIONS;
        } catch (NumberFormatException ex) {
            return true;
        }
    }

    private static boolean isModernHash(String storedHash) {
        return storedHash.startsWith(MODERN_PREFIX + "$");
    }

    private static boolean verifyModernHash(String inputPassword, String storedHash) {
        String[] parts = storedHash.split("\\$");
        if (parts.length != 4) {
            return false;
        }

        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[3]);
            byte[] actualHash = pbkdf2(inputPassword.toCharArray(), salt, iterations, expectedHash.length * 8);
            return MessageDigest.isEqual(actualHash, expectedHash);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static boolean verifyLegacySha256(String inputPassword, String storedHash) {
        String normalizedStoredHash = normalizeLegacyHash(storedHash);
        if (!isLikelyLegacySha256(normalizedStoredHash)) {
            return false;
        }

        byte[] expectedHash = hexToBytes(normalizedStoredHash);
        byte[] actualHash = sha256(inputPassword);
        return MessageDigest.isEqual(actualHash, expectedHash);
    }

    private static String normalizeLegacyHash(String storedHash) {
        if (storedHash.startsWith(LEGACY_SHA256_PREFIX)) {
            return storedHash.substring(LEGACY_SHA256_PREFIX.length());
        }
        return storedHash;
    }

    private static boolean isLikelyLegacySha256(String storedHash) {
        return storedHash.length() == 64 && storedHash.matches("(?i)[0-9a-f]{64}");
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            try {
                return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            } finally {
                spec.clearPassword();
            }
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Password hashing failed.", ex);
        }
    }

    private static byte[] sha256(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(password.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Legacy password hashing failed.", ex);
        }
    }

    private static byte[] hexToBytes(String value) {
        byte[] bytes = new byte[value.length() / 2];
        for (int i = 0; i < value.length(); i += 2) {
            bytes[i / 2] = (byte) Integer.parseInt(value.substring(i, i + 2), 16);
        }
        return bytes;
    }
}
