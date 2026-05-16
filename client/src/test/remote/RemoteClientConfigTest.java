package userauth.client.remote;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RemoteClientConfigTest {

    @AfterEach
    void clearProperties() {
        System.clearProperty("app.server.host");
        System.clearProperty("app.server.port");
    }

    @Test
    void systemPropertyOverridesDefaultHostAndIsTrimmed() {
        System.setProperty("app.server.host", "  10.0.0.5  ");

        assertEquals("10.0.0.5", RemoteClientConfig.host());
    }

    @Test
    void systemPropertyOverridesDefaultPort() {
        System.setProperty("app.server.port", "6060");

        assertEquals(6060, RemoteClientConfig.port());
    }

    @Test
    void invalidPortPropertyFailsFast() {
        System.setProperty("app.server.port", "not-a-port");

        assertThrows(NumberFormatException.class, RemoteClientConfig::port);
    }
}
