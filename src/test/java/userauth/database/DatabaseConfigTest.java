package userauth.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseConfigTest {

    @AfterEach
    void clearProperties() {
        System.clearProperty("db.type");
        System.clearProperty("db.host");
        System.clearProperty("db.port");
        System.clearProperty("db.name");
        System.clearProperty("db.username");
        System.clearProperty("db.password");
        System.clearProperty("db.sslMode");
        System.clearProperty("db.createDatabaseIfMissing");
    }

    @Test
    void systemPropertiesOverrideResourceDefaults() {
        System.setProperty("db.type", "mysql");
        System.setProperty("db.host", "db.example.test");
        System.setProperty("db.port", "12345");
        System.setProperty("db.name", "auction_test");
        System.setProperty("db.username", "tester");
        System.setProperty("db.password", "secret");
        System.setProperty("db.sslMode", "DISABLED");
        System.setProperty("db.createDatabaseIfMissing", "true");

        DatabaseConfig config = DatabaseConfig.load();

        assertTrue(config.isMySql());
        assertEquals("db.example.test", config.getHost());
        assertEquals(12345, config.getPort());
        assertEquals("auction_test", config.getDatabase());
        assertEquals("tester", config.getUsername());
        assertEquals("secret", config.getPassword());
        assertEquals("DISABLED", config.getSslMode());
        assertTrue(config.isCreateDatabaseIfMissing());
        assertTrue(config.getDatabaseJdbcUrl().startsWith("jdbc:mysql://db.example.test:12345/auction_test?"));
        assertTrue(config.getDatabaseJdbcUrl().contains("sslMode=DISABLED"));
        assertTrue(config.getDatabaseJdbcUrl().contains("connectTimeout=10000"));
    }
}
