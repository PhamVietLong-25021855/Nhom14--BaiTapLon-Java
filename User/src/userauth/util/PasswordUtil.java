package userauth.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Objects;

public final class PasswordUtil {
    private static final String HASH_PREFIX = "pbkdf2_sha256";
    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String hashPassword(String password) {
        Objects.requireNonNull(password, "password");

        byte[] salt = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);

        byte[] derivedKey = deriveKey(password.toCharArray(), salt, PBKDF2_ITERATIONS, HASH_BYTES);
        return HASH_PREFIX
                + "$" + PBKDF2_ITERATIONS
                + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(derivedKey);
    }

    public static boolean verifyPassword(String inputPassword, String storedHash) {
        if (inputPassword == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }

        if (isModernHash(storedHash)) {
            return verifyModernHash(inputPassword, storedHash);
        }

        // Backward compatibility for existing SHA-256 hashes already stored in the database.
        byte[] calculatedLegacyHash = hashLegacyPassword(inputPassword).getBytes(StandardCharsets.UTF_8);
        byte[] storedLegacyHash = storedHash.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(calculatedLegacyHash, storedLegacyHash);
    }

    public static boolean needsRehash(String storedHash) {
        if (!isModernHash(storedHash)) {
            return true;
        }

        HashParts hashParts = parseModernHash(storedHash);
        return hashParts == null
                || hashParts.iterations < PBKDF2_ITERATIONS
                || hashParts.hash.length < HASH_BYTES;
    }

    private static boolean verifyModernHash(String inputPassword, String storedHash) {
        HashParts hashParts = parseModernHash(storedHash);
        if (hashParts == null) {
            return false;
        }

        byte[] derivedKey = deriveKey(
                inputPassword.toCharArray(),
                hashParts.salt,
                hashParts.iterations,
                hashParts.hash.length
        );
        return MessageDigest.isEqual(derivedKey, hashParts.hash);
    }

    private static boolean isModernHash(String storedHash) {
        return storedHash != null && storedHash.startsWith(HASH_PREFIX + "$");
    }

    private static HashParts parseModernHash(String storedHash) {
        String[] parts = storedHash.split("\\$");
        if (parts.length != 4 || !HASH_PREFIX.equals(parts[0])) {
            return null;
        }

        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] hash = Base64.getDecoder().decode(parts[3]);
            if (iterations < 1 || salt.length == 0 || hash.length == 0) {
                return null;
            }
            return new HashParts(iterations, salt, hash);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static byte[] deriveKey(char[] password, byte[] salt, int iterations, int hashLengthBytes) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, hashLengthBytes * 8);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new RuntimeException("Password hashing failed", ex);
        } finally {
            spec.clearPassword();
        }
    }

    private static String hashLegacyPassword(String password) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = messageDigest.digest(password.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte value : hashed) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("Password hashing failed", ex);
        }
    }

    private record HashParts(int iterations, byte[] salt, byte[] hash) {
    }
}
