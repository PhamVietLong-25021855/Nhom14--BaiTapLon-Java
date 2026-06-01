package userauth.dao;

import userauth.database.DatabaseConnection;
import userauth.model.Notification;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAOImpl implements  NotificationDAO{
    private static final String INSERT_NOTIFICATION_SQL = """
            INSERT INTO notifications (
                user_id, title, content, created_at
            )
            VALUES (?, ?, ?, ?)
            """;
    private static final String FIND_NOTIFICATION_BY_USERID_SQL = """
            SELECT id, user_id, title, content, created_at
            FROM notifications
            WHERE user_id = ? OR user_id = 0
            ORDER BY created_at DESC, id DESC
            """;
    private static final String DELETE_NOTIFICATION_SQL = """
            DELETE FROM notifications
            WHERE id = ? AND user_id = ?
            """;
    private static final String DELETE_USER_NOTIFICATIONS_SQL = """
            DELETE FROM notifications
            WHERE user_id = ?
            """;


    @Override
    public void saveNotification(Notification item) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_NOTIFICATION_SQL, Statement.RETURN_GENERATED_KEYS)) {
            bindNotificationForInsert(statement, item);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    item.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to save the notification to database: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<Notification> findNotificationToUser(int user_id) {
        List<Notification> notifications = new ArrayList<>();
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_NOTIFICATION_BY_USERID_SQL)) {
            statement.setInt(1, user_id);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    notifications.add(mapNotification(resultSet));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to read all notification from database: " + ex.getMessage(), ex);
        }
        return notifications;
    }

    @Override
    public boolean deleteNotification(int user_id, int notification_id) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_NOTIFICATION_SQL)) {
            statement.setInt(1, notification_id);
            statement.setInt(2, user_id);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to delete notification from database: " + ex.getMessage(), ex);
        }
    }

    @Override
    public int deleteNotificationsForUser(int user_id) {
        try (Connection connection = DatabaseConnection.openDatabaseConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_USER_NOTIFICATIONS_SQL)) {
            statement.setInt(1, user_id);
            return statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to delete notifications from database: " + ex.getMessage(), ex);
        }
    }

    private void bindNotificationForInsert(PreparedStatement statement, Notification item) throws SQLException {
        statement.setInt(1, item.getUser_id());
        statement.setString(2, item.getTitle());
        statement.setString(3, item.getContent());
        statement.setLong(4, item.getCreated_at());
    }

    private Notification mapNotification(ResultSet resultSet) throws SQLException {
        return new Notification(
                resultSet.getInt("id"),
                resultSet.getInt("user_id"),
                resultSet.getString("title"),
                resultSet.getString("content"),
                resultSet.getLong("created_at")
        );
    }
}
