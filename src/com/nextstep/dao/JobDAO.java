package com.nextstep.dao;

import com.nextstep.database.DatabaseConnection;
import com.nextstep.model.Job;

import java.math.BigDecimal;
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
 * JobDAO
 * 
 * Read-focused Data Access Object (DAO) for job openings and eligibility criteria.
 */
public class JobDAO {

    /**
     * Retrieves all available job openings, joining company details and populating
     * eligible branches and required skills.
     */
    public List<Job> findAllJobs() throws SQLException {
        List<Job> jobs = new ArrayList<>();
        String sql = "SELECT j.id, j.company_id, c.name AS company_name, j.title, j.description, " +
                     "j.ctc_lpa, j.location, j.min_cgpa, j.max_backlogs, j.graduation_year, j.status, j.created_at " +
                     "FROM jobs j " +
                     "JOIN companies c ON j.company_id = c.id " +
                     "ORDER BY j.created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                jobs.add(mapRowToJob(conn, rs));
            }
        }
        return jobs;
    }

    /**
     * Finds a job opening by its primary key ID.
     */
    public Job findById(int id) throws SQLException {
        String sql = "SELECT j.id, j.company_id, c.name AS company_name, j.title, j.description, " +
                     "j.ctc_lpa, j.location, j.min_cgpa, j.max_backlogs, j.graduation_year, j.status, j.created_at " +
                     "FROM jobs j " +
                     "JOIN companies c ON j.company_id = c.id " +
                     "WHERE j.id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToJob(conn, rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all jobs posted by a specific company ID.
     */
    public List<Job> findByCompanyId(int companyId) throws SQLException {
        List<Job> jobs = new ArrayList<>();
        String sql = "SELECT j.id, j.company_id, c.name AS company_name, j.title, j.description, " +
                     "j.ctc_lpa, j.location, j.min_cgpa, j.max_backlogs, j.graduation_year, j.status, j.created_at " +
                     "FROM jobs j " +
                     "JOIN companies c ON j.company_id = c.id " +
                     "WHERE j.company_id = ? " +
                     "ORDER BY j.created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, companyId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    jobs.add(mapRowToJob(conn, rs));
                }
            }
        }
        return jobs;
    }

    /**
     * Retrieves eligible branches for a job opening.
     */
    public List<String> getEligibleBranches(Connection conn, int jobId) throws SQLException {
        List<String> branches = new ArrayList<>();
        String sql = "SELECT branch_name FROM job_eligible_branches WHERE job_id = ? ORDER BY branch_name";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, jobId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    branches.add(rs.getString("branch_name"));
                }
            }
        }
        return branches;
    }

    /**
     * Standalone query for eligible branches.
     */
    public List<String> getEligibleBranches(int jobId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getEligibleBranches(conn, jobId);
        }
    }

    /**
     * Retrieves required technical skills for a job opening.
     */
    public List<String> getRequiredSkills(Connection conn, int jobId) throws SQLException {
        List<String> skills = new ArrayList<>();
        String sql = "SELECT skill_name FROM job_required_skills WHERE job_id = ? ORDER BY skill_name";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, jobId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    skills.add(rs.getString("skill_name"));
                }
            }
        }
        return skills;
    }

    /**
     * Standalone query for required skills.
     */
    public List<String> getRequiredSkills(int jobId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getRequiredSkills(conn, jobId);
        }
    }

    private Job mapRowToJob(Connection conn, ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int companyId = rs.getInt("company_id");
        String companyName = rs.getString("company_name");
        String title = rs.getString("title");
        String description = rs.getString("description");
        BigDecimal ctcLpa = rs.getBigDecimal("ctc_lpa");
        String location = rs.getString("location");
        double minCgpa = rs.getDouble("min_cgpa");
        int maxBacklogs = rs.getInt("max_backlogs");
        int graduationYear = rs.getInt("graduation_year");
        String status = rs.getString("status");
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime createdAt = (ts != null) ? ts.toLocalDateTime() : null;

        List<String> branches = getEligibleBranches(conn, id);
        List<String> skills = getRequiredSkills(conn, id);

        Job job = new Job(id, companyId, title, description, ctcLpa, location,
                          minCgpa, maxBacklogs, graduationYear, status, createdAt,
                          branches, skills);
        job.setCompanyName(companyName);
        return job;
    }
}
