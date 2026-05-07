package userauth.network;

import org.junit.jupiter.api.Test;
import userauth.exception.ValidationException;
import userauth.model.HomepageAnnouncement;
import userauth.model.Role;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionNetworkSerializationTest {

    @Test
    void requestCopiesInputParamsAndSurvivesSerialization() throws Exception {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("username", "bidder01");
        params.put("role", Role.BIDDER);
        params.put("imageData", new byte[]{1, 2, 3});

        AuctionRequest request = new AuctionRequest(NetworkActions.AUTH_REGISTER, params);
        params.put("username", "changed");

        AuctionRequest restored = roundTrip(request);

        assertEquals(NetworkActions.AUTH_REGISTER, restored.getAction());
        assertEquals("bidder01", restored.get("username"));
        assertEquals(Role.BIDDER, restored.get("role"));
        assertArrayEquals(new byte[]{1, 2, 3}, (byte[]) restored.get("imageData"));
    }

    @Test
    void okResponseCanCarryHomepageAnnouncementAcrossSocketSerialization() throws Exception {
        HomepageAnnouncement announcement = new HomepageAnnouncement(
                1,
                "Auction today",
                "Summary",
                "Details",
                "08:00",
                10,
                99,
                1000L,
                2000L
        );

        AuctionResponse restored = roundTrip(AuctionResponse.ok(announcement));

        assertTrue(restored.isSuccess());
        HomepageAnnouncement restoredAnnouncement = (HomepageAnnouncement) restored.getData();
        assertEquals("Auction today", restoredAnnouncement.getTitle());
        assertEquals(10, restoredAnnouncement.getLinkedAuctionId());
    }

    @Test
    void failedResponseKeepsErrorTypeAndMessage() {
        AuctionResponse response = AuctionResponse.fail(new ValidationException("Invalid input"));

        assertFalse(response.isSuccess());
        assertEquals("ValidationException", response.getErrorType());
        assertEquals("Invalid input", response.getErrorMessage());
    }

    @SuppressWarnings("unchecked")
    private <T> T roundTrip(T value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(value);
        }

        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (T) input.readObject();
        }
    }
}
