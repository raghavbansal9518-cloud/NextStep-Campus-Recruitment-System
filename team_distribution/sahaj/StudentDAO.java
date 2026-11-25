package com.nextstep.dao;

import com.nextstep.database.DatabaseConnection;
import com.nextstep.model.Student;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * StudentDAO
 * 
 * Data Access Object (DAO) for managing students and student_skills tables.
 * 
 * College Viva Concepts:
 * - One-to-One with users (user_id FK).
 * - One-to-Many normalized skill storage (student_skills table).
 * - Transaction support: Overloaded methods take Connection for atomic commit/rollback.
 */
public class StudentDAO {

    /**
     * Inserts a student profile record within an existing connection / transaction.
     * Assigns the database-generated ID to student.setId().
     */
    public boolean createStudent(Connection conn, Student student) throws SQLException {
        if (conn == null || student == null) {
            throw new IllegalArgumentException("Connection and student cannot be null");
        }

        String sql = "INSERT INTO students (user_id, student_id, full_name, phone, branch, cgpa, active_backlogs, graduation_year) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, student.getUserId());
            stmt.setString(2, student.getStudentId().trim());
            stmt.setString(3, student.getFullName().trim());
            stmt.setString(4, student.getPhone().trim());
            stmt.setString(5, student.getBranch().trim());
            stmt.setDouble(6, student.getCgpa());
            stmt.setInt(7, student.getActiveBacklogs());
            stmt.setInt(8, student.getGraduationYear());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                return false;
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    student.setId(generatedKeys.getInt(1));
                }
            }

            return true;
        }
    }

    /**
     * Standalone insertion opening its own connection.
     */
    public boolean createStudent(Student student) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return createStudent(conn, student);
        }
    }

    /**
     * Saves a list of skills for a student within an existing connection.
     * Uses INSERT IGNORE to safely handle potential duplicate entries.
     */
    public void saveStudentSkills(Connection conn, int studentId, List<String> skills) throws SQLException {
        if (conn == null || skills == null || skills.isEmpty()) {
            return;
        }

        String sql = "INSERT IGNORE INTO student_skills (student_id, skill_name) VALUES (?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (String skill : skills) {
                if (skill != null && !skill.trim().isEmpty()) {
                    stmt.setInt(1, studentId);
                    stmt.setString(2, skill.trim());
                    stmt.executeUpdate();
                }
            }
        }
    }

    /**
     * Retrieves all normalized skills for a given student ID.
     */
    public List<String> getStudentSkills(int studentId) throws SQLException {
        List<String> skills = new ArrayList<>();
        String sql = "SELECT skill_name FROM student_skills WHERE student_id = ? ORDER BY skill_name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, studentId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    skills.add(rs.getString("skill_name"));
                }
            }
        }
        return skills;
    }

    /**
     * Finds a student by their primary key database ID.
     */
    public Student findById(int id) throws SQLException {
        String sql = "SELECT id, user_id, student_id, full_name, phone, branch, cgpa, active_backlogs, graduation_year " +
                     "FROM students WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToStudent(rs);
                }
            }
        }
        return null;
    }

    /**
     * Finds a student by their associated user account ID.
     */
    public Student findByUserId(int userId) throws SQLException {
        String sql = "SELECT id, user_id, student_id, full_name, phone, branch, cgpa, active_backlogs, graduation_year " +
                     "FROM students WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToStudent(rs);
                }
            }
        }
        return null;
    }

    /**
     * Finds a student by university registration number / USN.
     */
    public Student findByStudentId(String studentId) throws SQLException {
        if (studentId == null || studentId.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT id, user_id, student_id, full_name, phone, branch, cgpa, active_backlogs, graduation_year " +
                     "FROM students WHERE student_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, studentId.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToStudent(rs);
                }
            }
        }
        return null;
    }

    /**
     * Checks if a university student ID (USN) already exists.
     */
    public boolean existsByStudentId(String studentId) throws SQLException {
        if (studentId == null || studentId.trim().isEmpty()) {
            return false;
        }

        String sql = "SELECT 1 FROM students WHERE student_id = ? LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, studentId.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Counts the total number of registered students in the system.
     *
     * @return int total count of student records
     * @throws SQLException on database error
     */
    public int countStudents() throws SQLException {
        String sql = "SELECT COUNT(*) FROM students";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    /**
     * Updates an existing student's academic and profile details.
     */
    public boolean updateStudent(Student student) throws SQLException {
        if (student == null) {
            return false;
        }

        String sql = "UPDATE students SET full_name = ?, phone = ?, branch = ?, cgpa = ?, active_backlogs = ?, graduation_year = ? " +
                     "WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, student.getFullName().trim());
            stmt.setString(2, student.getPhone().trim());
            stmt.setString(3, student.getBranch().trim());
            stmt.setDouble(4, student.getCgpa());
            stmt.setInt(5, student.getActiveBacklogs());
            stmt.setInt(6, student.getGraduationYear());
            stmt.setInt(7, student.getId());

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Atomically updates a student's technical skills by replacing existing skills with new ones.
     */
    public void updateStudentSkills(Connection conn, int studentId, List<String> skills) throws SQLException {
        if (conn == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        String deleteSql = "DELETE FROM student_skills WHERE student_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setInt(1, studentId);
            stmt.executeUpdate();
        }
        if (skills != null && !skills.isEmpty()) {
            saveStudentSkills(conn, studentId, skills);
        }
    }

    /**
     * Standalone skill update opening its own database connection.
     */
    public void updateStudentSkills(int studentId, List<String> skills) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            updateStudentSkills(conn, studentId, skills);
        }
    }

    /**
     * Helper method to map a ResultSet row to a Student model and populate their skills.
     */
    private Student mapRowToStudent(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int userId = rs.getInt("user_id");
        String studentId = rs.getString("student_id");
        String fullName = rs.getString("full_name");
        String phone = rs.getString("phone");
        String branch = rs.getString("branch");
        double cgpa = rs.getDouble("cgpa");
        int activeBacklogs = rs.getInt("active_backlogs");
        int graduationYear = rs.getInt("graduation_year");

        List<String> skills = getStudentSkills(id);

        return new Student(id, userId, studentId, fullName, phone, branch, cgpa, activeBacklogs, graduationYear, skills);
    }
}
