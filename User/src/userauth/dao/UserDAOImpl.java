package userauth.dao;

import userauth.database.DatabaseConnection;
import userauth.model.Admin;
import userauth.model.Bidder;
import userauth.model.Role;
import userauth.model.Seller;
import userauth.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class UserDAOImpl implements UserDAO {
    private static final String INSERT_SQL = """
            INSERT INTO users (username, password, full_name, email, role, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String UPDATE_SQL = """
            UPDATE users
            SET username = ?, password = ?, full_name = ?, email = ?, role = ?, status = ?, updated_at = ?
            WHERE id = ?
            """;
    private static final String FIND_BY_ID_SQL = """
            SELECT id, username, password, full_name, email, role, status, created_at, updated_at
            FROM users
            WHERE id = ?
            """;
    private static final String FIND_BY_USERNAME_SQL = """
            SELECT id, username, password, full_name, email, role, status, created_at, updated_at
            FROM users
            WHERE LOWER(username) = LOWER(?)
            """;
    private static final String FIND_BY_EMAIL_SQL = """
            SELECT id, username, password, full_name, email, role, status, created_at, updated_at
            FROM users
            WHERE LOWER(email) = LOWER(?)
            """;
    private static final String FIND_ALL_SQL = """
            SELECT id, username, password, full_name, email, role, status, created_at, updated_at
            FROM users
            ORDER BY id
            """;
    private static final String FIND_SELLER_AUCTIONS_SQL = """
            SELECT id
            FROM auctions
            WHERE seller_id = ?
            ORDER BY id
            """;
    private static final String UNLINK_HOMEPAGE_AUCTION_SQL = """
            UPDATE homepage_announcements
            SET linked_auction_id = NULL, updated_at = ?
            WHERE linked_auction_id = ?
            """;
    private static final String DELETE_AUTO_BIDS_BY_AUCTION_SQL = "DELETE FROM auto_bids WHERE auction_id = ?";
    private static final String DELETE_BIDS_BY_AUCTION_SQL = "DELETE FROM bids WHERE auction_id = ?";
    private static final String DELETE_AUCTION_SQL = "DELETE FROM auctions WHERE id = ?";
    private static final String FIND_AFFECTED_AUCTIONS_BY_BIDDER_SQL = """
            SELECT DISTINCT auction_id
            FROM bids
            WHERE bidder_id = ?
            ORDER BY auction_id
            """;
    private static final String DELETE_AUTO_BIDS_BY_BIDDER_SQL = "DELETE FROM auto_bids WHERE bidder_id = ?";
    private static final String DELETE_BIDS_BY_BIDDER_SQL = "DELETE FROM bids WHERE bidder_id = ?";
    private static final String FIND_HIGHEST_BID_SQL = """
            SELECT bidder_id, amount
            FROM bids
            WHERE auction_id = ?
            ORDER BY amount DESC, bid_time DESC, id DESC
            LIMIT 1
            """;
    private static final String FIND_AUCTION_START_PRICE_SQL = """
            SELECT start_price
            FROM auctions
            WHERE id = ?
            """;
    private static final String UPDATE_AUCTION_BID_STATE_SQL = """
            UPDATE auctions
            SET current_highest_bid = ?, winner_id = ?, updated_at = ?
            WHERE id = ?
            """;
    private static final String DELETE_HOMEPAGE_BY_AUTHOR_SQL = "DELETE FROM homepage_announcements WHERE author_id = ?";
    private static final String DELETE_TOPUPS_BY_USER_SQL = "DELETE FROM topup_transactions WHERE user_id = ?";
    private static final String DELETE_WALLET_BY_USER_SQL = "DELETE FROM wallets WHERE user_id = ?";
    private static final String DELETE_USER_SQL = "DELETE FROM users WHERE id = ?";

    @Override
    public void save(User user) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPassword());
            statement.setString(3, user.getFullName());
            statement.setString(4, user.getEmail());
            statement.setString(5, user.getRoleName());
            statement.setString(6, user.getStatus());
            statement.setLong(7, user.getCreatedAt());
            statement.setLong(8, user.getUpdatedAt());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to save the user to PostgreSQL.", ex);
        }
    }

    @Override
    public void update(User user) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPassword());
            statement.setString(3, user.getFullName());
            statement.setString(4, user.getEmail());
            statement.setString(5, user.getRoleName());
            statement.setString(6, user.getStatus());
            statement.setLong(7, user.getUpdatedAt());
            statement.setInt(8, user.getId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to update the user in PostgreSQL.", ex);
        }
    }

    @Override
    public void delete(int userId) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                long now = System.currentTimeMillis();

                for (int auctionId : findAuctionIdsBySeller(connection, userId)) {
                    unlinkHomepageAnnouncements(connection, auctionId, now);
                    deleteAutoBidsByAuction(connection, auctionId);
                    deleteBidsByAuction(connection, auctionId);
                    deleteAuction(connection, auctionId);
                }

                List<Integer> affectedAuctionIds = findAffectedAuctionIdsByBidder(connection, userId);
                deleteAutoBidsByBidder(connection, userId);
                deleteBidsByBidder(connection, userId);
                for (int auctionId : affectedAuctionIds) {
                    recalculateAuctionBidState(connection, auctionId, now);
                }

                deleteHomepageAnnouncementsByAuthor(connection, userId);
                deleteTopUpsByUser(connection, userId);
                deleteWalletByUser(connection, userId);
                deleteUserById(connection, userId);

                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to delete the user in PostgreSQL.", ex);
        }
    }

    @Override
    public User findById(int userId) {
        if (userId <= 0) {
            return null;
        }

        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return mapUser(resultSet);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to find the user by id in PostgreSQL.", ex);
        }
    }

    @Override
    public User findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }

        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_USERNAME_SQL)) {
            statement.setString(1, username.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return mapUser(resultSet);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to find the user by username in PostgreSQL.", ex);
        }
    }

    @Override
    public User findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_EMAIL_SQL)) {
            statement.setString(1, email.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return mapUser(resultSet);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to find the user by email in PostgreSQL.", ex);
        }
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();

        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                users.add(mapUser(resultSet));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to read the user list from PostgreSQL.", ex);
        }

        return users;
    }

    private List<Integer> findAuctionIdsBySeller(Connection connection, int sellerId) throws SQLException {
        List<Integer> auctionIds = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(FIND_SELLER_AUCTIONS_SQL)) {
            statement.setInt(1, sellerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    auctionIds.add(resultSet.getInt("id"));
                }
            }
        }
        return auctionIds;
    }

    private void unlinkHomepageAnnouncements(Connection connection, int auctionId, long now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UNLINK_HOMEPAGE_AUCTION_SQL)) {
            statement.setLong(1, now);
            statement.setInt(2, auctionId);
            statement.executeUpdate();
        }
    }

    private List<Integer> findAffectedAuctionIdsByBidder(Connection connection, int bidderId) throws SQLException {
        List<Integer> auctionIds = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(FIND_AFFECTED_AUCTIONS_BY_BIDDER_SQL)) {
            statement.setInt(1, bidderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    auctionIds.add(resultSet.getInt("auction_id"));
                }
            }
        }
        return auctionIds;
    }

    private void deleteAutoBidsByAuction(Connection connection, int auctionId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(DELETE_AUTO_BIDS_BY_AUCTION_SQL)) {
            statement.setInt(1, auctionId);
            statement.executeUpdate();
        }
    }

    private void deleteBidsByAuction(Connection connection, int auctionId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(DELETE_BIDS_BY_AUCTION_SQL)) {
            statement.setInt(1, auctionId);
            statement.executeUpdate();
        }
    }

    private void deleteAuction(Connection connection, int auctionId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(DELETE_AUCTION_SQL)) {
            statement.setInt(1, auctionId);
            statement.executeUpdate();
        }
    }

    private void deleteAutoBidsByBidder(Connection connection, int bidderId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(DELETE_AUTO_BIDS_BY_BIDDER_SQL)) {
            statement.setInt(1, bidderId);
            statement.executeUpdate();
        }
    }

    private void deleteBidsByBidder(Connection connection, int bidderId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(DELETE_BIDS_BY_BIDDER_SQL)) {
            statement.setInt(1, bidderId);
            statement.executeUpdate();
        }
    }

    private void recalculateAuctionBidState(Connection connection, int auctionId, long now) throws SQLException {
        Double highestBid = null;
        Integer winnerId = null;

        try (PreparedStatement highestBidStatement = connection.prepareStatement(FIND_HIGHEST_BID_SQL)) {
            highestBidStatement.setInt(1, auctionId);
            try (ResultSet resultSet = highestBidStatement.executeQuery()) {
                if (resultSet.next()) {
                    highestBid = resultSet.getDouble("amount");
                    winnerId = resultSet.getInt("bidder_id");
                }
            }
        }

        if (highestBid == null) {
            try (PreparedStatement startPriceStatement = connection.prepareStatement(FIND_AUCTION_START_PRICE_SQL)) {
                startPriceStatement.setInt(1, auctionId);
                try (ResultSet resultSet = startPriceStatement.executeQuery()) {
                    if (!resultSet.next()) {
                        return;
                    }
                    highestBid = resultSet.getDouble("start_price");
                }
            }
        }

        try (PreparedStatement updateStatement = connection.prepareStatement(UPDATE_AUCTION_BID_STATE_SQL)) {
            updateStatement.setDouble(1, highestBid);
            if (winnerId == null) {
                updateStatement.setNull(2, Types.INTEGER);
            } else {
                updateStatement.setInt(2, winnerId);
            }
            updateStatement.setLong(3, now);
            updateStatement.setInt(4, auctionId);
            updateStatement.executeUpdate();
        }
    }

    private void deleteHomepageAnnouncementsByAuthor(Connection connection, int authorId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(DELETE_HOMEPAGE_BY_AUTHOR_SQL)) {
            statement.setInt(1, authorId);
            statement.executeUpdate();
        }
    }

    private void deleteTopUpsByUser(Connection connection, int userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(DELETE_TOPUPS_BY_USER_SQL)) {
            statement.setInt(1, userId);
            statement.executeUpdate();
        }
    }

    private void deleteWalletByUser(Connection connection, int userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(DELETE_WALLET_BY_USER_SQL)) {
            statement.setInt(1, userId);
            statement.executeUpdate();
        }
    }

    private void deleteUserById(Connection connection, int userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(DELETE_USER_SQL)) {
            statement.setInt(1, userId);
            statement.executeUpdate();
        }
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        String username = resultSet.getString("username");
        String password = resultSet.getString("password");
        String fullName = resultSet.getString("full_name");
        String email = resultSet.getString("email");
        Role role = Role.valueOf(resultSet.getString("role").trim().toUpperCase());
        String status = resultSet.getString("status");
        long createdAt = resultSet.getLong("created_at");
        long updatedAt = resultSet.getLong("updated_at");

        return switch (role) {
            case ADMIN -> new Admin(id, username, password, fullName, email, status, createdAt, updatedAt);
            case SELLER -> new Seller(id, username, password, fullName, email, status, createdAt, updatedAt);
            case BIDDER -> new Bidder(id, username, password, fullName, email, status, createdAt, updatedAt);
        };
    }
}
