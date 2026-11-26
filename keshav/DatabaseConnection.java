package com.nextstep.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Simple JDBC Database Connection Manager for NextStep.
 * Reads MySQL URL, username, and password from config/db.properties.
 */
public class DatabaseConnection {

    private static final String CONFIG_PATH = "config/db.properties";

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;

    // Load MySQL JDBC Driver when class is loaded
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("[ERROR] MySQL JDBC Driver not found: " + e.getMessage());
        }
    }

    // Load database credentials from config/db.properties
    private static void loadConfig() throws SQLException {
        if (dbUrl != null) {
            return; // Already loaded
        }

        Properties props = new Properties();
        File configFile = new File(CONFIG_PATH);

        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
            } catch (IOException e) {
                throw new SQLException("Could not read " + CONFIG_PATH, e);
            }
        }

        dbUrl = props.getProperty("db.url", "jdbc:mysql://localhost:3306/nextstep_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        dbUser = props.getProperty("db.user", "root");
        dbPassword = props.getProperty("db.password", "");
    }

    /**
     * Returns an active connection to MySQL database.
     */
    public static Connection getConnection() throws SQLException {
        loadConfig();
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    /**
     * Runs a .sql script file (used by DemoService to load sample data).
     */
    public static void executeSqlScript(Connection conn, String scriptPath) throws IOException, SQLException {
        try (BufferedReader reader = new BufferedReader(new FileReader(scriptPath));
             Statement stmt = conn.createStatement()) {

            StringBuilder sql = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--") || trimmed.startsWith("/*")) {
                    continue;
                }
                sql.append(line).append("\n");
                if (trimmed.endsWith(";")) {
                    String query = sql.toString().trim();
                    stmt.execute(query.substring(0, query.length() - 1));
                    sql.setLength(0);
                }
            }
        }
    }
}
