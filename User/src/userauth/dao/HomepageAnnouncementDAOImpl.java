package userauth.dao;

import userauth.database.DatabaseConnection;
import userauth.model.HomepageAnnouncement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class HomepageAnnouncementDAOImpl implements HomepageAnnouncementDAO {
    private static final String INSERT_SQL = """
            INSERT INTO homepage_announcements(
                id, title, summary, details, schedule_text, linked_auction_id, author_id, created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String UPDATE_SQL = """
            UPDATE homepage_announcements
            SET title = ?, summary = ?, details = ?, schedule_text = ?, linked_auction_id = ?, author_id = ?, created_at = ?, updated_at = ?
            WHERE id = ?
            """;
    private static final String DELETE_SQL = """
            DELETE FROM homepage_announcements
            WHERE id = ?
            """;
    private static final String FIND_BY_ID_SQL = """
            SELECT * FROM homepage_announcements
            WHERE id = ?
            LIMIT 1
            """;
    private static final String FIND_ALL_SQL = """
            SELECT * FROM homepage_announcements
            ORDER BY updated_at DESC, id DESC
            """;

    @Override
    public void save(HomepageAnnouncement announcement) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            bindInsert(statement, announcement);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Khong the luu bai dang trang chu vao MySQL.", ex);
        }
    }

    @Override
    public void update(HomepageAnnouncement announcement) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setString(1, announcement.getTitle());
            statement.setString(2, announcement.getSummary());
            statement.setString(3, announcement.getDetails());
            statement.setString(4, announcement.getScheduleText());
            bindNullableAuction(statement, 5, announcement.getLinkedAuctionId());
            statement.setInt(6, announcement.getAuthorId());
            statement.setLong(7, announcement.getCreatedAt());
            statement.setLong(8, announcement.getUpdatedAt());
            statement.setInt(9, announcement.getId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Khong the cap nhat bai dang trang chu trong MySQL.", ex);
        }
    }

    @Override
    public void delete(int id) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Khong the xoa bai dang trang chu trong MySQL.", ex);
        }
    }

    @Override
    public HomepageAnnouncement findById(int id) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapAnnouncement(resultSet);
                }
                return null;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Khong the tim bai dang trang chu theo id trong MySQL.", ex);
        }
    }

    @Override
    public List<HomepageAnnouncement> findAll() {
        List<HomepageAnnouncement> announcements = new ArrayList<>();
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                announcements.add(mapAnnouncement(resultSet));
            }
            return announcements;
        } catch (SQLException ex) {
            throw new IllegalStateException("Khong the tai danh sach bai dang trang chu tu MySQL.", ex);
        }
    }

    private void bindInsert(PreparedStatement statement, HomepageAnnouncement announcement) throws SQLException {
        statement.setInt(1, announcement.getId());
        statement.setString(2, announcement.getTitle());
        statement.setString(3, announcement.getSummary());
        statement.setString(4, announcement.getDetails());
        statement.setString(5, announcement.getScheduleText());
        bindNullableAuction(statement, 6, announcement.getLinkedAuctionId());
        statement.setInt(7, announcement.getAuthorId());
        statement.setLong(8, announcement.getCreatedAt());
        statement.setLong(9, announcement.getUpdatedAt());
    }

    private void bindNullableAuction(PreparedStatement statement, int index, int linkedAuctionId) throws SQLException {
        if (linkedAuctionId <= 0) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setInt(index, linkedAuctionId);
        }
    }

    private HomepageAnnouncement mapAnnouncement(ResultSet resultSet) throws SQLException {
        Object linkedAuctionValue = resultSet.getObject("linked_auction_id");
        int linkedAuctionId = linkedAuctionValue == null ? -1 : ((Number) linkedAuctionValue).intValue();

        return new HomepageAnnouncement(
                resultSet.getInt("id"),
                resultSet.getString("title"),
                resultSet.getString("summary"),
                resultSet.getString("details"),
                resultSet.getString("schedule_text"),
                linkedAuctionId,
                resultSet.getInt("author_id"),
                resultSet.getLong("created_at"),
                resultSet.getLong("updated_at")
        );
    }
}
