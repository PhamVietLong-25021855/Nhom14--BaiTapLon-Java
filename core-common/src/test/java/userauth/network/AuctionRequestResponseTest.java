package userauth.network;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionRequestResponseTest {
    @Test
    void requestHandlesNullParamsAsEmptyMap() {
        AuctionRequest request = new AuctionRequest(NetworkActions.PING, null);

        assertEquals(NetworkActions.PING, request.getAction());
        assertTrue(request.getParams().isEmpty());
        assertNull(request.get("missing"));
    }

    @Test
    void requestCopiesConstructorParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("auctionId", 10);

        AuctionRequest request = new AuctionRequest(NetworkActions.AUCTION_BIDS, params);
        params.put("auctionId", 20);

        assertEquals(10, request.get("auctionId"));
    }

    @Test
    void okResponseCarriesDataWithoutErrorFields() {
        AuctionResponse response = AuctionResponse.ok("PONG");

        assertTrue(response.isSuccess());
        assertEquals("PONG", response.getData());
        assertNull(response.getErrorType());
        assertNull(response.getErrorMessage());
    }

    @Test
    void failResponseCarriesThrowableTypeAndMessage() {
        AuctionResponse response = AuctionResponse.fail(new IllegalArgumentException("bad request"));

        assertFalse(response.isSuccess());
        assertNull(response.getData());
        assertEquals("IllegalArgumentException", response.getErrorType());
        assertEquals("bad request", response.getErrorMessage());
    }

    @Test
    void failResponseFallsBackWhenThrowableOrMessageIsMissing() {
        AuctionResponse nullThrowableResponse = AuctionResponse.fail(null);
        AuctionResponse blankMessageResponse = AuctionResponse.fail(new IllegalStateException(""));

        assertEquals("UNKNOWN", nullThrowableResponse.getErrorType());
        assertEquals("Unknown server error.", nullThrowableResponse.getErrorMessage());
        assertEquals("IllegalStateException", blankMessageResponse.getErrorType());
        assertEquals("IllegalStateException", blankMessageResponse.getErrorMessage());
    }
}
