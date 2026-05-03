package userauth.dao;

import userauth.database.DatabaseConnection;
import userauth.model.Wallet;
import userauth.model.TopUpTransaction;
import userauth.model.PaymentMethod;
import userauth.model.TopUpStatus;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WalletDAOImpl implements WalletDAO {

    private static final String SAVE_WALLET_SQL =
            "INSERT INTO wallets (user_id, balance, created_at, updated_at) VALUES (?, ?, ?, ?)";

    private static final String UPDATE_WALLET_SQL =
            "UPDATE wallets SET balance = ?, updated_at = ? WHERE id = ?";

    private static final String FIND_WALLET_BY_USER_ID_SQL =
            "SELECT id, user_id, balance, created_at, updated_at FROM wallets WHERE user_id = ?";

    private static final String DELETE_WALLET_SQL =
            "DELETE FROM wallets WHERE id = ?";

    private static final String SAVE_TOPUP_TRANSACTION_SQL =
            "INSERT INTO topup_transactions (user_id, amount, method, status, reference_code, transaction_time, complete_at) VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_TOPUP_TRANSACTION_SQL =
            "UPDATE topup_transactions SET status = ?, reference_code = ?, complete_at = ? WHERE id = ?";

    private static final String FIND_TOPUP_TRANSACTION_BY_ID_SQL =
            "SELECT id, user_id, amount, method, status, reference_code, transaction_time, complete_at FROM topup_transactions WHERE id = ?";

    private static final String FIND_TOPUP_BY_USER_SQL =
            "SELECT id, user_id, amount, method, status, reference_code, transaction_time, complete_at FROM topup_transactions WHERE user_id = ? ORDER BY transaction_time DESC";

    private static final String FIND_ALL_PENDING_TOPUP_SQL =
            "SELECT id, user_id, amount, method, status, reference_code, transaction_time, complete_at FROM topup_transactions WHERE status = ?";

    private static final String DELETE_TOPUP_TRANSACTION_SQL =
            "DELETE FROM topup_transactions WHERE id = ?";

    @Override
    public int saveWallet(Wallet wallet) {
        try (Connection conn = DatabaseConnection.openDatabaseConnection();
             PreparedStatement stmt = conn.prepareStatement(SAVE_WALLET_SQL, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, wallet.getUserID());
            stmt.setDouble(2, wallet.getBalance());
            stmt.setLong(3, wallet.getCreatedAt());
            stmt.setLong(4, wallet.getUpdatedAt());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
            return -1;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to save the wallet to PostgreSQL.", e);
        }
    }

    @Override
    public void updateWallet(Wallet wallet) {
        try (Connection conn = DatabaseConnection.openDatabaseConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_WALLET_SQL)) {
            stmt.setDouble(1, wallet.getBalance());
            stmt.setLong(2, wallet.getUpdatedAt());
            stmt.setInt(3, wallet.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to update the wallet in PostgreSQL.", e);
        }
    }

    @Override
    public Wallet findWalletByUserId(int userId) {
        try (Connection conn = DatabaseConnection.openDatabaseConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_WALLET_BY_USER_ID_SQL)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapWallet(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to find the wallet in PostgreSQL.", e);
        }
    }

    @Override
    public void deleteWallet(int walledId) {
        try (Connection conn = DatabaseConnection.openDatabaseConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_WALLET_SQL)) {
            stmt.setInt(1, walledId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to delete the wallet in PostgreSQL.", e);
        }
    }

    @Override
    public int saveTopUpTransaction(TopUpTransaction transaction) {
        try (Connection conn = DatabaseConnection.openDatabaseConnection();
             PreparedStatement stmt = conn.prepareStatement(SAVE_TOPUP_TRANSACTION_SQL, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, transaction.getUserID());
            stmt.setDouble(2, transaction.getAmount());
            stmt.setString(3, transaction.getMethod().name());
            stmt.setString(4, transaction.getStatus().name());
            stmt.setString(5, transaction.getReferenceCode());
            stmt.setLong(6, transaction.getTransactionTime());
            stmt.setObject(7, transaction.getCompleteAt());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
            return -1;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to save top-up transaction to PostgreSQL.", e);
        }
    }

    @Override
    public void updateTopUpTransaction(TopUpTransaction transaction) {
        try (Connection conn = DatabaseConnection.openDatabaseConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_TOPUP_TRANSACTION_SQL)) {
            stmt.setString(1, transaction.getStatus().name());
            stmt.setString(2, transaction.getReferenceCode());
            stmt.setObject(3, transaction.getCompleteAt());
            stmt.setInt(4, transaction.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to update top-up transaction in PostgreSQL.", e);
        }
    }

    @Override
    public TopUpTransaction findTopUpTransactionById(int transactionId) {
        try (Connection conn = DatabaseConnection.openDatabaseConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_TOPUP_TRANSACTION_BY_ID_SQL)) {
            stmt.setInt(1, transactionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapTopUpTransaction(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to find top-up transaction in PostgreSQL.", e);
        }
    }

    @Override
    public List<TopUpTransaction> findTopUpTransactionsByUserId(int userId) {
        List<TopUpTransaction> transactions = new ArrayList<>();
        try (Connection conn = DatabaseConnection.openDatabaseConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_TOPUP_BY_USER_SQL)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapTopUpTransaction(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to read top-up transactions from PostgreSQL.", e);
        }
        return transactions;
    }

    @Override
    public List<TopUpTransaction> findAllPendingTransactions() {
        List<TopUpTransaction> transactions = new ArrayList<>();
        try (Connection conn = DatabaseConnection.openDatabaseConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_ALL_PENDING_TOPUP_SQL)) {
            stmt.setString(1, TopUpStatus.PENDING.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapTopUpTransaction(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to read pending top-up transactions from PostgreSQL.", e);
        }
        return transactions;
    }

    @Override
    public void deleteTopUpTransaction(int transactionID) {
        try (Connection conn = DatabaseConnection.openDatabaseConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_TOPUP_TRANSACTION_SQL)) {
            stmt.setInt(1, transactionID);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to delete top-up transaction in PostgreSQL.", e);
        }
    }

    private Wallet mapWallet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int userId = rs.getInt("user_id");
        double balance = rs.getDouble("balance");
        long createdAt = rs.getLong("created_at");
        long updatedAt = rs.getLong("updated_at");
        return new Wallet(id, userId, balance, createdAt, updatedAt);
    }

    // map to object JAVA;
    private TopUpTransaction mapTopUpTransaction(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int userId = rs.getInt("user_id");
        double amount = rs.getDouble("amount");
        PaymentMethod method = PaymentMethod.valueOf(rs.getString("method"));
        TopUpStatus status = TopUpStatus.valueOf(rs.getString("status"));
        String referenceCode = rs.getString("reference_code");
        long transactionTime = rs.getLong("transaction_time");
        Long completeAt = rs.getLong("complete_at");
        if (rs.wasNull()) completeAt = null;
        return new TopUpTransaction(id, userId, amount, method, status, referenceCode, transactionTime, completeAt);
    }
}
