package uet.auctionsystem.database;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

// Ghi chu file: File thuoc tang database; phu trach doc cau hinh, tao ket noi va khoi tao schema.
// Khai bao lop DatabaseConfig; phu trach mot phan ha tang ket noi va khoi tao database.
public final class DatabaseConfig {
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho path.
    private static final String RESOURCE_PATH = "/uet/auctionsystem/database.properties";
    // Thuoc tinh: giu phu thuoc can dung xuyen suot vong doi doi tuong.
    private final String jdbcUrl;
    // Thuoc tinh: giu phu thuoc can dung xuyen suot vong doi doi tuong.
    private final String adminJdbcUrl;
    // Thuoc tinh: giu phu thuoc can dung xuyen suot vong doi doi tuong.
    private final String host;
    // Thuoc tinh: giu phu thuoc can dung xuyen suot vong doi doi tuong.
    private final int port;
    // Thuoc tinh: giu phu thuoc can dung xuyen suot vong doi doi tuong.
    private final String database;
    // Thuoc tinh: giu phu thuoc can dung xuyen suot vong doi doi tuong.
    private final String username;
    // Thuoc tinh: giu phu thuoc can dung xuyen suot vong doi doi tuong.
    private final String password;
    // Thuoc tinh: giu phu thuoc can dung xuyen suot vong doi doi tuong.
    private final String sslMode;
    // Thuoc tinh: giu phu thuoc can dung xuyen suot vong doi doi tuong.
    private final String schema;
    // Thuoc tinh: giu phu thuoc can dung xuyen suot vong doi doi tuong.
    private final boolean createDatabaseIfMissing;
    // Ham tao: khoi tao doi tuong DatabaseConfig voi cac phu thuoc can thiet.
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
    // Phuong thuc: lay hoac doc du lieu cho thao tac load.
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
    // Phuong thuc: lay hoac doc du lieu cho thao tac get server jdbc url.
    public String getServerJdbcUrl() {
        if (adminJdbcUrl != null) {
            return adminJdbcUrl;
        }
        return buildJdbcUrl("postgres");
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get database jdbc url.
    public String getDatabaseJdbcUrl() {
        if (jdbcUrl != null) {
            return jdbcUrl;
        }
        return buildJdbcUrl(database);
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get host.
    public String getHost() {
        return host;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get port.
    public int getPort() {
        return port;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get database.
    public String getDatabase() {
        return database;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get username.
    public String getUsername() {
        return username;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get password.
    public String getPassword() {
        return password;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get ssl mode.
    public String getSslMode() {
        return sslMode;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get schema.
    public String getSchema() {
        return schema;
    }
    // Phuong thuc: kiem tra dieu kien hoac xac thuc cho thao tac is create database if missing.
    public boolean isCreateDatabaseIfMissing() {
        return createDatabaseIfMissing;
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac build jdbc url.
    private String buildJdbcUrl(String targetDatabase) {
        return "jdbc:postgresql://" + host + ":" + port + "/" + targetDatabase + "?" + buildQueryString();
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac build query string.
    private String buildQueryString() {
        return "sslmode=" + encode(sslMode) +
                "&currentSchema=" + encode(schema);
    }
    // Phuong thuc: thuc hien chuc nang encode trong lop DatabaseConfig.
    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
    // Phuong thuc: thuc hien chuc nang resolve string trong lop DatabaseConfig.
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
    // Phuong thuc: thuc hien chuc nang resolve int trong lop DatabaseConfig.
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
    // Phuong thuc: thuc hien chuc nang resolve boolean trong lop DatabaseConfig.
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
    // Phuong thuc: thuc hien chuc nang normalize trong lop DatabaseConfig.
    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
