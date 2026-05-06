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

// Ghi chu file: File DAO; dinh nghia hoac trien khai cac thao tac doc ghi du lieu voi database.
// Khai bao lop AutoBidDAOImpl; phu trach hop dong hoac truy cap du lieu cho database.
public class AutoBidDAOImpl implements AutoBidDAO {
    // Khi lÃ†Â°u mÃ¡Â»â€ºi, createdAt/updatedAt Ã„â€˜Ã†Â°Ã¡Â»Â£c ghi thÃ¡ÂºÂ³ng Ã„â€˜Ã¡Â»Æ’ DB sort Ã¡Â»â€¢n Ã„â€˜Ã¡Â»â€¹nh.
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho sql.
    private static final String INSERT_AUTOBID_SQL = """
            INSERT INTO auto_bids (
                auction_id, bidder_id, max_price, "increment", created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?)
            """;
    // Update chÃ¡Â»â€° Ã„â€˜Ã¡Â»â€¢i phÃ¡ÂºÂ§n rule vÃƒÂ  dÃ¡ÂºÂ¥u thÃ¡Â»Âi gian chÃ¡Â»â€°nh sÃ¡Â»Â­a cuÃ¡Â»â€˜i.
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho sql.
    private static final String UPDATE_AUTOBID_SQL = """
            UPDATE auto_bids
            SET max_price = ?, "increment" = ?, updated_at = ?
            WHERE id = ?
            """;
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho sql.
    private static final String FIND_AUTOBID_BY_ID_SQL = """
            SELECT id, bidder_id, auction_id, max_price, "increment", created_at, updated_at
            FROM auto_bids
            WHERE id = ?
            """;
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho sql.
    private static final String FIND_AUTOBID_BY_AUCTION_BIDDER_SQL = """
            SELECT id, bidder_id, auction_id, max_price, "increment", created_at, updated_at
            FROM auto_bids
            WHERE auction_id = ? AND bidder_id = ?
            """;
    // Query nÃƒÂ y phÃ¡Â»Â¥c vÃ¡Â»Â¥ cÃƒÂ¡c xÃ¡Â»Â­ lÃƒÂ½ cÃ¡ÂºÂ§n xem tÃ¡ÂºÂ¥t cÃ¡ÂºÂ£ auto-bid trong mÃ¡Â»â„¢t auction.
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho sql.
    private static final String FIND_AUTOBIDS_BY_AUCTION_SQL = """
            SELECT id, bidder_id, auction_id, max_price, "increment", created_at, updated_at
            FROM auto_bids
            WHERE auction_id = ?
            ORDER BY created_at, id
            """;
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho sql.
    private static final String FIND_ALL_USER_AUTOBID_SQL = """
            SELECT id, bidder_id, auction_id, max_price, "increment", created_at, updated_at
            FROM auto_bids
            WHERE bidder_id = ?
            ORDER BY created_at, id
            """;
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho sql.
    private static final String DELETE_AUTOBID_SQL = "DELETE FROM auto_bids WHERE id = ?";

    @Override
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac save auto bid.
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
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac update auto bid.
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
    // Phuong thuc: huy, xoa, dong hoac don trang thai cho thao tac delete auto bid.
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
    // Phuong thuc: lay hoac doc du lieu cho thao tac find auto bid by id.
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
    // Phuong thuc: lay hoac doc du lieu cho thao tac find auto bid by auction bidder.
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
    // Phuong thuc: lay hoac doc du lieu cho thao tac find all user auto bid.
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
    // Phuong thuc: lay hoac doc du lieu cho thao tac find auto bids by auction.
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

    // GÃ¡ÂºÂ¯n dÃ¡Â»Â¯ liÃ¡Â»â€¡u Java vÃƒÂ o thÃ¡Â»Â© tÃ¡Â»Â± placeholder cÃ¡Â»Â§a cÃƒÂ¢u INSERT.
    // Phuong thuc: thuc hien chuc nang bind auto bid for insert trong lop AutoBidDAOImpl.
    private void bindAutoBidForInsert(PreparedStatement statement, AutoBid item) throws SQLException {
        statement.setInt(1, item.getAuctionId());
        statement.setInt(2, item.getBidderId());
        statement.setDouble(3, item.getMaxPrice());
        statement.setDouble(4, item.getIncrement());
        statement.setLong(5, item.getCreatedAt());
        statement.setLong(6, item.getUpdatedAt());
    }

    // GÃ¡ÂºÂ¯n dÃ¡Â»Â¯ liÃ¡Â»â€¡u Java vÃƒÂ o thÃ¡Â»Â© tÃ¡Â»Â± placeholder cÃ¡Â»Â§a cÃƒÂ¢u UPDATE.
    // Phuong thuc: thuc hien chuc nang bind auto bid for update trong lop AutoBidDAOImpl.
    private void bindAutoBidForUpdate(PreparedStatement statement, AutoBid item) throws SQLException {
        statement.setDouble(1, item.getMaxPrice());
        statement.setDouble(2, item.getIncrement());
        statement.setLong(3, item.getUpdatedAt());
        statement.setInt(4, item.getId());
    }

    // DÃ¡Â»Â±ng AutoBid tÃ¡Â»Â« row DB Ã„â€˜Ã¡Â»Æ’ service xÃ¡Â»Â­ lÃƒÂ½ tiÃ¡ÂºÂ¿p.
    // Phuong thuc: bien doi du lieu cho thao tac map autobid.
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
