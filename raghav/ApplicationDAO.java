package com.nextstep.dao;

import com.nextstep.database.DatabaseConnection;
import com.nextstep.model.Application;
import com.nextstep.model.ApplicationStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ApplicationDAO
 * 
 * Data Access Object (DAO) for student recruitment applications.
 * 
 * College Viva Concepts:
 * - PreparedStatement: Parameterized queries preventing SQL Injection.
 * - Unique Constraint: Traps MySQL error 1062 to prevent duplicate applications.
 * - JOIN Queries: Joins applications with jobs and companies to populate display metadata.
 */
public class ApplicationDAO {

    /**
     * Submits a new job application.
     * 
     * @param application Application entity
     * @return boolean true if created, false if duplicate or failed
     * @throws SQLException on database errors
     */
    public boolean createApplication(Application application) throws SQLException {
        if (application == null) {
            throw new IllegalArgumentException("Application cannot be null");
        }

        String sql = "INSERT INTO applications (student_id, job_id, status) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, application.getStudentId());
            stmt.setInt(2, application.getJobId());
            stmt.setString(3, application.getStatus().name());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                return false;
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    application.setId(generatedKeys.getInt(1));
                }
            }

            return true;
        } catch (SQLException e) {
            // MySQL Error Code 1062 = Duplicate entry (uq_student_job)
            if (e.getErrorCode() == 1062) {
                System.err.println("[WARN] Duplicate application: Student has already applied for this job.");
                return false;
            }
            throw e;
        }
    }

    /**
     * Checks if a student has already applied for a specific job.
     */
    public boolean hasStudentApplied(int studentId, int jobId) throws SQLException {
        String sql = "SELECT 1 FROM applications WHERE student_id = ? AND job_id = ? LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, studentId);
            stmt.setInt(2, jobId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Retrieves all applications submitted by a specific student.
     * Joins with jobs and companies to populate job title and company name.
     */
    public List<Application> findByStudentId(int studentId) throws SQLException {
        List<Application> list = new ArrayList<>();
        String sql = "SELECT a.id, a.student_id, a.job_id, a.status, a.applied_at, " +
                     "j.title AS job_title, c.name AS company_name " +
                     "FROM applications a " +
                     "JOIN jobs j ON a.job_id = j.id " +
                     "JOIN companies c ON j.company_id = c.id " +
                     "WHERE a.student_id = ? " +
                     "ORDER BY a.applied_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, studentId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToApplication(rs));
                }
            }
        }
        return list;
    }

    /**
     * Retrieves an application by its primary key ID with joined details.
     */
    public Application findById(int id) throws SQLException {
        String sql = "SELECT a.id, a.student_id, a.job_id, a.status, a.applied_at, " +
                     "j.title AS job_title, c.name AS company_name " +
                     "FROM applications a " +
                     "JOIN jobs j ON a.job_id = j.id " +
                     "JOIN companies c ON j.company_id = c.id " +
                     "WHERE a.id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToApplication(rs);
                }
            }
        }
        return null;
    }

    /**
     * Updates the recruitment lifecycle status of an application.
     */
    public boolean updateStatus(int applicationId, ApplicationStatus newStatus) throws SQLException {
        if (newStatus == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        String sql = "UPDATE applications SET status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newStatus.name());
            stmt.setInt(2, applicationId);

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Retrieves all applications submitted in the system.
     * Joins with students, jobs, and companies to populate complete metadata.
     * Used by placement officers for recruitment management and reports.
     * 
     * @return List of all Application records
     * @throws SQLException on database errors
     */
    public List<Application> findAllApplications() throws SQLException {
        List<Application> list = new ArrayList<>();
        String sql = "SELECT a.id, a.student_id, a.job_id, a.status, a.applied_at, " +
                     "s.student_id AS student_roll_no, s.full_name AS student_name, " +
                     "j.title AS job_title, c.name AS company_name " +
                     "FROM applications a " +
                     "JOIN students s ON a.student_id = s.id " +
                     "JOIN jobs j ON a.job_id = j.id " +
                     "JOIN companies c ON j.company_id = c.id " +
                     "ORDER BY a.applied_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToApplication(rs));
            }
        }
        return list;
    }

    /**
     * Counts the total number of applications filed across all recruitment drives.
     *
     * @return int total count of application records
     * @throws SQLException on database error
     */
    public int countApplications() throws SQLException {
        String sql = "SELECT COUNT(*) FROM applications";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    private Application mapRowToApplication(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int studentId = rs.getInt("student_id");
        int jobId = rs.getInt("job_id");
        ApplicationStatus status = ApplicationStatus.fromString(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("applied_at");
        LocalDateTime appliedAt = (ts != null) ? ts.toLocalDateTime() : null;

        String jobTitle = rs.getString("job_title");
        String companyName = rs.getString("company_name");

        Application app = new Application(id, studentId, jobId, status, appliedAt);
        app.setJobTitle(jobTitle);
        app.setCompanyName(companyName);

        try {
            app.setStudentName(rs.getString("student_name"));
        } catch (SQLException ignored) {
        }
        try {
            app.setStudentRollNumber(rs.getString("student_roll_no"));
        } catch (SQLException ignored) {
        }

        return app;
    }
}
