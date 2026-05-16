package uet.auctionsystem.dao;

import uet.auctionsystem.database.DatabaseConnection;
import uet.auctionsystem.model.PaymentMethod;
import uet.auctionsystem.model.TopUpStatus;
import uet.auctionsystem.model.TopUpTransaction;
import uet.auctionsystem.model.Wallet;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

// Ghi chu file: File DAO; dinh nghia hoac trien khai cac thao tac doc ghi du lieu voi database.
// Khai bao lop WalletDAOImpl; phu trach hop dong hoac truy cap du lieu cho database.
public class WalletDAOImpl implements WalletDAO {
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho sql.
    private static final String SAVE_WALLET_SQL = """
            INSERT INTO wallets (user_id, balance, reserved_balance, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?)
            """;
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho sql.
    private static final String UPDATE_WALLET_SQL = """
            UPDATE wallets
            SET balance = ?, reserved_balance = ?, updated_at = ?
            WHERE id = ?
            """;
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho sql.
    private static final String FIND_WALLET_BY_USER_ID_SQL = """
            SELECT id, user_id, balance, reserved_balance, created_at, updated_at
            FROM wallets
            WHERE user_id = ?
            """;
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho sql.
    private static final String DELETE_WALLET_SQL = "DELETE FROM wallets WHERE id = ?";
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho sql.
    private static final String SAVE_TOPUP_TRANSACTION_SQL = """
            INSERT INTO topup_transactions (
                user_id, amount, method, status, reference_code, transaction_time, complete_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho sql.
    private static final String UPDATE_TOPUP_TRANSACTION_SQL = """
            UPDATE topup_transactions
            SET status = ?, reference_code = ?, complete_at = ?
            WHERE id = ?
            """;
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho sql.
    private static final String FIND_TOPUP_TRANSACTION_BY_ID_SQL = """
            SELECT id, user_id, amount, method, status, reference_code, transaction_time, complete_at
            FROM topup_transactions
            WHERE id = ?
            """;
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho sql.
    private static final String FIND_TOPUP_BY_USER_SQL = """
            SELECT id, user_id, amount, method, status, reference_code, transaction_time, complete_at
            FROM topup_transactions
            WHERE user_id = ?
            ORDER BY transaction_time DESC
            """;
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho sql.
    private static final String FIND_ALL_PENDING_TOPUP_SQL = """
            SELECT id, user_id, amount, method, status, reference_code, transaction_time, complete_at
            FROM topup_transactions
            WHERE status = ?
            ORDER BY transaction_time DESC
            """;
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho sql.
    private static final String DELETE_TOPUP_TRANSACTION_SQL = "DELETE FROM topup_transactions WHERE id = ?";

    @Override
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac save wallet.
    public int saveWallet(Wallet wallet) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(SAVE_WALLET_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, wallet.getUserId());
            statement.setDouble(2, wallet.getBalance());
            statement.setDouble(3, wallet.getReservedBalance());
            statement.setLong(4, wallet.getCreatedAt());
            statement.setLong(5, wallet.getUpdatedAt());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
            throw new IllegalStateException("Unable to read the generated wallet id from PostgreSQL.");
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to save the wallet to PostgreSQL.", ex);
        }
    }

    @Override
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac update wallet.
    public void updateWallet(Wallet wallet) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_WALLET_SQL)) {
            statement.setDouble(1, wallet.getBalance());
            statement.setDouble(2, wallet.getReservedBalance());
            statement.setLong(3, wallet.getUpdatedAt());
            statement.setInt(4, wallet.getId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to update the wallet in PostgreSQL.", ex);
        }
    }

    @Override
    // Phuong thuc: lay hoac doc du lieu cho thao tac find wallet by user id.
    public Wallet findWalletByUserId(int userId) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_WALLET_BY_USER_ID_SQL)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return mapWallet(resultSet);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to find the wallet in PostgreSQL.", ex);
        }
    }

    @Override
    // Phuong thuc: huy, xoa, dong hoac don trang thai cho thao tac delete wallet.
    public void deleteWallet(int walletId) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_WALLET_SQL)) {
            statement.setInt(1, walletId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to delete the wallet in PostgreSQL.", ex);
        }
    }

    @Override
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac save top up transaction.
    public int saveTopUpTransaction(TopUpTransaction transaction) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(SAVE_TOPUP_TRANSACTION_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, transaction.getUserId());
            statement.setDouble(2, transaction.getAmount());
            statement.setString(3, transaction.getMethod().name());
            statement.setString(4, transaction.getStatus().name());
            statement.setString(5, transaction.getReferenceCode());
            statement.setLong(6, transaction.getTransactionTime());
            if (transaction.getCompleteAt() == null) {
                statement.setNull(7, java.sql.Types.BIGINT);
            } else {
                statement.setLong(7, transaction.getCompleteAt());
            }
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
            throw new IllegalStateException("Unable to read the generated top-up transaction id from PostgreSQL.");
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to save the top-up transaction to PostgreSQL.", ex);
        }
    }

    @Override
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac update top up transaction.
    public void updateTopUpTransaction(TopUpTransaction transaction) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_TOPUP_TRANSACTION_SQL)) {
            statement.setString(1, transaction.getStatus().name());
            statement.setString(2, transaction.getReferenceCode());
            if (transaction.getCompleteAt() == null) {
                statement.setNull(3, java.sql.Types.BIGINT);
            } else {
                statement.setLong(3, transaction.getCompleteAt());
            }
            statement.setInt(4, transaction.getId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to update the top-up transaction in PostgreSQL.", ex);
        }
    }

    @Override
    // Phuong thuc: lay hoac doc du lieu cho thao tac find top up transaction by id.
    public TopUpTransaction findTopUpTransactionById(int transactionId) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_TOPUP_TRANSACTION_BY_ID_SQL)) {
            statement.setInt(1, transactionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return mapTopUpTransaction(resultSet);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to find the top-up transaction in PostgreSQL.", ex);
        }
    }

    @Override
    // Phuong thuc: lay hoac doc du lieu cho thao tac find top up transactions by user id.
    public List<TopUpTransaction> findTopUpTransactionsByUserId(int userId) {
        List<TopUpTransaction> transactions = new ArrayList<>();

        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_TOPUP_BY_USER_SQL)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    transactions.add(mapTopUpTransaction(resultSet));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to read top-up history from PostgreSQL.", ex);
        }

        return transactions;
    }

    @Override
    // Phuong thuc: lay hoac doc du lieu cho thao tac find all pending transactions.
    public List<TopUpTransaction> findAllPendingTransactions() {
        List<TopUpTransaction> transactions = new ArrayList<>();

        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ALL_PENDING_TOPUP_SQL)) {
            statement.setString(1, TopUpStatus.PENDING.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    transactions.add(mapTopUpTransaction(resultSet));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to read pending top-up transactions from PostgreSQL.", ex);
        }

        return transactions;
    }

    @Override
    // Phuong thuc: huy, xoa, dong hoac don trang thai cho thao tac delete top up transaction.
    public void deleteTopUpTransaction(int transactionId) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_TOPUP_TRANSACTION_SQL)) {
            statement.setInt(1, transactionId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to delete the top-up transaction in PostgreSQL.", ex);
        }
    }
    // Phuong thuc: bien doi du lieu cho thao tac map wallet.
    private Wallet mapWallet(ResultSet resultSet) throws SQLException {
        return new Wallet(
                resultSet.getInt("id"),
                resultSet.getInt("user_id"),
                resultSet.getDouble("balance"),
                resultSet.getDouble("reserved_balance"),
                resultSet.getLong("created_at"),
                resultSet.getLong("updated_at")
        );
    }
    // Phuong thuc: bien doi du lieu cho thao tac map top up transaction.
    private TopUpTransaction mapTopUpTransaction(ResultSet resultSet) throws SQLException {
        Long completeAt = resultSet.getObject("complete_at") == null
                ? null
                : resultSet.getLong("complete_at");
        return new TopUpTransaction(
                resultSet.getInt("id"),
                resultSet.getInt("user_id"),
                resultSet.getDouble("amount"),
                PaymentMethod.valueOf(resultSet.getString("method")),
                TopUpStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("reference_code"),
                resultSet.getLong("transaction_time"),
                completeAt
        );
    }
}
