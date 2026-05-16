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
    void auctionCreatePayloadKeepsAllSubmittedFieldsAcrossSocketSerialization() throws Exception {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", "Laptop Pro");
        params.put("desc", "Clean condition");
        params.put("startPrice", 1200.0);
        params.put("startTime", 1000L);
        params.put("endTime", 7000L);
        params.put("category", "Electronics");
        params.put("imageSource", "laptop.png");
        params.put("imageData", new byte[]{9, 8, 7, 6});
        params.put("sellerId", 42);

        AuctionRequest restored = roundTrip(new AuctionRequest(NetworkActions.AUCTION_CREATE, params));

        assertEquals(NetworkActions.AUCTION_CREATE, restored.getAction());
        assertEquals("Laptop Pro", restored.get("name"));
        assertEquals("Clean condition", restored.get("desc"));
        assertEquals(1200.0, restored.get("startPrice"));
        assertEquals(1000L, restored.get("startTime"));
        assertEquals(7000L, restored.get("endTime"));
        assertEquals("Electronics", restored.get("category"));
        assertEquals("laptop.png", restored.get("imageSource"));
        assertArrayEquals(new byte[]{9, 8, 7, 6}, (byte[]) restored.get("imageData"));
        assertEquals(42, restored.get("sellerId"));
    }

    @Test
    void profileUpdatePayloadKeepsSubmittedFieldsAcrossSocketSerialization() throws Exception {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("username", "seller01");
        params.put("fullName", "Seller Updated");
        params.put("email", "seller.updated@example.com");

        AuctionRequest restored = roundTrip(new AuctionRequest(NetworkActions.AUTH_UPDATE_PROFILE, params));

        assertEquals(NetworkActions.AUTH_UPDATE_PROFILE, restored.getAction());
        assertEquals("seller01", restored.get("username"));
        assertEquals("Seller Updated", restored.get("fullName"));
        assertEquals("seller.updated@example.com", restored.get("email"));
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
