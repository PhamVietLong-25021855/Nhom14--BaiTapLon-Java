package userauth.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilTest {

    @Test
    void verifiesModernHashesAndMarksThemAsUpToDate() {
        String hash = PasswordUtil.hashPassword("Pass123");

        assertTrue(PasswordUtil.verifyPassword("Pass123", hash));
        assertFalse(PasswordUtil.verifyPassword("Wrong123", hash));
        assertFalse(PasswordUtil.needsRehash(hash));
    }

    @Test
    void verifiesLegacySha256HashesAndMarksThemForRehash() throws Exception {
        byte[] legacyBytes = MessageDigest.getInstance("SHA-256")
                .digest("Pass123".getBytes(StandardCharsets.UTF_8));

        StringBuilder legacyHash = new StringBuilder();
        for (byte value : legacyBytes) {
            legacyHash.append(String.format("%02x", value));
        }

        assertTrue(PasswordUtil.verifyPassword("Pass123", legacyHash.toString()));
        assertTrue(PasswordUtil.needsRehash(legacyHash.toString()));
    }
}
