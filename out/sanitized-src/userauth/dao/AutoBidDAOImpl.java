package userauth.dao;

import userauth.database.DatabaseConnection;
import userauth.model.AutoBid;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

// File note: Triển khai PostgreSQL cho DAO của module này.
// DAO PostgreSQL cho luáº­t auto-bid vÃ  timestamp cá»§a chÃºng.
public class AutoBidDAOImpl implements AutoBidDAO {
    // Khi lÆ°u má»›i, createdAt/updatedAt Ä‘Æ°á»£c ghi tháº³ng Ä‘á»ƒ DB sort á»•n Ä‘á»‹nh.
    private static final String INSERT_AUTOBID_SQL = """
            INSERT INTO auto_bids (
                auction_id, bidder_id, max_price, "increment", created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?)
            """;
    // Update chá»‰ Ä‘á»•i pháº§n rule vÃ  dáº¥u thá»i gian chá»‰nh sá»­a cuá»‘i.
    private static final String UPDATE_AUTOBID_SQL = """
            UPDATE auto_bids
            SET max_price = ?, "increment" = ?, updated_at = ?
            WHERE id = ?
            """;
    private static final String FIND_AUTOBID_BY_ID_SQL = """
            SELECT id, bidder_id, auction_id, max_price, "increment", created_at, updated_at
            FROM auto_bids
            WHERE id = ?
            """;
    private static final String FIND_AUTOBID_BY_AUCTION_BIDDER_SQL = """
            SELECT id, bidder_id, auction_id, max_price, "increment", created_at, updated_at
            FROM auto_bids
            WHERE auction_id = ? AND bidder_id = ?
            """;
    // Query nÃ y phá»¥c vá»¥ cÃ¡c xá»­ lÃ½ cáº§n xem táº¥t cáº£ auto-bid trong má»™t auction.
    private static final String FIND_AUTOBIDS_BY_AUCTION_SQL = """
            SELECT id, bidder_id, auction_id, max_price, "increment", created_at, updated_at
            FROM auto_bids
            WHERE auction_id = ?
            ORDER BY created_at, id
            """;
    private static final String FIND_ALL_USER_AUTOBID_SQL = """
            SELECT id, bidder_id, auction_id, max_price, "increment", created_at, updated_at
            FROM auto_bids
            WHERE bidder_id = ?
            ORDER BY created_at, id
            """;
    private static final String DELETE_AUTOBID_SQL = "DELETE FROM auto_bids WHERE id = ?";

    @Override
    public void saveAutoBid(AutoBid item) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_AUTOBID_SQL, Statement.RETURN_GENERATED_KEYS)) {
            bindAutoBidForInsert(statement, item);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    item.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to save the auto bid to PostgreSQL.", ex);
        }
    }

    @Override
    public void updateAutoBid(AutoBid item) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_AUTOBID_SQL)) {
            bindAutoBidForUpdate(statement, item);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to update the auto bid in PostgreSQL.", ex);
        }
    }

    @Override
    public void deleteAutoBid(int id) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(DELETE_AUTOBID_SQL)) {
                statement.setInt(1, id);
                statement.executeUpdate();
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to delete the auto bid in PostgreSQL.", ex);
        }
    }

    @Override
    public AutoBid findAutoBidById(int id) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_AUTOBID_BY_ID_SQL)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return mapAutobid(resultSet);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to find the auto bid in PostgreSQL.", ex);
        }
    }

    @Override
    public AutoBid findAutoBidByAuctionBidder(int auctionId, int bidderId) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_AUTOBID_BY_AUCTION_BIDDER_SQL)) {
            statement.setInt(1, auctionId);
            statement.setInt(2, bidderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return mapAutobid(resultSet);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to find the auto bid in PostgreSQL.", ex);
        }
    }

    @Override
    public List<AutoBid> findAllUserAutoBid(int bidderId) {
        List<AutoBid> autobids = new ArrayList<>();
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ALL_USER_AUTOBID_SQL)) {
            statement.setInt(1, bidderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    autobids.add(mapAutobid(resultSet));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to read all auto bids from PostgreSQL.", ex);
        }
        return autobids;
    }

    @Override
    public List<AutoBid> findAutoBidsByAuction(int auctionId) {
        List<AutoBid> autobids = new ArrayList<>();
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_AUTOBIDS_BY_AUCTION_SQL)) {
            statement.setInt(1, auctionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    autobids.add(mapAutobid(resultSet));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to read auto bids by auction from PostgreSQL.", ex);
        }
        return autobids;
    }

    // Gáº¯n dá»¯ liá»‡u Java vÃ o thá»© tá»± placeholder cá»§a cÃ¢u INSERT.
    private void bindAutoBidForInsert(PreparedStatement statement, AutoBid item) throws SQLException {
        statement.setInt(1, item.getAuctionId());
        statement.setInt(2, item.getBidderId());
        statement.setDouble(3, item.getMaxPrice());
        statement.setDouble(4, item.getIncrement());
        statement.setLong(5, item.getCreatedAt());
        statement.setLong(6, item.getUpdatedAt());
    }

    // Gáº¯n dá»¯ liá»‡u Java vÃ o thá»© tá»± placeholder cá»§a cÃ¢u UPDATE.
    private void bindAutoBidForUpdate(PreparedStatement statement, AutoBid item) throws SQLException {
        statement.setDouble(1, item.getMaxPrice());
        statement.setDouble(2, item.getIncrement());
        statement.setLong(3, item.getUpdatedAt());
        statement.setInt(4, item.getId());
    }

    // Dá»±ng AutoBid tá»« row DB Ä‘á»ƒ service xá»­ lÃ½ tiáº¿p.
    private AutoBid mapAutobid(ResultSet resultSet) throws SQLException {
        return new AutoBid(
                resultSet.getInt("id"),
                resultSet.getInt("auction_id"),
                resultSet.getInt("bidder_id"),
                resultSet.getDouble("max_price"),
                resultSet.getDouble("increment"),
                resultSet.getLong("created_at"),
                resultSet.getLong("updated_at")
        );
    }
}

