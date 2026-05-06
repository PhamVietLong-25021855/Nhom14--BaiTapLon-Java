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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
    private static final String DELETE_USER_SQL = "DELETE FROM users WHERE id = ?";
    private static final String FIND_AUCTION_IDS_BY_SELLER_SQL = "SELECT id FROM auctions WHERE seller_id = ? ORDER BY id";
    private static final String FIND_AUCTION_IDS_BY_BIDDER_SQL = "SELECT DISTINCT auction_id FROM bids WHERE bidder_id = ? ORDER BY auction_id";
    private static final String FIND_AUCTION_IDS_BY_WINNER_SQL = "SELECT id FROM auctions WHERE winner_id = ? ORDER BY id";
    private static final String DELETE_AUTO_BIDS_BY_BIDDER_SQL = "DELETE FROM auto_bids WHERE bidder_id = ?";
    private static final String DELETE_BIDS_BY_BIDDER_SQL = "DELETE FROM bids WHERE bidder_id = ?";
    private static final String DELETE_HOMEPAGE_ANNOUNCEMENTS_BY_AUTHOR_SQL = "DELETE FROM homepage_announcements WHERE author_id = ?";
    private static final String DELETE_HOMEPAGE_ANNOUNCEMENTS_BY_LINKED_AUCTION_SQL = "DELETE FROM homepage_announcements WHERE linked_auction_id = ?";
    private static final String DELETE_AUTO_BIDS_BY_AUCTION_SQL = "DELETE FROM auto_bids WHERE auction_id = ?";
    private static final String DELETE_BIDS_BY_AUCTION_SQL = "DELETE FROM bids WHERE auction_id = ?";
    private static final String DELETE_AUCTION_SQL = "DELETE FROM auctions WHERE id = ?";
    private static final String FIND_AUCTION_START_PRICE_SQL = "SELECT start_price FROM auctions WHERE id = ?";
    private static final String FIND_TOP_BID_FOR_AUCTION_SQL = """
            SELECT bidder_id, amount
            FROM bids
            WHERE auction_id = ?
            ORDER BY amount DESC, bid_time DESC, id DESC
            LIMIT 1
            """;
    private static final String UPDATE_AUCTION_RECALC_SQL = """
            UPDATE auctions
            SET current_highest_bid = ?, winner_id = ?, updated_at = ?
            WHERE id = ?
            """;

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
    public void deleteById(int userId) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection()) {
            connection.setAutoCommit(false);
            try {
                List<Integer> sellerAuctionIds = findAuctionIds(connection, FIND_AUCTION_IDS_BY_SELLER_SQL, userId);
                Set<Integer> affectedAuctionIds = new LinkedHashSet<>(findAuctionIds(connection, FIND_AUCTION_IDS_BY_BIDDER_SQL, userId));
                affectedAuctionIds.addAll(findAuctionIds(connection, FIND_AUCTION_IDS_BY_WINNER_SQL, userId));
                affectedAuctionIds.removeAll(sellerAuctionIds);

                executeSingleIdStatement(connection, DELETE_HOMEPAGE_ANNOUNCEMENTS_BY_AUTHOR_SQL, userId);

                for (Integer auctionId : sellerAuctionIds) {
                    executeSingleIdStatement(connection, DELETE_HOMEPAGE_ANNOUNCEMENTS_BY_LINKED_AUCTION_SQL, auctionId);
                    executeSingleIdStatement(connection, DELETE_AUTO_BIDS_BY_AUCTION_SQL, auctionId);
                    executeSingleIdStatement(connection, DELETE_BIDS_BY_AUCTION_SQL, auctionId);
                    executeSingleIdStatement(connection, DELETE_AUCTION_SQL, auctionId);
                }

                executeSingleIdStatement(connection, DELETE_AUTO_BIDS_BY_BIDDER_SQL, userId);
                executeSingleIdStatement(connection, DELETE_BIDS_BY_BIDDER_SQL, userId);

                for (Integer auctionId : affectedAuctionIds) {
                    recalculateAuctionAfterBidDeletion(connection, auctionId);
                }

                executeSingleIdStatement(connection, DELETE_USER_SQL, userId);
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to delete the user from PostgreSQL.", ex);
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

    private List<Integer> findAuctionIds(Connection connection, String sql, int userId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(resultSet.getInt(1));
                }
            }
        }
        return ids;
    }

    private void executeSingleIdStatement(Connection connection, String sql, int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private void recalculateAuctionAfterBidDeletion(Connection connection, int auctionId) throws SQLException {
        Double startPrice = findAuctionStartPrice(connection, auctionId);
        if (startPrice == null) {
            return;
        }

        Integer winnerId = null;
        double currentHighestBid = startPrice;
        try (PreparedStatement statement = connection.prepareStatement(FIND_TOP_BID_FOR_AUCTION_SQL)) {
            statement.setInt(1, auctionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    winnerId = resultSet.getInt("bidder_id");
                    currentHighestBid = resultSet.getDouble("amount");
                }
            }
        }

        try (PreparedStatement statement = connection.prepareStatement(UPDATE_AUCTION_RECALC_SQL)) {
            statement.setDouble(1, currentHighestBid);
            if (winnerId == null || winnerId <= 0) {
                statement.setNull(2, java.sql.Types.INTEGER);
            } else {
                statement.setInt(2, winnerId);
            }
            statement.setLong(3, System.currentTimeMillis());
            statement.setInt(4, auctionId);
            statement.executeUpdate();
        }
    }

    private Double findAuctionStartPrice(Connection connection, int auctionId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(FIND_AUCTION_START_PRICE_SQL)) {
            statement.setInt(1, auctionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return resultSet.getDouble("start_price");
            }
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
