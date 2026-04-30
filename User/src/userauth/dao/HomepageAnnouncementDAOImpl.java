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
            INSERT INTO homepage_announcements (
                title, summary, details, schedule_text, linked_auction_id, author_id, created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String UPDATE_SQL = """
            UPDATE homepage_announcements
            SET title = ?, summary = ?, details = ?, schedule_text = ?, linked_auction_id = ?, author_id = ?, updated_at = ?
            WHERE id = ?
            """;
    private static final String DELETE_SQL = "DELETE FROM homepage_announcements WHERE id = ?";
    private static final String FIND_BY_ID_SQL = """
            SELECT id, title, summary, details, schedule_text, linked_auction_id, author_id, created_at, updated_at
            FROM homepage_announcements
            WHERE id = ?
            """;
    private static final String FIND_ALL_SQL = """
            SELECT id, title, summary, details, schedule_text, linked_auction_id, author_id, created_at, updated_at
            FROM homepage_announcements
            ORDER BY id
            """;

    @Override
    public void save(HomepageAnnouncement announcement) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            bindForInsert(statement, announcement);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    announcement.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to save homepage content to PostgreSQL.", ex);
        }
    }

    @Override
    public void update(HomepageAnnouncement announcement) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            bindForUpdate(statement, announcement);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to update homepage content in PostgreSQL.", ex);
        }
    }

    @Override
    public void delete(int id) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to delete homepage content in PostgreSQL.", ex);
        }
    }

    @Override
    public HomepageAnnouncement findById(int id) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return mapAnnouncement(resultSet);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to find the homepage announcement in PostgreSQL.", ex);
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
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to read homepage announcements from PostgreSQL.", ex);
        }

        return announcements;
    }

    private void bindForInsert(PreparedStatement statement, HomepageAnnouncement announcement) throws SQLException {
        statement.setString(1, announcement.getTitle());
        statement.setString(2, announcement.getSummary());
        statement.setString(3, announcement.getDetails());
        statement.setString(4, announcement.getScheduleText());
        if (announcement.getLinkedAuctionId() <= 0) {
            statement.setNull(5, Types.INTEGER);
        } else {
            statement.setInt(5, announcement.getLinkedAuctionId());
        }
        statement.setInt(6, announcement.getAuthorId());
        statement.setLong(7, announcement.getCreatedAt());
        statement.setLong(8, announcement.getUpdatedAt());
    }

    private void bindForUpdate(PreparedStatement statement, HomepageAnnouncement announcement) throws SQLException {
        statement.setString(1, announcement.getTitle());
        statement.setString(2, announcement.getSummary());
        statement.setString(3, announcement.getDetails());
        statement.setString(4, announcement.getScheduleText());
        if (announcement.getLinkedAuctionId() <= 0) {
            statement.setNull(5, Types.INTEGER);
        } else {
            statement.setInt(5, announcement.getLinkedAuctionId());
        }
        statement.setInt(6, announcement.getAuthorId());
        statement.setLong(7, announcement.getUpdatedAt());
        statement.setInt(8, announcement.getId());
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
