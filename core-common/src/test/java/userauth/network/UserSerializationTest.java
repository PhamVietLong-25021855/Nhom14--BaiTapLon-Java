package userauth.network;

import org.junit.jupiter.api.Test;
import userauth.model.Bidder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserSerializationTest {
    @Test
    void serializedUserDoesNotExposePasswordHash() throws Exception {
        Bidder user = new Bidder(7, "bidder", "secret-password-hash", "Bidder", "bidder@example.com",
                "ACTIVE", 100L, 200L);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(user);
        }

        Bidder copy;
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            copy = (Bidder) input.readObject();
        }

        assertNull(copy.getPassword());
        assertFalse(user.toString().contains("secret-password-hash"));
    }
}
