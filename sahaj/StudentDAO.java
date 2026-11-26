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

public class StudentDAO {

    private static final String SELECT_STUDENT =
            "SELECT id, user_id, student_id, full_name, phone, branch, cgpa, active_backlogs, graduation_year FROM students ";

    public boolean createStudent(Connection conn, Student student) throws SQLException {
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

            if (stmt.executeUpdate() == 0) return false;

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) student.setId(keys.getInt(1));
            }
            return true;
        }
    }

    public boolean createStudent(Student student) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return createStudent(conn, student);
        }
    }

    public void saveStudentSkills(Connection conn, int studentId, List<String> skills) throws SQLException {
        if (skills == null || skills.isEmpty()) return;
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

    public Student findById(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_STUDENT + "WHERE id = ?")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapRowToStudent(rs) : null;
            }
        }
    }

    public Student findByUserId(int userId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_STUDENT + "WHERE user_id = ?")) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapRowToStudent(rs) : null;
            }
        }
    }

    public Student findByStudentId(String studentId) throws SQLException {
        if (studentId == null || studentId.trim().isEmpty()) return null;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_STUDENT + "WHERE student_id = ?")) {
            stmt.setString(1, studentId.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapRowToStudent(rs) : null;
            }
        }
    }

    public boolean existsByStudentId(String studentId) throws SQLException {
        if (studentId == null || studentId.trim().isEmpty()) return false;
        String sql = "SELECT 1 FROM students WHERE student_id = ? LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, studentId.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int countStudents() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM students");
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public boolean updateStudent(Student student) throws SQLException {
        if (student == null) return false;
        String sql = "UPDATE students SET full_name = ?, phone = ?, branch = ?, cgpa = ?, active_backlogs = ?, graduation_year = ? WHERE id = ?";

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

    public void updateStudentSkills(Connection conn, int studentId, List<String> skills) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM student_skills WHERE student_id = ?")) {
            stmt.setInt(1, studentId);
            stmt.executeUpdate();
        }
        saveStudentSkills(conn, studentId, skills);
    }

    public void updateStudentSkills(int studentId, List<String> skills) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            updateStudentSkills(conn, studentId, skills);
        }
    }

    private Student mapRowToStudent(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        return new Student(
                id,
                rs.getInt("user_id"),
                rs.getString("student_id"),
                rs.getString("full_name"),
                rs.getString("phone"),
                rs.getString("branch"),
                rs.getDouble("cgpa"),
                rs.getInt("active_backlogs"),
                rs.getInt("graduation_year"),
                getStudentSkills(id)
        );
    }
}
