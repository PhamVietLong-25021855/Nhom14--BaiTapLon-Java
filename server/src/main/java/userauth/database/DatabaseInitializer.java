package userauth.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public final class DatabaseInitializer {
    private DatabaseInitializer() {
    }

    public static void initialize() {
        DatabaseConfig config = DatabaseConnection.getConfig();
        try {
            if (config.isCreateDatabaseIfMissing()) {
                createDatabaseIfMissing(config);
            }
            createTables(config);
            synchronizeDatabaseObjects(config);
            System.out.println(
                    "[Database] Connected to " + config.getDbType().toUpperCase() + " successfully: " +
                            config.getHost() + ":" + config.getPort() + "/" + config.getDatabase() +
                            " (user: " + config.getUsername() + ")"
            );
        } catch (SQLException ex) {
            throw new IllegalStateException(
                    "Unable to initialize the database connection to " +
                            config.getHost() + ":" + config.getPort() + "/" + config.getDatabase() +
                            ". Check the database host, port, Akamai trusted sources/firewall, and DB credentials.",
                    ex
            );
        }
    }

    public static boolean testConnection() {
        try (Connection connection = DatabaseConnection.openDatabaseConnection()) {
            return !connection.isClosed();
        } catch (SQLException ex) {
            return false;
        }
    }

    private static void createDatabaseIfMissing(DatabaseConfig config) throws SQLException {
        String safeDatabaseName = config.getDatabase().replace("`", "``").replace("\"", "\"\"");
        String sql = config.isMySql()
                ? "CREATE DATABASE IF NOT EXISTS `" + safeDatabaseName + "`"
                : "CREATE DATABASE \"" + safeDatabaseName + "\"";
        try (Connection connection = DatabaseConnection.openServerConnection();
             Statement statement = connection.createStatement()) {
            try {
                statement.executeUpdate(sql);
            } catch (SQLException ex) {
                if (!config.isPostgres() || !"42P04".equals(ex.getSQLState())) {
                    throw ex;
                }
            }
        }
    }

    private static void createTables(DatabaseConfig config) throws SQLException {
        List<String> statements = config.isMySql() ? mysqlCreateStatements() : postgresCreateStatements();
        executeStatements(statements, config);
    }

    private static void synchronizeDatabaseObjects(DatabaseConfig config) throws SQLException {
        List<String> statements = config.isMySql() ? mysqlSyncStatements() : postgresSyncStatements();
        executeStatements(statements, config);
    }

    private static void executeStatements(List<String> statements, DatabaseConfig config) throws SQLException {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                try {
                    statement.executeUpdate(sql);
                } catch (SQLException ex) {
                    if (isIgnorableMySqlSchemaError(config, sql, ex)) {
                        continue;
                    }
                    throw ex;
                }
            }
        }
    }

    private static boolean isIgnorableMySqlSchemaError(DatabaseConfig config, String sql, SQLException ex) {
        if (!config.isMySql()) {
            return false;
        }
        String normalizedSql = sql.toLowerCase();
        if (normalizedSql.startsWith("alter table") && normalizedSql.contains("add column")) {
            return ex.getErrorCode() == 1060 || "42S21".equals(ex.getSQLState());
        }
        if (normalizedSql.contains("add constraint") && normalizedSql.contains("check")) {
            return ex.getErrorCode() == 1061 || "42S21".equals(ex.getSQLState()) || ex.getErrorCode() == 3822;
        }
        if (normalizedSql.contains("create index") || normalizedSql.contains("add index")) {
            return ex.getErrorCode() == 1061 || "42000".equals(ex.getSQLState());
        }
        return false;
    }

    private static List<String> mysqlCreateStatements() {
        return List.of(
                """
                CREATE TABLE IF NOT EXISTS users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(50) NOT NULL UNIQUE,
                    password VARCHAR(255) NOT NULL,
                    full_name VARCHAR(100) NOT NULL,
                    email VARCHAR(100) NOT NULL UNIQUE,
                    role VARCHAR(20) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    created_at BIGINT NOT NULL,
                    updated_at BIGINT NOT NULL
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS auctions (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(200) NOT NULL,
                    description TEXT,
                    start_price DECIMAL(15, 2) NOT NULL,
                    current_highest_bid DECIMAL(15, 2) NOT NULL,
                    start_time BIGINT NOT NULL,
                    end_time BIGINT NOT NULL,
                    category VARCHAR(100),
                    image_source TEXT NULL,
                    image_data LONGBLOB NULL,
                    created_at BIGINT NOT NULL,
                    updated_at BIGINT NOT NULL,
                    seller_id INT NOT NULL,
                    winner_id INT NULL,
                    status VARCHAR(20) NOT NULL,
                    anti_sniping_extensions INT NOT NULL DEFAULT 0,
                    CONSTRAINT fk_auctions_seller FOREIGN KEY (seller_id) REFERENCES users(id),
                    CONSTRAINT fk_auctions_winner FOREIGN KEY (winner_id) REFERENCES users(id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS bids (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    auction_id INT NOT NULL,
                    bidder_id INT NOT NULL,
                    amount DECIMAL(15, 2) NOT NULL,
                    bid_time BIGINT NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    CONSTRAINT fk_bids_auction FOREIGN KEY (auction_id) REFERENCES auctions(id),
                    CONSTRAINT fk_bids_bidder FOREIGN KEY (bidder_id) REFERENCES users(id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS homepage_announcements (
                    id INT AUTO_INCREMENT PRIMARY KEY,
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
                """,
                """
                CREATE TABLE IF NOT EXISTS auto_bids (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    auction_id INT NOT NULL,
                    bidder_id INT NOT NULL,
                    increment DECIMAL(15, 2) NOT NULL,
                    max_price DECIMAL(15, 2) NOT NULL,
                    created_at BIGINT NOT NULL,
                    updated_at BIGINT NOT NULL,
                    CONSTRAINT fk_auto_bids_auction FOREIGN KEY (auction_id) REFERENCES auctions(id),
                    CONSTRAINT fk_auto_bids_bidder FOREIGN KEY (bidder_id) REFERENCES users(id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS wallets (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NOT NULL UNIQUE,
                    balance DECIMAL(15, 2) NOT NULL DEFAULT 0,
                    reserved_balance DECIMAL(15, 2) NOT NULL DEFAULT 0,
                    created_at BIGINT NOT NULL,
                    updated_at BIGINT NOT NULL,
                    CONSTRAINT fk_wallets_user FOREIGN KEY (user_id) REFERENCES users(id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS topup_transactions (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NOT NULL,
                    amount DECIMAL(15, 2) NOT NULL,
                    method VARCHAR(30) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    reference_code VARCHAR(100) NULL,
                    transaction_time BIGINT NOT NULL,
                    complete_at BIGINT NULL,
                    CONSTRAINT fk_topup_transactions_user FOREIGN KEY (user_id) REFERENCES users(id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS wallet_transactions (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NOT NULL,
                    type VARCHAR(20) NOT NULL,
                    amount DECIMAL(15, 2) NOT NULL,
                    auction_id INT NULL,
                    reference VARCHAR(100) NULL,
                    created_at BIGINT NOT NULL,
                    CONSTRAINT fk_wallet_tx_user FOREIGN KEY (user_id) REFERENCES users(id),
                    CONSTRAINT fk_wallet_tx_auction FOREIGN KEY (auction_id) REFERENCES auctions(id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS notifications (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NOT NULL,
                    title VARCHAR(255) NOT NULL,
                    content TEXT NOT NULL,
                    created_at BIGINT NOT NULL,
                );        
                """
        );
    }

    private static List<String> mysqlSyncStatements() {
        return List.of(
                "ALTER TABLE auctions ADD COLUMN image_data LONGBLOB NULL",
                "ALTER TABLE auctions ADD COLUMN anti_sniping_extensions INT NOT NULL DEFAULT 0",
                "ALTER TABLE auto_bids ADD COLUMN created_at BIGINT",
                "ALTER TABLE auto_bids ADD COLUMN updated_at BIGINT",
                "ALTER TABLE wallets ADD COLUMN reserved_balance DECIMAL(15, 2) NOT NULL DEFAULT 0",
                "UPDATE auto_bids SET created_at = COALESCE(created_at, id), updated_at = COALESCE(updated_at, created_at, id) WHERE created_at IS NULL OR updated_at IS NULL",
                "DROP TRIGGER IF EXISTS trigger_auto_bid",
                "DROP TRIGGER IF EXISTS trigger_auto_bid_on_new_rule",
                "DROP FUNCTION IF EXISTS handle_auto_bidding",
                "CREATE INDEX IF NOT EXISTS idx_topup_user_time ON topup_transactions(user_id, transaction_time DESC)",
                "ALTER TABLE wallets ADD CONSTRAINT chk_wallets_balance CHECK (balance >= 0)",
                "ALTER TABLE wallets ADD CONSTRAINT chk_wallets_reserved CHECK (reserved_balance >= 0)",
                "CREATE TABLE IF NOT EXISTS wallet_transactions (" +
                        "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                        "user_id INT NOT NULL," +
                        "type VARCHAR(20) NOT NULL," +
                        "amount DECIMAL(15, 2) NOT NULL," +
                        "auction_id INT NULL," +
                        "reference VARCHAR(100) NULL," +
                        "created_at BIGINT NOT NULL" +
                        ")",
                "CREATE INDEX IF NOT EXISTS idx_wallet_tx_user_time ON wallet_transactions(user_id, created_at DESC)"
        );
    }

    private static List<String> postgresCreateStatements() {
        return List.of(
                """
                CREATE TABLE IF NOT EXISTS users (
                    id INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    username VARCHAR(50) NOT NULL UNIQUE,
                    password VARCHAR(255) NOT NULL,
                    full_name VARCHAR(100) NOT NULL,
                    email VARCHAR(100) NOT NULL UNIQUE,
                    role VARCHAR(20) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    created_at BIGINT NOT NULL,
                    updated_at BIGINT NOT NULL
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS auctions (
                    id INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    name VARCHAR(200) NOT NULL,
                    description TEXT,
                    start_price DECIMAL(15, 2) NOT NULL,
                    current_highest_bid DECIMAL(15, 2) NOT NULL,
                    start_time BIGINT NOT NULL,
                    end_time BIGINT NOT NULL,
                    category VARCHAR(100),
                    image_source TEXT NULL,
                    image_data BYTEA NULL,
                    created_at BIGINT NOT NULL,
                    updated_at BIGINT NOT NULL,
                    seller_id INT NOT NULL,
                    winner_id INT NULL,
                    status VARCHAR(20) NOT NULL,
                    anti_sniping_extensions INT NOT NULL DEFAULT 0,
                    CONSTRAINT fk_auctions_seller FOREIGN KEY (seller_id) REFERENCES users(id),
                    CONSTRAINT fk_auctions_winner FOREIGN KEY (winner_id) REFERENCES users(id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS bids (
                    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    auction_id INT NOT NULL,
                    bidder_id INT NOT NULL,
                    amount DECIMAL(15, 2) NOT NULL,
                    bid_time BIGINT NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    CONSTRAINT fk_bids_auction FOREIGN KEY (auction_id) REFERENCES auctions(id),
                    CONSTRAINT fk_bids_bidder FOREIGN KEY (bidder_id) REFERENCES users(id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS homepage_announcements (
                    id INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
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
                """,
                """
                CREATE TABLE IF NOT EXISTS auto_bids (
                    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    auction_id INT NOT NULL,
                    bidder_id INT NOT NULL,
                    increment DECIMAL(15, 2) NOT NULL,
                    max_price DECIMAL(15, 2) NOT NULL,
                    created_at BIGINT NOT NULL,
                    updated_at BIGINT NOT NULL,
                    CONSTRAINT fk_auto_bids_auction FOREIGN KEY (auction_id) REFERENCES auctions(id),
                    CONSTRAINT fk_auto_bids_bidder FOREIGN KEY (bidder_id) REFERENCES users(id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS wallets (
                    id INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    user_id INT NOT NULL UNIQUE,
                    balance DECIMAL(15, 2) NOT NULL DEFAULT 0,
                    reserved_balance DECIMAL(15, 2) NOT NULL DEFAULT 0,
                    created_at BIGINT NOT NULL,
                    updated_at BIGINT NOT NULL,
                    CONSTRAINT fk_wallets_user FOREIGN KEY (user_id) REFERENCES users(id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS topup_transactions (
                    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    user_id INT NOT NULL,
                    amount DECIMAL(15, 2) NOT NULL,
                    method VARCHAR(30) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    reference_code VARCHAR(100) NULL,
                    transaction_time BIGINT NOT NULL,
                    complete_at BIGINT NULL,
                    CONSTRAINT fk_topup_transactions_user FOREIGN KEY (user_id) REFERENCES users(id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS wallet_transactions (
                    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    user_id INT NOT NULL,
                    type VARCHAR(20) NOT NULL,
                    amount DECIMAL(15, 2) NOT NULL,
                    auction_id INT NULL,
                    reference VARCHAR(100) NULL,
                    created_at BIGINT NOT NULL,
                    CONSTRAINT fk_wallet_tx_user FOREIGN KEY (user_id) REFERENCES users(id),
                    CONSTRAINT fk_wallet_tx_auction FOREIGN KEY (auction_id) REFERENCES auctions(id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS notifications (
                    id INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    user_id INT NOT NULL,
                    title VARCHAR(255) NOT NULL,
                    content TEXT NOT NULL,
                    created_at BIGINT NOT NULL,
                );
                """
        );
    }

    private static List<String> postgresSyncStatements() {
        return List.of(
                "ALTER TABLE auctions ADD COLUMN IF NOT EXISTS image_data BYTEA NULL",
                "ALTER TABLE auctions ADD COLUMN IF NOT EXISTS anti_sniping_extensions INT NOT NULL DEFAULT 0",
                "ALTER TABLE auto_bids ADD COLUMN IF NOT EXISTS created_at BIGINT",
                "ALTER TABLE auto_bids ADD COLUMN IF NOT EXISTS updated_at BIGINT",
                "ALTER TABLE wallets ADD COLUMN IF NOT EXISTS reserved_balance DECIMAL(15, 2) NOT NULL DEFAULT 0",
                "ALTER TABLE wallets ADD CONSTRAINT chk_wallets_balance CHECK (balance >= 0)",
                "ALTER TABLE wallets ADD CONSTRAINT chk_wallets_reserved CHECK (reserved_balance >= 0)",
                "UPDATE auto_bids SET created_at = COALESCE(created_at, id), updated_at = COALESCE(updated_at, created_at, id) WHERE created_at IS NULL OR updated_at IS NULL",
                "CREATE UNIQUE INDEX IF NOT EXISTS ux_auto_bids_auction_bidder ON auto_bids (auction_id, bidder_id)",
                "CREATE INDEX IF NOT EXISTS idx_topup_user_time ON topup_transactions(user_id, transaction_time DESC)",
                "CREATE INDEX IF NOT EXISTS idx_wallet_tx_user_time ON wallet_transactions(user_id, created_at DESC)",
                "DROP TRIGGER IF EXISTS trigger_auto_bid ON auctions",
                "DROP TRIGGER IF EXISTS trigger_auto_bid_on_new_rule ON auto_bids",
                "DROP FUNCTION IF EXISTS public.handle_auto_bidding()"
        );
    }
}
