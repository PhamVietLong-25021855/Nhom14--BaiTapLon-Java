package userauth.database;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class DatabaseConfig {
    private static final String RESOURCE_PATH = "/userauth/database.properties";

    private final String jdbcUrl;
    private final String adminJdbcUrl;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final String sslMode;
    private final String schema;
    private final boolean createDatabaseIfMissing;

    private DatabaseConfig(Properties properties) {
        this.jdbcUrl = resolveString(properties, "db.url", "DB_URL", null);
        this.adminJdbcUrl = resolveString(properties, "db.adminUrl", "DB_ADMIN_URL", null);
        this.host = resolveString(properties, "db.host", "DB_HOST", "127.0.0.1");
        this.port = resolveInt(properties, "db.port", "DB_PORT", 5432);
        this.database = resolveString(properties, "db.name", "DB_NAME", "postgres");
        this.username = resolveString(properties, "db.username", "DB_USERNAME", "postgres");
        this.password = resolveString(properties, "db.password", "DB_PASSWORD", "");
        this.sslMode = resolveString(
                properties,
                "db.sslMode",
                "DB_SSL_MODE",
                resolveBoolean(properties, "db.useSSL", "DB_USE_SSL", true) ? "require" : "disable"
        );
        this.schema = resolveString(properties, "db.schema", "DB_SCHEMA", "public");
        this.createDatabaseIfMissing = resolveBoolean(
                properties,
                "db.createDatabaseIfMissing",
                "DB_CREATE_DATABASE_IF_MISSING",
                false
        );
    }

    public static DatabaseConfig load() {
        Properties properties = new Properties();
        try (InputStream inputStream = DatabaseConfig.class.getResourceAsStream(RESOURCE_PATH)) {
            if (inputStream == null) {
                throw new IllegalStateException("Database configuration file not found: " + RESOURCE_PATH);
            }
            properties.load(inputStream);
            return new DatabaseConfig(properties);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read the database configuration file.", ex);
        }
    }

    public String getServerJdbcUrl() {
        if (adminJdbcUrl != null) {
            return adminJdbcUrl;
        }
        return buildJdbcUrl("postgres");
    }

    public String getDatabaseJdbcUrl() {
        if (jdbcUrl != null) {
            return jdbcUrl;
        }
        return buildJdbcUrl(database);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getSslMode() {
        return sslMode;
    }

    public String getSchema() {
        return schema;
    }

    public boolean isCreateDatabaseIfMissing() {
        return createDatabaseIfMissing;
    }

    private String buildJdbcUrl(String targetDatabase) {
        System.out.println("jdbc:postgresql://" + host + ":" + port + "/" + targetDatabase + "?" + buildQueryString());
        return "jdbc:postgresql://" + host + ":" + port + "/" + targetDatabase + "?" + buildQueryString();
    }

    private String buildQueryString() {
        return "sslmode=" + encode(sslMode) +
                "&currentSchema=" + encode(schema);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String resolveString(Properties properties, String key, String envKey, String defaultValue) {
        String value = normalize(System.getProperty(key));
        if (value != null) {
            return value;
        }

        value = normalize(System.getenv(envKey));
        if (value != null) {
            return value;
        }

        value = normalize(properties.getProperty(key));
        return value == null ? defaultValue : value;
    }

    private static int resolveInt(Properties properties, String key, String envKey, int defaultValue) {
        String value = normalize(System.getProperty(key));
        if (value == null) {
            value = normalize(System.getenv(envKey));
        }
        if (value == null) {
            value = normalize(properties.getProperty(key));
        }
        if (value == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Invalid numeric value for " + key + ": " + value, ex);
        }
    }

    private static boolean resolveBoolean(Properties properties, String key, String envKey, boolean defaultValue) {
        String value = normalize(System.getProperty(key));
        if (value == null) {
            value = normalize(System.getenv(envKey));
        }
        if (value == null) {
            value = normalize(properties.getProperty(key));
        }
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
