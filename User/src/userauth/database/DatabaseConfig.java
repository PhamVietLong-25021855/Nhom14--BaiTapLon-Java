package userauth.database;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class DatabaseConfig {
    private static final String RESOURCE_PATH = "/userauth/database.properties";

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final boolean useSsl;
    private final boolean createDatabaseIfMissing;
    private final String serverTimezone;

    private DatabaseConfig(Properties properties) {
        this.host = readString(properties, "db.host", "127.0.0.1");
        this.port = readInt(properties, "db.port", 3306);
        this.database = readString(properties, "db.name", "baitaplon");
        this.username = readString(properties, "db.username", "root");
        this.password = readString(properties, "db.password", "");
        this.useSsl = readBoolean(properties, "db.useSSL", false);
        this.createDatabaseIfMissing = readBoolean(properties, "db.createDatabaseIfMissing", true);
        this.serverTimezone = readString(properties, "db.serverTimezone", "Asia/Ho_Chi_Minh");
    }

    public static DatabaseConfig load() {
        Properties properties = new Properties();
        try (InputStream inputStream = DatabaseConfig.class.getResourceAsStream(RESOURCE_PATH)) {
            if (inputStream == null) {
                throw new IllegalStateException("Khong tim thay file cau hinh database: " + RESOURCE_PATH);
            }
            properties.load(inputStream);
            return new DatabaseConfig(properties);
        } catch (IOException ex) {
            throw new IllegalStateException("Khong the doc file cau hinh database.", ex);
        }
    }

    public String getServerJdbcUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/?" + buildQueryString();
    }

    public String getDatabaseJdbcUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/" + database + "?" + buildQueryString();
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

    public boolean isCreateDatabaseIfMissing() {
        return createDatabaseIfMissing;
    }

    private String buildQueryString() {
        return "useSSL=" + useSsl +
                "&allowPublicKeyRetrieval=true" +
                "&serverTimezone=" + encode(serverTimezone) +
                "&characterEncoding=UTF-8";
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String readString(Properties properties, String key, String defaultValue) {
        String value = properties.getProperty(key);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static int readInt(Properties properties, String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Gia tri so khong hop le cho " + key + ": " + value, ex);
        }
    }

    private static boolean readBoolean(Properties properties, String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }
}
