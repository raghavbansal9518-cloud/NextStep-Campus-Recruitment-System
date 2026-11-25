package com.nextstep.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
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
 * Centralized JDBC connection management and database initializer for NextStep.
 * Loads configuration from config/db.properties using standard java.util.Properties.
 * 
 * College Viva Concepts:
 * - Uses DriverManager.getConnection() with JDBC URL.
 * - Explicitly loads the MySQL 8.0 Driver class (com.mysql.cj.jdbc.Driver).
 * - try-with-resources: Ensures connections and statements close automatically.
 * - Security: Never logs passwords or prints credentials to console or logs.
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
     */
    public static synchronized void loadConfiguration() throws IOException {
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
            // Fallback to environment variables
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
     * Establishes and returns an active JDBC Connection to nextstep_db.
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
            if (e.getErrorCode() == 1045) { // Access Denied
                System.err.println("[ERROR] MySQL Authentication Failed: Invalid username or password in " + CONFIG_FILE_PATH);
            } else if (e.getErrorCode() == 1049) { // Unknown database
                System.err.println("[WARN] Database 'nextstep_db' not found. Run initializeDatabase() to create it.");
            } else if (e.getMessage() != null && e.getMessage().contains("Communications link failure")) {
                System.err.println("[ERROR] Could not connect to MySQL server. Ensure MySQL80 service is running on port 3306.");
            }
            throw e;
        }
    }

    /**
     * Connects to the MySQL Server instance without specifying a database name.
     * Used for creating databases and executing initial DDL scripts.
     */
    public static Connection getRootServerConnection() throws SQLException {
        if (!isConfigLoaded) {
            try {
                loadConfiguration();
            } catch (IOException e) {
                throw new SQLException("Failed to load database configuration", e);
            }
        }

        // Strip database name from URL (e.g., jdbc:mysql://localhost:3306/nextstep_db?params -> jdbc:mysql://localhost:3306/?params)
        String serverUrl = dbUrl;
        int portIndex = serverUrl.indexOf(":3306/");
        if (portIndex != -1) {
            int queryIndex = serverUrl.indexOf("?", portIndex);
            if (queryIndex != -1) {
                serverUrl = serverUrl.substring(0, portIndex + 6) + serverUrl.substring(queryIndex);
            } else {
                serverUrl = serverUrl.substring(0, portIndex + 6);
            }
        }

        return DriverManager.getConnection(serverUrl, dbUser, dbPassword);
    }

    /**
     * Reads and executes a SQL script file statement by statement.
     */
    public static void executeSqlScript(Connection conn, String scriptPath) throws IOException, SQLException {
        File file = new File(scriptPath);
        if (!file.exists()) {
            throw new IOException("SQL file not found at " + scriptPath);
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file));
             Statement stmt = conn.createStatement()) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                // Skip empty lines and comments
                if (trimmed.isEmpty() || trimmed.startsWith("--") || trimmed.startsWith("/*")) {
                    continue;
                }
                sb.append(line).append("\n");
                // When statement terminator is found, execute
                if (trimmed.endsWith(";")) {
                    String sql = sb.toString().trim();
                    // Remove trailing semicolon for JDBC
                    if (sql.endsWith(";")) {
                        sql = sql.substring(0, sql.length() - 1);
                    }
                    if (!sql.isEmpty()) {
                        stmt.execute(sql);
                    }
                    sb.setLength(0);
                }
            }
        }
    }

    /**
     * Automatically initializes nextstep_db and sample data if needed.
     */
    public static boolean initializeDatabase() {
        System.out.println("[INFO] Checking if nextstep_db requires initialization...");
        try (Connection serverConn = getRootServerConnection()) {
            System.out.println("[SUCCESS] Connected to MySQL Server.");

            // 1. Run schema.sql
            System.out.println("[INFO] Executing database/schema.sql...");
            executeSqlScript(serverConn, "database/schema.sql");
            System.out.println("[SUCCESS] Database 'nextstep_db' and 8 tables initialized.");

            // 2. Run sample_data.sql
            System.out.println("[INFO] Executing database/sample_data.sql...");
            executeSqlScript(serverConn, "database/sample_data.sql");
            System.out.println("[SUCCESS] Sample companies and jobs populated.");

            return true;
        } catch (Exception e) {
            System.err.println("[ERROR] Database initialization failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Executes a complete self-contained connection test and table verification.
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

            // Attempt connection; if DB is missing (1049), initialize it
            Connection conn = null;
            try {
                conn = getConnection();
            } catch (SQLException e) {
                if (e.getErrorCode() == 1049) {
                    System.out.println("[INFO] Database not found. Initializing now...");
                    boolean initialized = initializeDatabase();
                    if (initialized) {
                        conn = getConnection();
                    } else {
                        return false;
                    }
                } else {
                    throw e;
                }
            }

            try (Connection activeConn = conn) {
                if (activeConn != null && !activeConn.isClosed()) {
                    System.out.println("[SUCCESS] MySQL connection established successfully.");

                    DatabaseMetaData meta = activeConn.getMetaData();
                    System.out.println("[INFO] Database Product: " + meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion());
                    System.out.println("[INFO] Driver Name: " + meta.getDriverName() + " " + meta.getDriverVersion());

                    // Verify current database
                    try (Statement stmt = activeConn.createStatement();
                         ResultSet rs = stmt.executeQuery("SELECT DATABASE() AS current_db")) {
                        if (rs.next()) {
                            System.out.println("[SUCCESS] Connected database: " + rs.getString("current_db"));
                        }
                    }

                    // Verify tables
                    String[] expectedTables = {
                        "users", "students", "student_skills", "companies",
                        "jobs", "job_eligible_branches", "job_required_skills", "applications"
                    };
                    int foundCount = 0;
                    for (String table : expectedTables) {
                        try (ResultSet rs = meta.getTables("nextstep_db", null, table, new String[]{"TABLE"})) {
                            if (rs.next()) {
                                System.out.println("  [TABLE FOUND] " + table);
                                foundCount++;
                            } else {
                                System.out.println("  [TABLE MISSING] " + table);
                            }
                        }
                    }
                    System.out.println("[RESULT] Verified tables: " + foundCount + " / " + expectedTables.length);

                    System.out.println("[SUCCESS] Connection closed cleanly.");
                    System.out.println("--------------------------------------------------");
                    return foundCount == expectedTables.length;
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
