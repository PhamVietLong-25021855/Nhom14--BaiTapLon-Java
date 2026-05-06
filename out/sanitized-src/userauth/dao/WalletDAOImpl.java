package userauth.dao;

import userauth.database.DatabaseConnection;
import userauth.model.PaymentMethod;
import userauth.model.TopUpStatus;
import userauth.model.TopUpTransaction;
import userauth.model.Wallet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

// File note: Triển khai PostgreSQL cho DAO của module này.
public class WalletDAOImpl implements WalletDAO {
    private static final String SAVE_WALLET_SQL = """
            INSERT INTO wallets (user_id, balance, reserved_balance, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?)
            """;
    private static final String UPDATE_WALLET_SQL = """
            UPDATE wallets
            SET balance = ?, reserved_balance = ?, updated_at = ?
            WHERE id = ?
            """;
    private static final String FIND_WALLET_BY_USER_ID_SQL = """
            SELECT id, user_id, balance, reserved_balance, created_at, updated_at
            FROM wallets
            WHERE user_id = ?
            """;
    private static final String DELETE_WALLET_SQL = "DELETE FROM wallets WHERE id = ?";
    private static final String SAVE_TOPUP_TRANSACTION_SQL = """
            INSERT INTO topup_transactions (
                user_id, amount, method, status, reference_code, transaction_time, complete_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String UPDATE_TOPUP_TRANSACTION_SQL = """
            UPDATE topup_transactions
            SET status = ?, reference_code = ?, complete_at = ?
            WHERE id = ?
            """;
    private static final String FIND_TOPUP_TRANSACTION_BY_ID_SQL = """
            SELECT id, user_id, amount, method, status, reference_code, transaction_time, complete_at
            FROM topup_transactions
            WHERE id = ?
            """;
    private static final String FIND_TOPUP_BY_USER_SQL = """
            SELECT id, user_id, amount, method, status, reference_code, transaction_time, complete_at
            FROM topup_transactions
            WHERE user_id = ?
            ORDER BY transaction_time DESC
            """;
    private static final String FIND_ALL_PENDING_TOPUP_SQL = """
            SELECT id, user_id, amount, method, status, reference_code, transaction_time, complete_at
            FROM topup_transactions
            WHERE status = ?
            ORDER BY transaction_time DESC
            """;
    private static final String DELETE_TOPUP_TRANSACTION_SQL = "DELETE FROM topup_transactions WHERE id = ?";

    @Override
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
    public void deleteTopUpTransaction(int transactionId) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_TOPUP_TRANSACTION_SQL)) {
            statement.setInt(1, transactionId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to delete the top-up transaction in PostgreSQL.", ex);
        }
    }

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

