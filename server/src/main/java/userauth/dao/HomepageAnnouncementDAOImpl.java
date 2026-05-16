package userauth.dao;

import userauth.database.DatabaseConnection;
import userauth.model.HomepageAnnouncement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class HomepageAnnouncementDAOImpl implements HomepageAnnouncementDAO {
    private static final String INSERT_SQL = """
            INSERT INTO homepage_announcements (title, summary, details, schedule_text, linked_auction_id, author_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String UPDATE_SQL = """
            UPDATE homepage_announcements SET title = ?, summary = ?, details = ?, schedule_text = ?, linked_auction_id = ?, author_id = ?, updated_at = ? WHERE id = ?
            """;
    private static final String DELETE_SQL = "DELETE FROM homepage_announcements WHERE id = ?";
    private static final String FIND_BY_ID_SQL = """
            SELECT id, title, summary, details, schedule_text, linked_auction_id, author_id, created_at, updated_at FROM homepage_announcements WHERE id = ?
            """;
    private static final String FIND_ALL_SQL = """
            SELECT id, title, summary, details, schedule_text, linked_auction_id, author_id, created_at, updated_at FROM homepage_announcements ORDER BY id
            """;

    @Override
    public void save(HomepageAnnouncement announcement) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            bindForInsert(statement, announcement);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) announcement.setId(generatedKeys.getInt(1));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to save homepage content to database.", ex);
        }
    }

    @Override
    public void update(HomepageAnnouncement announcement) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            bindForUpdate(statement, announcement);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to update homepage content in database.", ex);
        }
    }

    @Override
    public void delete(int id) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to delete homepage content in database.", ex);
        }
    }

    @Override
    public HomepageAnnouncement findById(int id) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) return null;
                return mapAnnouncement(resultSet);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to find the homepage announcement in database.", ex);
        }
    }

    @Override
    public List<HomepageAnnouncement> findAll() {
        List<HomepageAnnouncement> announcements = new ArrayList<>();
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) announcements.add(mapAnnouncement(resultSet));
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to read homepage announcements from database.", ex);
        }
        return announcements;
    }

    private void bindForInsert(PreparedStatement statement, HomepageAnnouncement a) throws SQLException {
        statement.setString(1, a.getTitle());
        statement.setString(2, a.getSummary());
        statement.setString(3, a.getDetails());
        statement.setString(4, a.getScheduleText());
        if (a.getLinkedAuctionId() <= 0) statement.setNull(5, Types.INTEGER);
        else statement.setInt(5, a.getLinkedAuctionId());
        statement.setInt(6, a.getAuthorId());
        statement.setLong(7, a.getCreatedAt());
        statement.setLong(8, a.getUpdatedAt());
    }

    private void bindForUpdate(PreparedStatement statement, HomepageAnnouncement a) throws SQLException {
        statement.setString(1, a.getTitle());
        statement.setString(2, a.getSummary());
        statement.setString(3, a.getDetails());
        statement.setString(4, a.getScheduleText());
        if (a.getLinkedAuctionId() <= 0) statement.setNull(5, Types.INTEGER);
        else statement.setInt(5, a.getLinkedAuctionId());
        statement.setInt(6, a.getAuthorId());
        statement.setLong(7, a.getUpdatedAt());
        statement.setInt(8, a.getId());
    }

    private HomepageAnnouncement mapAnnouncement(ResultSet rs) throws SQLException {
        Object linkedAuctionValue = rs.getObject("linked_auction_id");
        int linkedAuctionId = linkedAuctionValue == null ? -1 : ((Number) linkedAuctionValue).intValue();
        return new HomepageAnnouncement(
                rs.getInt("id"), rs.getString("title"), rs.getString("summary"), rs.getString("details"),
                rs.getString("schedule_text"), linkedAuctionId, rs.getInt("author_id"),
                rs.getLong("created_at"), rs.getLong("updated_at")
        );
    }
}
