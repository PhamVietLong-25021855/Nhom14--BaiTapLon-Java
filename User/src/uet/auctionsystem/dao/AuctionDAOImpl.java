package uet.auctionsystem.dao;

import uet.auctionsystem.database.DatabaseConnection;
import uet.auctionsystem.model.AuctionItem;
import uet.auctionsystem.model.AuctionStatus;
import uet.auctionsystem.model.BidTransaction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

// Ghi chu file: File DAO; dinh nghia hoac trien khai cac thao tac doc ghi du lieu voi database.
// Khai bao lop AuctionDAOImpl; phu trach hop dong hoac truy cap du lieu cho database.
public class AuctionDAOImpl implements AuctionDAO {
    // Insert mÃ¡Â»â€ºi Ã„â€˜ÃƒÂ£ mang theo Ã¡ÂºÂ£nh binary vÃƒÂ  sÃ¡Â»â€˜ lÃ¡ÂºÂ§n anti-sniping.
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho sql.
    private static final String INSERT_AUCTION_SQL = """
            INSERT INTO auctions (
                name, description, start_price, current_highest_bid, start_time, end_time,
                category, image_source, image_data, created_at, updated_at, seller_id, winner_id, status, anti_sniping_extensions
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    // Update cÃ…Â©ng ghi lÃ¡ÂºÂ¡i Ã¡ÂºÂ£nh mÃ¡Â»â€ºi vÃƒÂ  trÃ¡ÂºÂ¡ng thÃƒÂ¡i anti-sniping hiÃ¡Â»â€¡n tÃ¡ÂºÂ¡i.
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho sql.
    private static final String UPDATE_AUCTION_SQL = """
            UPDATE auctions
            SET name = ?, description = ?, start_price = ?, current_highest_bid = ?, start_time = ?, end_time = ?,
                category = ?, image_source = ?, image_data = ?, updated_at = ?, seller_id = ?, winner_id = ?, status = ?, anti_sniping_extensions = ?
            WHERE id = ?
            """;
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho sql.
    private static final String DELETE_BIDS_BY_AUCTION_SQL = "DELETE FROM bids WHERE auction_id = ?";
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho sql.
    private static final String DELETE_AUCTION_SQL = "DELETE FROM auctions WHERE id = ?";
    // Select phÃ¡ÂºÂ£i Ã„â€˜Ã¡Â»Âc Ã„â€˜Ã¡Â»Â§ cÃ¡Â»â„¢t mÃ¡Â»â€ºi Ã„â€˜Ã¡Â»Æ’ UI/service nhÃƒÂ¬n thÃ¡ÂºÂ¥y dÃ¡Â»Â¯ liÃ¡Â»â€¡u Ã„â€˜Ã¡ÂºÂ§y Ã„â€˜Ã¡Â»Â§.
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho sql.
    private static final String FIND_AUCTION_BY_ID_SQL = """
            SELECT id, name, description, start_price, current_highest_bid, start_time, end_time,
                   category, image_source, image_data, created_at, updated_at, seller_id, winner_id, status, anti_sniping_extensions
            FROM auctions
            WHERE id = ?
            """;
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho sql.
    private static final String FIND_ALL_AUCTIONS_SQL = """
            SELECT id, name, description, start_price, current_highest_bid, start_time, end_time,
                   category, image_source, image_data, created_at, updated_at, seller_id, winner_id, status, anti_sniping_extensions
            FROM auctions
            ORDER BY id
            """;
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho sql.
    private static final String INSERT_BID_SQL = """
            INSERT INTO bids (auction_id, bidder_id, amount, bid_time, status)
            VALUES (?, ?, ?, ?, ?)
            """;
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho sql.
    private static final String FIND_BIDS_BY_AUCTION_SQL = """
            SELECT id, auction_id, bidder_id, amount, bid_time, status
            FROM bids
            WHERE auction_id = ?
            ORDER BY bid_time, id
            """;
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho sql.
    private static final String FIND_ALL_BIDS_SQL = """
            SELECT id, auction_id, bidder_id, amount, bid_time, status
            FROM bids
            ORDER BY auction_id, bid_time, id
            """;

    @Override
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac save auction.
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
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac update auction.
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
    // Phuong thuc: huy, xoa, dong hoac don trang thai cho thao tac delete auction.
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
    // Phuong thuc: lay hoac doc du lieu cho thao tac find auction by id.
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
    // Phuong thuc: lay hoac doc du lieu cho thao tac find all auctions.
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
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac save bid.
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
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac save bid and update auction.
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
    // Phuong thuc: lay hoac doc du lieu cho thao tac find bids by auction.
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
    // Phuong thuc: lay hoac doc du lieu cho thao tac find all bids.
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

    // Map thÃ¡Â»Â© tÃ¡Â»Â± field Java sang Ã„â€˜ÃƒÂºng thÃ¡Â»Â© tÃ¡Â»Â± placeholder cÃ¡Â»Â§a cÃƒÂ¢u INSERT.
    // Phuong thuc: thuc hien chuc nang bind auction for insert trong lop AuctionDAOImpl.
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

    // Map thÃ¡Â»Â© tÃ¡Â»Â± field Java sang Ã„â€˜ÃƒÂºng thÃ¡Â»Â© tÃ¡Â»Â± placeholder cÃ¡Â»Â§a cÃƒÂ¢u UPDATE.
    // Phuong thuc: thuc hien chuc nang bind auction for update trong lop AuctionDAOImpl.
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

    // DÃ¡Â»Â±ng AuctionItem hoÃƒÂ n chÃ¡Â»â€°nh tÃ¡Â»Â« row PostgreSQL.
    // Phuong thuc: bien doi du lieu cho thao tac map auction.
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
    // Phuong thuc: bien doi du lieu cho thao tac map bid.
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
