import com.nextstep.database.DatabaseConnection;
import java.sql.*;

public class InspectDatabase {
    public static void main(String[] args) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("=== 1. TABLE COUNTS ===");
            String[] tables = {"users", "students", "student_skills", "companies", "jobs", "job_eligible_branches", "job_required_skills", "applications"};
            for (String t : tables) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + t)) {
                    if (rs.next()) {
                        System.out.println("Table " + t + ": " + rs.getInt(1) + " rows");
                    }
                }
            }

            System.out.println("\n=== 2. SAMPLE COMPANIES ===");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, name, industry, email, location FROM companies")) {
                while (rs.next()) {
                    System.out.println("ID: " + rs.getInt("id") + " | " + rs.getString("name") + " (" + rs.getString("industry") + ") - " + rs.getString("location"));
                }
            }

            System.out.println("\n=== 3. SAMPLE JOBS ===");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT j.id, j.title, c.name, j.ctc_lpa, j.min_cgpa, j.max_backlogs, j.status FROM jobs j JOIN companies c ON j.company_id = c.id")) {
                while (rs.next()) {
                    System.out.println("Job: " + rs.getString("title") + " at " + rs.getString("name") + " | CTC: " + rs.getBigDecimal("ctc_lpa") + " LPA | Min CGPA: " + rs.getDouble("min_cgpa") + " | Status: " + rs.getString("status"));
                }
            }

            System.out.println("\n=== 4. UNIQUE CONSTRAINTS & INDEXES ===");
            for (String t : tables) {
                try (ResultSet rs = meta.getIndexInfo("nextstep_db", null, t, false, false)) {
                    while (rs.next()) {
                        String idxName = rs.getString("INDEX_NAME");
                        boolean nonUnique = rs.getBoolean("NON_UNIQUE");
                        String colName = rs.getString("COLUMN_NAME");
                        if ("PRIMARY".equals(idxName)) {
                            // Primary key
                        } else if (!nonUnique) {
                            System.out.println("Unique Index on " + t + ": " + idxName + " (" + colName + ")");
                        }
                    }
                }
            }

            System.out.println("\n=== 5. FOREIGN KEYS ===");
            for (String t : tables) {
                try (ResultSet rs = meta.getImportedKeys("nextstep_db", null, t)) {
                    while (rs.next()) {
                        String fkTable = rs.getString("FKTABLE_NAME");
                        String fkCol = rs.getString("FKCOLUMN_NAME");
                        String pkTable = rs.getString("PKTABLE_NAME");
                        String pkCol = rs.getString("PKCOLUMN_NAME");
                        System.out.println("FK: " + fkTable + "." + fkCol + " -> " + pkTable + "." + pkCol);
                    }
                }
            }
        }
    }
}
