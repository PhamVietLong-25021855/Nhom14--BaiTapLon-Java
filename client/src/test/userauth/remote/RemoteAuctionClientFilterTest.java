package userauth.remote;

import org.junit.jupiter.api.Test;
import userauth.model.AuctionItem;
import userauth.network.AuctionResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RemoteAuctionClientFilterTest {
    @Test
    void filterAllowsExpectedResponseContainersAndModels() throws Exception {
        List<AuctionItem> auctions = new ArrayList<>();
        auctions.add(new AuctionItem(0, "Item", "Description", 100_000D, 1L, 2L, "Category", null, null, 7));

        AuctionResponse response = (AuctionResponse) readFiltered(AuctionResponse.ok(auctions));

        assertEquals(1, ((List<?>) response.getData()).size());
    }

    @Test
    void filterRejectsUnexpectedSerializableClasses() {
        assertThrows(InvalidClassException.class, () -> readFiltered(AuctionResponse.ok(new File("unexpected"))));
    }

    private Object readFiltered(Object value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(value);
        }
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            input.setObjectInputFilter(RemoteAuctionClient::filterResponse);
            return input.readObject();
        }
    }
}
