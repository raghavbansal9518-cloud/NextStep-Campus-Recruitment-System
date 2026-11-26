package com.nextstep.dao;

import com.nextstep.database.DatabaseConnection;
import com.nextstep.model.Role;
import com.nextstep.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * UserDAO — Handles all SQL queries on the 'users' table using PreparedStatement.
 */
public class UserDAO {

    /** Inserts a new user within an existing transaction and sets generated ID. */
    public boolean createUser(Connection conn, User user) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, role, email) VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getUsername().trim());
            stmt.setString(2, user.getPasswordHash());
            stmt.setString(3, user.getRole().name());
            stmt.setString(4, user.getEmail().trim());

            if (stmt.executeUpdate() == 0) {
                return false;
            }
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getInt(1));
                }
            }
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) { // Duplicate entry
                return false;
            }
            throw e;
        }
    }

    /** Inserts a new user using its own connection. */
    public boolean createUser(User user) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return createUser(conn, user);
        }
    }

    public User findByUsername(String username) throws SQLException {
        if (username == null || username.trim().isEmpty()) return null;
        String sql = "SELECT id, username, password_hash, role, email, created_at FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapRowToUser(rs) : null;
            }
        }
    }

    public User findById(int id) throws SQLException {
        if (id <= 0) return null;
        String sql = "SELECT id, username, password_hash, role, email, created_at FROM users WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapRowToUser(rs) : null;
            }
        }
    }

    public boolean existsByUsername(String username) throws SQLException {
        return existsByField("SELECT 1 FROM users WHERE username = ? LIMIT 1", username);
    }

    public boolean existsByEmail(String email) throws SQLException {
        return existsByField("SELECT 1 FROM users WHERE email = ? LIMIT 1", email);
    }

    public boolean existsByRole(Role role) throws SQLException {
        return role != null && existsByField("SELECT 1 FROM users WHERE role = ? LIMIT 1", role.name());
    }

    public boolean updateUser(User user) throws SQLException {
        if (user == null || user.getId() <= 0) return false;
        String sql = "UPDATE users SET email = ?, password_hash = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getEmail().trim());
            stmt.setString(2, user.getPasswordHash());
            stmt.setInt(3, user.getId());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updatePassword(String username, String newPasswordHash) throws SQLException {
        if (username == null || username.trim().isEmpty() || newPasswordHash == null) return false;
        String sql = "UPDATE users SET password_hash = ? WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newPasswordHash);
            stmt.setString(2, username.trim());
            return stmt.executeUpdate() > 0;
        }
    }

    // Helper: checks if any row matches a single string parameter
    private boolean existsByField(String sql, String value) throws SQLException {
        if (value == null || value.trim().isEmpty()) return false;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, value.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    // Helper: converts a SQL ResultSet row into a User object
    private User mapRowToUser(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime createdAt = (ts != null) ? ts.toLocalDateTime() : null;
        return new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                Role.fromString(rs.getString("role")),
                rs.getString("email"),
                createdAt
        );
    }
}
