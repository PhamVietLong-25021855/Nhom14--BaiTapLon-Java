package userauth.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {
    private static final DatabaseConfig CONFIG = DatabaseConfig.load();

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Khong tim thay MySQL JDBC Driver. Hay dam bao mysql-connector-j da duoc nap.", ex);
        }
    }

    private DatabaseConnection() {
    }

    public static DatabaseConfig getConfig() {
        return CONFIG;
    }

    public static Connection openServerConnection() throws SQLException {
        return DriverManager.getConnection(
                CONFIG.getServerJdbcUrl(),
                CONFIG.getUsername(),
                CONFIG.getPassword()
        );
    }

    public static Connection openDatabaseConnection() throws SQLException {
        return DriverManager.getConnection(
                CONFIG.getDatabaseJdbcUrl(),
                CONFIG.getUsername(),
                CONFIG.getPassword()
        );
    }
}
