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
     * Determines the next expected job ID for display prior to insertion.
     */
    public int getNextJobId() throws SQLException {
        String sql = "SELECT COALESCE(MAX(id), 0) + 1 FROM jobs";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 1;
    }

    /**
     * Persists a new job opening along with its normalized eligible branches and required skills
     * in a single atomic database transaction.
     * 
     * @param job Job entity containing companyId, title, description, minCgpa, graduationYear, etc.
     * @param eligibleBranches list of branch names
     * @param requiredSkills list of required skill names
     * @return Job the persisted job with database-assigned primary key
     * @throws SQLException on database error
     */
    public Job createJob(Job job, List<String> eligibleBranches, List<String> requiredSkills) throws SQLException {
        if (job == null) {
            throw new IllegalArgumentException("Job cannot be null.");
        }

        String sql = "INSERT INTO jobs (company_id, title, description, ctc_lpa, location, min_cgpa, max_backlogs, graduation_year, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int generatedId;
                try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setInt(1, job.getCompanyId());
                    stmt.setString(2, job.getTitle());
                    stmt.setString(3, job.getDescription() != null ? job.getDescription() : "Campus Drive for " + job.getTitle());
                    stmt.setBigDecimal(4, job.getCtcLpa() != null ? job.getCtcLpa() : BigDecimal.ZERO);
                    stmt.setString(5, job.getLocation() != null ? job.getLocation() : "Campus");
                    stmt.setDouble(6, job.getMinCgpa());
                    stmt.setInt(7, job.getMaxBacklogs());
                    stmt.setInt(8, job.getGraduationYear());
                    stmt.setString(9, job.getStatus() != null ? job.getStatus() : "OPEN");

                    stmt.executeUpdate();

                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            generatedId = rs.getInt(1);
                        } else {
                            throw new SQLException("Creating job drive failed, no ID obtained.");
                        }
                    }
                }

                // Insert eligible branches
                if (eligibleBranches != null && !eligibleBranches.isEmpty()) {
                    String branchSql = "INSERT INTO job_eligible_branches (job_id, branch_name) VALUES (?, ?)";
                    try (PreparedStatement bStmt = conn.prepareStatement(branchSql)) {
                        for (String branch : eligibleBranches) {
                            String trimmed = branch.trim();
                            if (!trimmed.isEmpty()) {
                                bStmt.setInt(1, generatedId);
                                bStmt.setString(2, trimmed);
                                bStmt.addBatch();
                            }
                        }
                        bStmt.executeBatch();
                    }
                }

                // Insert required skills
                if (requiredSkills != null && !requiredSkills.isEmpty()) {
                    String skillSql = "INSERT INTO job_required_skills (job_id, skill_name) VALUES (?, ?)";
                    try (PreparedStatement sStmt = conn.prepareStatement(skillSql)) {
                        for (String skill : requiredSkills) {
                            String trimmed = skill.trim();
                            if (!trimmed.isEmpty()) {
                                sStmt.setInt(1, generatedId);
                                sStmt.setString(2, trimmed);
                                sStmt.addBatch();
                            }
                        }
                        sStmt.executeBatch();
                    }
                }

                conn.commit();
                job.setId(generatedId);
                job.setEligibleBranches(eligibleBranches);
                job.setRequiredSkills(requiredSkills);
                return job;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
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
