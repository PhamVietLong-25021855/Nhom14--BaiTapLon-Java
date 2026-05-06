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
import java.util.List;

// Ghi chu file: File DAO; dinh nghia hoac trien khai cac thao tac doc ghi du lieu voi database.
// Khai bao lop UserDAOImpl; phu trach hop dong hoac truy cap du lieu cho database.
public class UserDAOImpl implements UserDAO {
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho sql.
    private static final String INSERT_SQL = """
            INSERT INTO users (username, password, full_name, email, role, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho sql.
    private static final String UPDATE_SQL = """
            UPDATE users
            SET username = ?, password = ?, full_name = ?, email = ?, role = ?, status = ?, updated_at = ?
            WHERE id = ?
            """;
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho sql.
    private static final String FIND_BY_USERNAME_SQL = """
            SELECT id, username, password, full_name, email, role, status, created_at, updated_at
            FROM users
            WHERE LOWER(username) = LOWER(?)
            """;
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho sql.
    private static final String FIND_BY_EMAIL_SQL = """
            SELECT id, username, password, full_name, email, role, status, created_at, updated_at
            FROM users
            WHERE LOWER(email) = LOWER(?)
            """;
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho sql.
    private static final String FIND_ALL_SQL = """
            SELECT id, username, password, full_name, email, role, status, created_at, updated_at
            FROM users
            ORDER BY id
            """;

    @Override
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac save.
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
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac update.
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
    // Phuong thuc: lay hoac doc du lieu cho thao tac find by username.
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
    // Phuong thuc: lay hoac doc du lieu cho thao tac find by email.
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
    // Phuong thuc: lay hoac doc du lieu cho thao tac find all.
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
    // Phuong thuc: bien doi du lieu cho thao tac map user.
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
