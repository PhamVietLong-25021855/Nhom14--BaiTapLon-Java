package userauth.dao;

import userauth.database.DatabaseConnection;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;
import userauth.model.BidTransaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

// File note: Triển khai PostgreSQL cho DAO của module này.
// DAO PostgreSQL cho auction vÃ  bid history.
public class AuctionDAOImpl implements AuctionDAO {
    // Insert má»›i Ä‘Ã£ mang theo áº£nh binary vÃ  sá»‘ láº§n anti-sniping.
    private static final String INSERT_AUCTION_SQL = """
            INSERT INTO auctions (
                name, description, start_price, current_highest_bid, start_time, end_time,
                category, image_source, image_data, created_at, updated_at, seller_id, winner_id, status, anti_sniping_extensions
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    // Update cÅ©ng ghi láº¡i áº£nh má»›i vÃ  tráº¡ng thÃ¡i anti-sniping hiá»‡n táº¡i.
    private static final String UPDATE_AUCTION_SQL = """
            UPDATE auctions
            SET name = ?, description = ?, start_price = ?, current_highest_bid = ?, start_time = ?, end_time = ?,
                category = ?, image_source = ?, image_data = ?, updated_at = ?, seller_id = ?, winner_id = ?, status = ?, anti_sniping_extensions = ?
            WHERE id = ?
            """;
    private static final String DELETE_BIDS_BY_AUCTION_SQL = "DELETE FROM bids WHERE auction_id = ?";
    private static final String DELETE_AUCTION_SQL = "DELETE FROM auctions WHERE id = ?";
    // Select pháº£i Ä‘á»c Ä‘á»§ cá»™t má»›i Ä‘á»ƒ UI/service nhÃ¬n tháº¥y dá»¯ liá»‡u Ä‘áº§y Ä‘á»§.
    private static final String FIND_AUCTION_BY_ID_SQL = """
            SELECT id, name, description, start_price, current_highest_bid, start_time, end_time,
                   category, image_source, image_data, created_at, updated_at, seller_id, winner_id, status, anti_sniping_extensions
            FROM auctions
            WHERE id = ?
            """;
    private static final String FIND_ALL_AUCTIONS_SQL = """
            SELECT id, name, description, start_price, current_highest_bid, start_time, end_time,
                   category, image_source, image_data, created_at, updated_at, seller_id, winner_id, status, anti_sniping_extensions
            FROM auctions
            ORDER BY id
            """;
    private static final String INSERT_BID_SQL = """
            INSERT INTO bids (auction_id, bidder_id, amount, bid_time, status)
            VALUES (?, ?, ?, ?, ?)
            """;
    private static final String FIND_BIDS_BY_AUCTION_SQL = """
            SELECT id, auction_id, bidder_id, amount, bid_time, status
            FROM bids
            WHERE auction_id = ?
            ORDER BY bid_time, id
            """;
    private static final String FIND_ALL_BIDS_SQL = """
            SELECT id, auction_id, bidder_id, amount, bid_time, status
            FROM bids
            ORDER BY auction_id, bid_time, id
            """;

    @Override
    public void saveAuction(AuctionItem item) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_AUCTION_SQL, Statement.RETURN_GENERATED_KEYS)) {
            bindAuctionForInsert(statement, item);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    item.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to save the auction to PostgreSQL.", ex);
        }
    }

    @Override
    public void updateAuction(AuctionItem item) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_AUCTION_SQL)) {
            bindAuctionForUpdate(statement, item);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException(
                    "Unable to update the auction in PostgreSQL. SQLState=" + ex.getSQLState() +
                            ", detail=" + ex.getMessage(),
                    ex
            );
        }
    }

    @Override
    public void deleteAuction(int id) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement deleteBidsStatement = connection.prepareStatement(DELETE_BIDS_BY_AUCTION_SQL);
                 PreparedStatement deleteAuctionStatement = connection.prepareStatement(DELETE_AUCTION_SQL)) {
                deleteBidsStatement.setInt(1, id);
                deleteBidsStatement.executeUpdate();

                deleteAuctionStatement.setInt(1, id);
                deleteAuctionStatement.executeUpdate();
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to delete the auction in PostgreSQL.", ex);
        }
    }

    @Override
    public AuctionItem findAuctionById(int id) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_AUCTION_BY_ID_SQL)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return mapAuction(resultSet);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to find the auction in PostgreSQL.", ex);
        }
    }

    @Override
    public List<AuctionItem> findAllAuctions() {
        List<AuctionItem> auctions = new ArrayList<>();

        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ALL_AUCTIONS_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                auctions.add(mapAuction(resultSet));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to read the auction list from PostgreSQL.", ex);
        }

        return auctions;
    }

    @Override
    public void saveBid(BidTransaction bid) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_BID_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, bid.getAuctionId());
            statement.setInt(2, bid.getBidderId());
            statement.setDouble(3, bid.getAmount());
            statement.setLong(4, bid.getTimestamp());
            statement.setString(5, bid.getStatus());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    bid.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to save the bid transaction to PostgreSQL.", ex);
        }
    }

    @Override
    public void saveBidAndUpdateAuction(BidTransaction bid, AuctionItem item) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement saveBidStatement = connection.prepareStatement(INSERT_BID_SQL, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement updateAuctionStatement = connection.prepareStatement(UPDATE_AUCTION_SQL)) {
                saveBidStatement.setInt(1, bid.getAuctionId());
                saveBidStatement.setInt(2, bid.getBidderId());
                saveBidStatement.setDouble(3, bid.getAmount());
                saveBidStatement.setLong(4, bid.getTimestamp());
                saveBidStatement.setString(5, bid.getStatus());
                saveBidStatement.executeUpdate();

                try (ResultSet generatedKeys = saveBidStatement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        bid.setId(generatedKeys.getInt(1));
                    }
                }

                bindAuctionForUpdate(updateAuctionStatement, item);
                updateAuctionStatement.executeUpdate();
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException(
                    "Unable to save the bid and update the auction in PostgreSQL. SQLState=" + ex.getSQLState() +
                            ", detail=" + ex.getMessage(),
                    ex
            );
        }
    }

    @Override
    public List<BidTransaction> findBidsByAuction(int auctionId) {
        List<BidTransaction> bids = new ArrayList<>();

        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BIDS_BY_AUCTION_SQL)) {
            statement.setInt(1, auctionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    bids.add(mapBid(resultSet));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to read bid history from PostgreSQL.", ex);
        }

        return bids;
    }

    @Override
    public List<BidTransaction> findAllBids() {
        List<BidTransaction> bids = new ArrayList<>();

        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ALL_BIDS_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                bids.add(mapBid(resultSet));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to read all bid transactions from PostgreSQL.", ex);
        }

        return bids;
    }

    // Map thá»© tá»± field Java sang Ä‘Ãºng thá»© tá»± placeholder cá»§a cÃ¢u INSERT.
    private void bindAuctionForInsert(PreparedStatement statement, AuctionItem item) throws SQLException {
        statement.setString(1, item.getName());
        statement.setString(2, item.getDescription());
        statement.setDouble(3, item.getStartPrice());
        statement.setDouble(4, item.getCurrentHighestBid());
        statement.setLong(5, item.getStartTime());
        statement.setLong(6, item.getEndTime());
        statement.setString(7, item.getCategory());
        statement.setString(8, item.getImageSource());
        statement.setBytes(9, item.getImageData());
        statement.setLong(10, item.getCreatedAt());
        statement.setLong(11, item.getUpdatedAt());
        statement.setInt(12, item.getSellerId());
        if (item.getWinnerId() <= 0) {
            statement.setNull(13, Types.INTEGER);
        } else {
            statement.setInt(13, item.getWinnerId());
        }
        statement.setString(14, item.getStatus().name());
        statement.setInt(15, item.getAntiSnipingExtensionCount());
    }

    // Map thá»© tá»± field Java sang Ä‘Ãºng thá»© tá»± placeholder cá»§a cÃ¢u UPDATE.
    private void bindAuctionForUpdate(PreparedStatement statement, AuctionItem item) throws SQLException {
        statement.setString(1, item.getName());
        statement.setString(2, item.getDescription());
        statement.setDouble(3, item.getStartPrice());
        statement.setDouble(4, item.getCurrentHighestBid());
        statement.setLong(5, item.getStartTime());
        statement.setLong(6, item.getEndTime());
        statement.setString(7, item.getCategory());
        statement.setString(8, item.getImageSource());
        statement.setBytes(9, item.getImageData());
        statement.setLong(10, item.getUpdatedAt());
        statement.setInt(11, item.getSellerId());
        if (item.getWinnerId() <= 0) {
            statement.setNull(12, Types.INTEGER);
        } else {
            statement.setInt(12, item.getWinnerId());
        }
        statement.setString(13, item.getStatus().name());
        statement.setInt(14, item.getAntiSnipingExtensionCount());
        statement.setInt(15, item.getId());
    }

    // Dá»±ng AuctionItem hoÃ n chá»‰nh tá»« row PostgreSQL.
    private AuctionItem mapAuction(ResultSet resultSet) throws SQLException {
        Object winnerValue = resultSet.getObject("winner_id");
        int winnerId = winnerValue == null ? -1 : ((Number) winnerValue).intValue();

        return new AuctionItem(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getDouble("start_price"),
                resultSet.getDouble("current_highest_bid"),
                resultSet.getLong("start_time"),
                resultSet.getLong("end_time"),
                resultSet.getString("category"),
                resultSet.getString("image_source"),
                resultSet.getBytes("image_data"),
                resultSet.getLong("created_at"),
                resultSet.getLong("updated_at"),
                resultSet.getInt("seller_id"),
                winnerId,
                AuctionStatus.valueOf(resultSet.getString("status").trim().toUpperCase()),
                resultSet.getInt("anti_sniping_extensions")
        );
    }

    private BidTransaction mapBid(ResultSet resultSet) throws SQLException {
        return new BidTransaction(
                resultSet.getInt("id"),
                resultSet.getInt("auction_id"),
                resultSet.getInt("bidder_id"),
                resultSet.getDouble("amount"),
                resultSet.getLong("bid_time"),
                resultSet.getString("status")
        );
    }
}

