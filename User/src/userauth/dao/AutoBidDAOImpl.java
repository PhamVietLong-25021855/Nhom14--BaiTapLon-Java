package userauth.dao;

import userauth.database.DatabaseConnection;
import userauth.model.AuctionItem;
import userauth.model.AutoBid;
import userauth.model.BidTransaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AutoBidDAOImpl implements AutoBidDAO{
    private static final String INSERT_AUTOBID_SQL = """
            INSERT INTO auto_bids (
                auction_id, bidder_id, max_price, "increment"
            )
            VALUES (?, ?, ?, ?)
            """;
    private static final String UPDATE_AUTOBID_SQL = """
            UPDATE auto_bids
            SET max_price = ?, "increment" = ?
            WHERE id = ?
            """;
    private static final String FIND_AUTOBID_BY_ID_SQL = """
            SELECT id, bidder_id, auction_id, max_price , "increment"
            FROM auto_bids
            WHERE id = ?
            """;
    private static final String FIND_AUTOBID_BY_AUCTION_BIDDER_SQL = """
            SELECT id, bidder_id, auction_id, max_price , "increment"
            FROM auto_bids
            WHERE auction_id = ? AND bidder_id = ?
            """;
    private static final String FIND_ALL_USER_AUTOBID_SQL = """
            SELECT id, bidder_id, auction_id, max_price , "increment"
            FROM auto_bids
            WHERE bidder_id = ?
            ORDER BY id
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
    public AutoBid findAutoBidByAuctionBidder(int auction_id, int bidder_id) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_AUTOBID_BY_AUCTION_BIDDER_SQL)) {
            statement.setInt(1, auction_id);
            statement.setInt(2, bidder_id);
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
             PreparedStatement statement = connection.prepareStatement(FIND_ALL_USER_AUTOBID_SQL)){
             statement.setInt(1, bidderId);
             ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                autobids.add(mapAutobid(resultSet));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to read all auto bids from PostgreSQL.", ex);
        }
        return autobids;
    }

    private void bindAutoBidForInsert(PreparedStatement statement, AutoBid item) throws SQLException {
        statement.setInt(1, item.getAuctionId());
        statement.setInt(2, item.getBidderId());
        statement.setDouble(3, item.getMaxPrice());
        statement.setDouble(4, item.getIncrement());
    }
    private void bindAutoBidForUpdate(PreparedStatement statement, AutoBid item) throws SQLException {
        statement.setDouble(1, item.getMaxPrice());
        statement.setDouble(2, item.getIncrement());
        statement.setInt(3, item.getId());
    }
    private AutoBid mapAutobid(ResultSet resultSet) throws SQLException {
        return new AutoBid(
                resultSet.getInt("id"),
                resultSet.getInt("auction_id"),
                resultSet.getInt("bidder_id"),
                resultSet.getDouble("max_price"),
                resultSet.getDouble("increment")
        );
    }
}
