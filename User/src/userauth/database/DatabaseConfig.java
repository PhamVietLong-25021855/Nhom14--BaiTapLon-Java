package userauth.database;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class DatabaseConfig {
    private static final String RESOURCE_PATH = "/userauth/database.properties";
    private static final String LOCAL_OVERRIDE_PROPERTY = "db.localOverridePath";
    private static final String LOCAL_OVERRIDE_ENV = "DB_LOCAL_OVERRIDE_PATH";
    private static final String DEFAULT_LOCAL_OVERRIDE_PATH = "User/resources/userauth/database.local.properties";

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
    private final int connectTimeoutSeconds;
    private final int socketTimeoutSeconds;
    private final boolean tcpKeepAlive;
    private final String applicationName;
    private final int maxPoolConnections;

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
        this.connectTimeoutSeconds = resolveInt(
                properties,
                "db.connectTimeoutSeconds",
                "DB_CONNECT_TIMEOUT_SECONDS",
                10
        );
        this.socketTimeoutSeconds = resolveInt(
                properties,
                "db.socketTimeoutSeconds",
                "DB_SOCKET_TIMEOUT_SECONDS",
                30
        );
        this.tcpKeepAlive = resolveBoolean(
                properties,
                "db.tcpKeepAlive",
                "DB_TCP_KEEP_ALIVE",
                true
        );
        this.applicationName = resolveString(
                properties,
                "db.applicationName",
                "DB_APPLICATION_NAME",
                "online-auction-client"
        );
        this.maxPoolConnections = Math.max(
                1,
                resolveInt(properties, "db.maxPoolConnections", "DB_MAX_POOL_CONNECTIONS", 2)
        );
    }

    public static DatabaseConfig load() {
        Properties properties = new Properties();
        try (InputStream inputStream = DatabaseConfig.class.getResourceAsStream(RESOURCE_PATH)) {
            if (inputStream == null) {
                throw new IllegalStateException("Database configuration file not found: " + RESOURCE_PATH);
            }
            properties.load(inputStream);
            loadOptionalOverride(properties);
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

    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public int getSocketTimeoutSeconds() {
        return socketTimeoutSeconds;
    }

    public boolean isTcpKeepAlive() {
        return tcpKeepAlive;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public int getMaxPoolConnections() {
        return maxPoolConnections;
    }

    private String buildJdbcUrl(String targetDatabase) {
        return "jdbc:postgresql://" + host + ":" + port + "/" + targetDatabase + "?" + buildQueryString();
    }

    private String buildQueryString() {
        return "sslmode=" + encode(sslMode) +
                "&currentSchema=" + encode(schema) +
                "&connectTimeout=" + connectTimeoutSeconds +
                "&socketTimeout=" + socketTimeoutSeconds +
                "&tcpKeepAlive=" + tcpKeepAlive +
                "&ApplicationName=" + encode(applicationName);
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

    private static void loadOptionalOverride(Properties properties) throws IOException {
        String overridePathValue = normalize(System.getProperty(LOCAL_OVERRIDE_PROPERTY));
        if (overridePathValue == null) {
            overridePathValue = normalize(System.getenv(LOCAL_OVERRIDE_ENV));
        }
        if (overridePathValue == null) {
            overridePathValue = DEFAULT_LOCAL_OVERRIDE_PATH;
        }

        Path overridePath = Paths.get(overridePathValue).normalize();
        if (!Files.isRegularFile(overridePath)) {
            return;
        }

        try (InputStream overrideStream = Files.newInputStream(overridePath)) {
            properties.load(overrideStream);
        }
    }
}
