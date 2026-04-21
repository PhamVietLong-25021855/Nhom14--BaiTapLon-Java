package userauth.database;

import userauth.utils.ConsoleUI;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public final class DatabaseInitializer {
    private static final String CREATE_USERS_TABLE = """
            CREATE TABLE IF NOT EXISTS users (
                id INT PRIMARY KEY AUTO_INCREMENT,
                username VARCHAR(50) NOT NULL UNIQUE,
                password VARCHAR(255) NOT NULL,
                full_name VARCHAR(100) NOT NULL,
                email VARCHAR(100) NOT NULL UNIQUE,
                role VARCHAR(20) NOT NULL,
                status VARCHAR(20) NOT NULL,
                created_at BIGINT NOT NULL,
                updated_at BIGINT NOT NULL
            )
            """;

    private static final String CREATE_AUCTIONS_TABLE = """
            CREATE TABLE IF NOT EXISTS auctions (
                id INT PRIMARY KEY AUTO_INCREMENT,
                name VARCHAR(200) NOT NULL,
                description TEXT,
                start_price DECIMAL(15, 2) NOT NULL,
                current_highest_bid DECIMAL(15, 2) NOT NULL,
                start_time BIGINT NOT NULL,
                end_time BIGINT NOT NULL,
                category VARCHAR(100),
                created_at BIGINT NOT NULL,
                updated_at BIGINT NOT NULL,
                seller_id INT NOT NULL,
                winner_id INT NULL,
                status VARCHAR(20) NOT NULL,
                CONSTRAINT fk_auctions_seller FOREIGN KEY (seller_id) REFERENCES users(id)
            )
            """;

    private static final String CREATE_BIDS_TABLE = """
            CREATE TABLE IF NOT EXISTS bids (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                auction_id INT NOT NULL,
                bidder_id INT NOT NULL,
                amount DECIMAL(15, 2) NOT NULL,
                bid_time BIGINT NOT NULL,
                status VARCHAR(20) NOT NULL,
                CONSTRAINT fk_bids_auction FOREIGN KEY (auction_id) REFERENCES auctions(id),
                CONSTRAINT fk_bids_bidder FOREIGN KEY (bidder_id) REFERENCES users(id)
            )
            """;

    private static final String CREATE_HOMEPAGE_ANNOUNCEMENTS_TABLE = """
            CREATE TABLE IF NOT EXISTS homepage_announcements (
                id INT PRIMARY KEY AUTO_INCREMENT,
                title VARCHAR(255) NOT NULL,
                summary TEXT NOT NULL,
                details TEXT,
                schedule_text VARCHAR(255) NOT NULL,
                linked_auction_id INT NULL,
                author_id INT NOT NULL,
                created_at BIGINT NOT NULL,
                updated_at BIGINT NOT NULL,
                CONSTRAINT fk_homepage_author FOREIGN KEY (author_id) REFERENCES users(id),
                CONSTRAINT fk_homepage_linked_auction FOREIGN KEY (linked_auction_id) REFERENCES auctions(id)
            )
            """;

    private DatabaseInitializer() {
    }

    public static void initialize() {
        DatabaseConfig config = DatabaseConnection.getConfig();
        try {
            if (config.isCreateDatabaseIfMissing()) {
                createDatabaseIfMissing(config.getDatabase());
            }
            createTables();
            ConsoleUI.printSuccess(
                    "Da ket noi MySQL thanh cong: " +
                            config.getHost() + ":" + config.getPort() + "/" + config.getDatabase() +
                            " (user: " + config.getUsername() + ")"
            );
        } catch (SQLException ex) {
            throw new IllegalStateException("Khong the khoi tao ket noi MySQL.", ex);
        }
    }

    public static boolean testConnection() {
        try (Connection ignored = DatabaseConnection.openDatabaseConnection()) {
            return true;
        } catch (SQLException ex) {
            return false;
        }
    }

    private static void createDatabaseIfMissing(String databaseName) throws SQLException {
        String safeDatabaseName = databaseName.replace("`", "");
        String sql = "CREATE DATABASE IF NOT EXISTS `" + safeDatabaseName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
        try (Connection connection = DatabaseConnection.openServerConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static void createTables() throws SQLException {
        List<String> statements = List.of(
                CREATE_USERS_TABLE,
                CREATE_AUCTIONS_TABLE,
                CREATE_BIDS_TABLE,
                CREATE_HOMEPAGE_ANNOUNCEMENTS_TABLE
        );

        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.executeUpdate(sql);
            }
        }
    }
}
