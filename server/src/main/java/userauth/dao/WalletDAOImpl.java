package userauth.dao;

import userauth.database.DatabaseConnection;
import userauth.exception.ValidationException;
import userauth.model.PaymentMethod;
import userauth.model.TopUpStatus;
import userauth.model.TopUpTransaction;
import userauth.model.Wallet;
import userauth.model.WalletTransaction;
import userauth.model.WalletTransactionType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

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
    private static final String SAVE_TOP_UP_SQL = """
            INSERT INTO topup_transactions (
                user_id, amount, method, status, reference_code, transaction_time, complete_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String UPDATE_TOP_UP_SQL = """
            UPDATE topup_transactions
            SET status = ?, reference_code = ?, complete_at = ?
            WHERE id = ?
            """;
    private static final String FIND_TOP_UP_BY_ID_SQL = """
            SELECT id, user_id, amount, method, status, reference_code, transaction_time, complete_at
            FROM topup_transactions
            WHERE id = ?
            """;
    private static final String FIND_TOP_UP_BY_USER_SQL = """
            SELECT id, user_id, amount, method, status, reference_code, transaction_time, complete_at
            FROM topup_transactions
            WHERE user_id = ?
            ORDER BY transaction_time DESC, id DESC
            """;
    private static final String SAVE_WALLET_TRANSACTION_SQL = """
            INSERT INTO wallet_transactions (user_id, type, amount, auction_id, reference, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
    private static final String FIND_WALLET_TRANSACTIONS_BY_USER_SQL = """
            SELECT id, user_id, type, amount, auction_id, reference, created_at
            FROM wallet_transactions
            WHERE user_id = ?
            ORDER BY created_at DESC
            """;

    @Override
    public int saveWallet(Wallet wallet) throws ValidationException {
        validateWallet(wallet);
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(SAVE_WALLET_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, wallet.getUserId());
            statement.setLong(2, wallet.getBalance());
            statement.setLong(3, wallet.getReservedBalance());
            statement.setLong(4, wallet.getCreatedAt());
            statement.setLong(5, wallet.getUpdatedAt());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    wallet.setId(generatedId);
                    return generatedId;
                }
            }
            throw new IllegalStateException("Unable to read generated wallet id.");
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to save wallet.", ex);
        }
    }

    @Override
    public void updateWallet(Wallet wallet) throws ValidationException {
        validateWallet(wallet);
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_WALLET_SQL)) {
            statement.setLong(1, wallet.getBalance());
            statement.setLong(2, wallet.getReservedBalance());
            statement.setLong(3, wallet.getUpdatedAt());
            statement.setInt(4, wallet.getId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to update wallet.", ex);
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
            throw new IllegalStateException("Unable to find wallet.", ex);
        }
    }

    @Override
    public int saveTopUpTransaction(TopUpTransaction transaction) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(SAVE_TOP_UP_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, transaction.getUserId());
            statement.setLong(2, transaction.getAmount());
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
                    int generatedId = generatedKeys.getInt(1);
                    transaction.setId(generatedId);
                    return generatedId;
                }
            }
            throw new IllegalStateException("Unable to read generated top-up id.");
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to save top-up transaction.", ex);
        }
    }

    @Override
    public void updateTopUpTransaction(TopUpTransaction transaction) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_TOP_UP_SQL)) {
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
            throw new IllegalStateException("Unable to update top-up transaction.", ex);
        }
    }

    @Override
    public TopUpTransaction findTopUpTransactionById(int transactionId) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_TOP_UP_BY_ID_SQL)) {
            statement.setInt(1, transactionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return mapTopUpTransaction(resultSet);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to find top-up transaction.", ex);
        }
    }

    @Override
    public List<TopUpTransaction> findTopUpTransactionsByUserId(int userId) {
        List<TopUpTransaction> transactions = new ArrayList<>();
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_TOP_UP_BY_USER_SQL)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    transactions.add(mapTopUpTransaction(resultSet));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to read top-up history.", ex);
        }
        return transactions;
    }

    @Override
    public void saveWalletTransaction(WalletTransaction transaction) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(SAVE_WALLET_TRANSACTION_SQL)) {
            statement.setInt(1, transaction.getUserId());
            statement.setString(2, transaction.getType().name());
            statement.setLong(3, transaction.getAmount());
            if (transaction.getAuctionId() == null) {
                statement.setNull(4, java.sql.Types.INTEGER);
            } else {
                statement.setInt(4, transaction.getAuctionId());
            }
            statement.setString(5, transaction.getReference());
            statement.setLong(6, transaction.getCreatedAt());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to save wallet transaction.", ex);
        }
    }

    @Override
    public List<WalletTransaction> findWalletTransactionsByUserId(int userId) {
        List<WalletTransaction> transactions = new ArrayList<>();
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_WALLET_TRANSACTIONS_BY_USER_SQL)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    transactions.add(mapWalletTransaction(resultSet));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to read wallet transactions.", ex);
        }
        return transactions;
    }

    private void validateWallet(Wallet wallet) throws ValidationException {
        if (wallet.getBalance() < 0) {
            throw new ValidationException("Wallet balance cannot be negative.");
        }
        if (wallet.getReservedBalance() < 0) {
            throw new ValidationException("Reserved balance cannot be negative.");
        }
        if (wallet.getReservedBalance() > wallet.getBalance()) {
            throw new ValidationException("Reserved balance cannot exceed the wallet balance.");
        }
    }

    private Wallet mapWallet(ResultSet resultSet) throws SQLException {
        return new Wallet(
                resultSet.getInt("id"),
                resultSet.getInt("user_id"),
                resultSet.getLong("balance"),
                resultSet.getLong("reserved_balance"),
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
                resultSet.getLong("amount"),
                PaymentMethod.valueOf(resultSet.getString("method")),
                TopUpStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("reference_code"),
                resultSet.getLong("transaction_time"),
                completeAt
        );
    }

    private WalletTransaction mapWalletTransaction(ResultSet resultSet) throws SQLException {
        return new WalletTransaction(
                resultSet.getInt("id"),
                resultSet.getInt("user_id"),
                WalletTransactionType.valueOf(resultSet.getString("type")),
                resultSet.getLong("amount"),
                (Integer) resultSet.getObject("auction_id"),
                resultSet.getString("reference"),
                resultSet.getLong("created_at")
        );
    }
}
