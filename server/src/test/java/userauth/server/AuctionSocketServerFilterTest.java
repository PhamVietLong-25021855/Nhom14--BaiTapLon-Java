package userauth.server;

import org.junit.jupiter.api.Test;
import userauth.model.PaymentMethod;
import userauth.model.Role;
import userauth.network.AuctionRequest;
import userauth.network.NetworkActions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuctionSocketServerFilterTest {
    @Test
    void filterAllowsExpectedRequestValueTypes() throws Exception {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("sellerId", 7);
        params.put("startPrice", 100_000D);
        params.put("amount", 10_000L);
        params.put("role", Role.SELLER);
        params.put("method", PaymentMethod.BANK_TRANSFER);
        params.put("imageData", new byte[64]);

        AuctionRequest request = (AuctionRequest) readFiltered(
                new AuctionRequest(NetworkActions.AUCTION_CREATE, params, "session-token"));

        assertEquals(NetworkActions.AUCTION_CREATE, request.getAction());
        assertEquals(Role.SELLER, request.get("role"));
    }

    @Test
    void filterRejectsUnexpectedSerializableClasses() {
        assertThrows(InvalidClassException.class, () -> readFiltered(new File("unexpected")));
    }

    private Object readFiltered(Object value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(value);
        }
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            input.setObjectInputFilter(AuctionSocketServer::filterRequest);
            return input.readObject();
        }
    }
}
