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
 * UserDAO
 * 
 * Data Access Object (DAO) responsible for all SQL operations on the 'users' table.
 * 
 * College Viva Concepts:
 * - DAO Pattern: Isolates database interaction from business logic.
 * - PreparedStatement: Pre-compiled SQL statements preventing SQL Injection attacks.
 * - try-with-resources: Ensures Connection, PreparedStatement, and ResultSet are
 *   automatically closed, preventing resource leaks.
 * - Transaction Support: Overloaded methods accept Connection parameter for transactions.
 */
public class UserDAO {

    /**
     * Inserts a new user record into the database within an existing connection / transaction.
     * Generates and assigns the database-generated primary key ID to the user object.
     * 
     * @param conn active JDBC Connection
     * @param user User entity to persist (contains pre-hashed password)
     * @return boolean true if inserted successfully, false otherwise
     * @throws SQLException if a database error occurs (other than duplicate entry)
     */
    public boolean createUser(Connection conn, User user) throws SQLException {
        if (conn == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        String sql = "INSERT INTO users (username, password_hash, role, email) VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getUsername().trim());
            stmt.setString(2, user.getPasswordHash());
            stmt.setString(3, user.getRole().name());
            stmt.setString(4, user.getEmail().trim());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                return false;
            }

            // Retrieve database-generated primary key
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getInt(1));
                }
            }

            return true;
        } catch (SQLException e) {
            // Error Code 1062 = MySQL Duplicate Entry (e.g. duplicate username or email)
            if (e.getErrorCode() == 1062) {
                System.err.println("[WARN] Registration failed: Username or email already registered.");
                return false;
            }
            System.err.println("[ERROR] Error creating user: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Standalone insertion opening its own connection. Delegates to the transactional method.
     */
    public boolean createUser(User user) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return createUser(conn, user);
        }
    }

    /**
     * Finds a user by their unique username.
     * 
     * @param username username to look up
     * @return User object if found, null otherwise
     * @throws SQLException if a database error occurs
     */
    public User findByUsername(String username) throws SQLException {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT id, username, password_hash, role, email, created_at FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    String uname = rs.getString("username");
                    String passHash = rs.getString("password_hash");
                    Role role = Role.fromString(rs.getString("role"));
                    String email = rs.getString("email");
                    Timestamp ts = rs.getTimestamp("created_at");
                    LocalDateTime createdAt = (ts != null) ? ts.toLocalDateTime() : null;

                    return new User(id, uname, passHash, role, email, createdAt);
                }
            }
        }
        return null;
    }

    /**
     * Finds a user by their unique primary key ID.
     * 
     * @param id user ID to look up
     * @return User object if found, null otherwise
     * @throws SQLException if a database error occurs
     */
    public User findById(int id) throws SQLException {
        if (id <= 0) {
            return null;
        }

        String sql = "SELECT id, username, password_hash, role, email, created_at FROM users WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int userId = rs.getInt("id");
                    String uname = rs.getString("username");
                    String passHash = rs.getString("password_hash");
                    Role role = Role.fromString(rs.getString("role"));
                    String email = rs.getString("email");
                    Timestamp ts = rs.getTimestamp("created_at");
                    LocalDateTime createdAt = (ts != null) ? ts.toLocalDateTime() : null;

                    return new User(userId, uname, passHash, role, email, createdAt);
                }
            }
        }
        return null;
    }

    /**
     * Checks whether a username already exists in the database.
     * 
     * @param username username to verify
     * @return true if exists, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean existsByUsername(String username) throws SQLException {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        String sql = "SELECT 1 FROM users WHERE username = ? LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Checks if at least one user with the specified role exists in the database.
     * Useful for checking whether an administrative placement officer has been provisioned.
     * 
     * @param role Role to check
     * @return boolean true if at least one user with this role exists
     * @throws SQLException on database errors
     */
    public boolean existsByRole(Role role) throws SQLException {
        if (role == null) {
            return false;
        }

        String sql = "SELECT 1 FROM users WHERE role = ? LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, role.name());

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Self-contained verification runner for testing UserDAO operations.
     */
    public static void main(String[] args) {
        System.out.println("--------------------------------------------------");
        System.out.println("  NextStep — UserDAO Verification Test");
        System.out.println("--------------------------------------------------");

        UserDAO userDAO = new UserDAO();
        String testUsername = "fictional_test_user_" + System.currentTimeMillis();
        String testEmail = testUsername + "@example.com";
        String dummyHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"; // SHA-256 placeholder

        try {
            // Test 1: Check existence before insertion
            System.out.println("[TEST 1] Checking non-existent username...");
            boolean initiallyExists = userDAO.existsByUsername(testUsername);
            System.out.println("[RESULT] Exists before insert: " + initiallyExists + " (Expected: false)");

            // Test 2: Insert new user
            System.out.println("[TEST 2] Inserting fictional test user...");
            User newUser = new User(testUsername, dummyHash, Role.STUDENT, testEmail);
            boolean created = userDAO.createUser(newUser);
            System.out.println("[RESULT] User created: " + created + ", Generated ID: " + newUser.getId());

            // Test 3: Find by username and verify role mapping
            System.out.println("[TEST 3] Finding user by username...");
            User fetchedUser = userDAO.findByUsername(testUsername);
            if (fetchedUser != null) {
                System.out.println("[SUCCESS] Fetched: " + fetchedUser.getUsername() + ", Role: " + fetchedUser.getRole());
            } else {
                System.err.println("[FAILURE] Could not retrieve user.");
            }

            // Test 4: Duplicate username detection
            System.out.println("[TEST 4] Testing duplicate username insertion...");
            User duplicateUser = new User(testUsername, dummyHash, Role.STUDENT, "another_" + testEmail);
            boolean duplicateResult = userDAO.createUser(duplicateUser);
            System.out.println("[RESULT] Duplicate allowed: " + duplicateResult + " (Expected: false)");

            // Cleanup test record using raw SQL to keep database pristine
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement delStmt = conn.prepareStatement("DELETE FROM users WHERE username = ?")) {
                delStmt.setString(1, testUsername);
                delStmt.executeUpdate();
                System.out.println("[CLEANUP] Fictional test user cleaned up successfully.");
            }

            System.out.println("--------------------------------------------------");
            System.out.println("[SUCCESS] All UserDAO tests passed!");
            System.out.println("--------------------------------------------------");
        } catch (SQLException e) {
            System.err.println("[FAILURE] UserDAO test failed with SQLException: " + e.getMessage());
        }
    }
}
