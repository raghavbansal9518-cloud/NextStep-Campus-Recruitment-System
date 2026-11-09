package com.nextstep.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * DatabaseConnection
 * 
 * Provides centralized JDBC connection management for NextStep.
 * Loads configuration from config/db.properties using standard java.util.Properties.
 * 
 * College Viva Concepts:
 * - Uses DriverManager.getConnection() with JDBC URL.
 * - Explicitly loads the MySQL 8.0 Driver class (com.mysql.cj.jdbc.Driver).
 * - Implements security practices: never logs passwords or exposes credentials.
 */
public class DatabaseConnection {

    private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";
    private static final String CONFIG_FILE_PATH = "config/db.properties";

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;
    private static boolean isConfigLoaded = false;

    // Static initialization block to load MySQL JDBC Driver
    static {
        try {
            Class.forName(DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            System.err.println("[ERROR] MySQL JDBC Driver not found. Ensure mysql-connector-j.jar is in the classpath.");
        }
    }

    /**
     * Loads database configuration properties from config/db.properties.
     * If the file is not found, attempts to read environment variables.
     */
    public static synchronized void loadConfiguration() throws IOException {
        if (isConfigLoaded) {
            return;
        }

        File configFile = new File(CONFIG_FILE_PATH);
        Properties props = new Properties();

        if (configFile.exists()) {
            try (InputStream input = new FileInputStream(configFile)) {
                props.load(input);
                dbUrl = props.getProperty("db.url");
                dbUser = props.getProperty("db.user");
                dbPassword = props.getProperty("db.password");
            }
        } else {
            // Fallback to environment variables if config file is not yet created
            dbUrl = System.getenv("NEXTSTEP_DB_URL");
            dbUser = System.getenv("NEXTSTEP_DB_USER");
            dbPassword = System.getenv("NEXTSTEP_DB_PASSWORD");
        }

        // Apply defaults if properties were not specified
        if (dbUrl == null || dbUrl.trim().isEmpty()) {
            dbUrl = "jdbc:mysql://localhost:3306/nextstep_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        }
        if (dbUser == null || dbUser.trim().isEmpty()) {
            dbUser = "root";
        }
        if (dbPassword == null) {
            dbPassword = "";
        }

        isConfigLoaded = true;
    }

    /**
     * Establishes and returns an active JDBC Connection.
     * Callers are responsible for closing the connection using try-with-resources.
     * 
     * @return Connection active database connection
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        if (!isConfigLoaded) {
            try {
                loadConfiguration();
            } catch (IOException e) {
                throw new SQLException("Failed to load database configuration from " + CONFIG_FILE_PATH, e);
            }
        }

        try {
            return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        } catch (SQLException e) {
            // Provide helpful, beginner-friendly diagnostic messages without exposing password
            if (e.getErrorCode() == 1045) { // Access Denied
                System.err.println("[ERROR] MySQL Authentication Failed: Invalid username or password in " + CONFIG_FILE_PATH);
            } else if (e.getErrorCode() == 1049) { // Unknown database
                System.err.println("[ERROR] Database 'nextstep_db' not found. Run database/schema.sql first.");
            } else if (e.getMessage() != null && e.getMessage().contains("Communications link failure")) {
                System.err.println("[ERROR] Could not connect to MySQL server. Ensure MySQL80 service is running on port 3306.");
            }
            throw e;
        }
    }

    /**
     * Executes a self-contained connection test.
     */
    public static boolean testConnection() {
        System.out.println("--------------------------------------------------");
        System.out.println("  NextStep — JDBC Database Connection Test");
        System.out.println("--------------------------------------------------");
        try {
            loadConfiguration();
            System.out.println("[SUCCESS] Configuration loaded successfully.");
            System.out.println("[INFO] Target URL: " + dbUrl);
            System.out.println("[INFO] Database User: " + dbUser);

            try (Connection conn = getConnection()) {
                if (conn != null && !conn.isClosed()) {
                    System.out.println("[SUCCESS] MySQL connection established successfully.");

                    DatabaseMetaData meta = conn.getMetaData();
                    System.out.println("[INFO] Database Product: " + meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion());
                    System.out.println("[INFO] Driver Name: " + meta.getDriverName() + " " + meta.getDriverVersion());

                    // Execute simple test query
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery("SELECT DATABASE() AS current_db, 1 + 1 AS sanity_check")) {
                        if (rs.next()) {
                            String currentDb = rs.getString("current_db");
                            int sanity = rs.getInt("sanity_check");
                            System.out.println("[SUCCESS] Connected database: " + (currentDb != null ? currentDb : "none"));
                            System.out.println("[SUCCESS] Sanity query result (1 + 1 = " + sanity + ").");
                        }
                    }

                    System.out.println("[SUCCESS] Connection closed cleanly.");
                    System.out.println("--------------------------------------------------");
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("[FAILURE] Connection test failed: " + e.getMessage());
            System.out.println("--------------------------------------------------");
            return false;
        }
        return false;
    }

    /**
     * Command-line runner for connection verification.
     */
    public static void main(String[] args) {
        testConnection();
    }
}
