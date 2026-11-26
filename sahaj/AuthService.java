package com.nextstep.service;

import com.nextstep.dao.StudentDAO;
import com.nextstep.dao.UserDAO;
import com.nextstep.database.DatabaseConnection;
import com.nextstep.model.Role;
import com.nextstep.model.Student;
import com.nextstep.model.User;
import com.nextstep.util.ValidationUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public class AuthService {

    public static final String OFFICER_ACTIVATION_CODE = "123456";

    private final UserDAO userDAO;
    private final StudentDAO studentDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
        this.studentDAO = new StudentDAO();
    }

    public AuthService(UserDAO userDAO, StudentDAO studentDAO) {
        this.userDAO = userDAO;
        this.studentDAO = studentDAO;
    }

    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    public static boolean verifyPassword(String password, String storedHash) {
        if (password == null || storedHash == null) {
            return false;
        }
        return hashPassword(password).equals(storedHash);
    }

    public Student registerStudent(String username, String password, String email,
                                   String studentId, String fullName,
                                   String branch, double cgpa, int graduationYear) throws SQLException {
        return registerStudent(username, password, email, studentId, fullName, "N/A",
                branch, cgpa, 0, graduationYear, Collections.emptyList());
    }

    public Student registerStudent(String username, String password, String email,
                                   String studentId, String fullName,
                                   String branch, double cgpa, int graduationYear,
                                   List<String> skills) throws SQLException {
        return registerStudent(username, password, email, studentId, fullName, "N/A",
                branch, cgpa, 0, graduationYear, skills);
    }

    public Student registerStudent(String username, String password, String email,
                                   String studentId, String fullName, String phone,
                                   String branch, double cgpa, int activeBacklogs,
                                   int graduationYear, List<String> skills) throws SQLException {
        validateStudentInput(username, password, email, studentId, fullName, phone, branch, cgpa, activeBacklogs, graduationYear);
        if (userDAO.existsByUsername(username)) throw new IllegalArgumentException("Username is already taken.");
        if (userDAO.existsByEmail(email)) throw new IllegalArgumentException("Email is already registered.");
        if (studentDAO.existsByStudentId(studentId)) throw new IllegalArgumentException("Student ID / USN is already registered.");

        String passwordHash = hashPassword(password);
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                User user = new User(username, passwordHash, Role.STUDENT, email);
                if (!userDAO.createUser(conn, user) || user.getId() == 0) {
                    throw new SQLException("Failed to create user record.");
                }
                Student student = new Student(user.getId(), studentId, fullName, phone, branch, cgpa, activeBacklogs, graduationYear);
                if (!studentDAO.createStudent(conn, student) || student.getId() == 0) {
                    throw new SQLException("Failed to create student profile.");
                }
                if (skills != null && !skills.isEmpty()) {
                    List<String> cleanSkills = ValidationUtil.parseCommaSeparatedList(String.join(",", skills));
                    if (!cleanSkills.isEmpty()) {
                        studentDAO.saveStudentSkills(conn, student.getId(), cleanSkills);
                        cleanSkills.forEach(student::addSkill);
                    }
                }
                conn.commit();
                return student;
            } catch (Exception e) {
                conn.rollback();
                throw new SQLException("Registration failed: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private void validateStudentInput(String username, String password, String email, String studentId,
                                      String fullName, String phone, String branch,
                                      double cgpa, int backlogs, int gradYear) {
        if (!ValidationUtil.isValidUsername(username)) throw new IllegalArgumentException("Username must be 3-30 alphanumeric characters.");
        if (!ValidationUtil.isValidPassword(password)) throw new IllegalArgumentException("Password must be at least 6 characters.");
        if (!ValidationUtil.isValidEmail(email)) throw new IllegalArgumentException("Invalid email format.");
        if (ValidationUtil.isNullOrBlank(studentId)) throw new IllegalArgumentException("Student ID / USN cannot be empty.");
        if (ValidationUtil.isNullOrBlank(fullName)) throw new IllegalArgumentException("Full name cannot be empty.");
        if (ValidationUtil.isNullOrBlank(phone)) throw new IllegalArgumentException("Phone number cannot be empty.");
        if (!"N/A".equals(phone) && !ValidationUtil.isValidPhone(phone)) throw new IllegalArgumentException("Phone number must be 10 digits.");
        if (ValidationUtil.isNullOrBlank(branch)) throw new IllegalArgumentException("Branch cannot be empty.");
        if (!ValidationUtil.isValidCgpa(cgpa)) throw new IllegalArgumentException("CGPA must be between 0.00 and 10.00.");
        if (!ValidationUtil.isValidBacklogs(backlogs)) throw new IllegalArgumentException("Active backlogs cannot be negative.");
        if (!ValidationUtil.isValidGraduationYear(gradYear)) throw new IllegalArgumentException("Graduation year must be between 2020 and 2035.");
    }

    public boolean isOfficerActivationConfigured() {
        return true;
    }

    public boolean verifyOfficerActivationCode(String activationCode) {
        return activationCode != null && OFFICER_ACTIVATION_CODE.equals(activationCode.trim());
    }

    public User registerPlacementOfficer(String officerId, String fullName, String email,
                                         String username, String password, String activationCode) throws SQLException {
        if (!verifyOfficerActivationCode(activationCode)) {
            throw new IllegalArgumentException("Invalid 6-digit activation code.");
        }
        if (ValidationUtil.isNullOrBlank(officerId)) throw new IllegalArgumentException("Officer ID cannot be empty.");
        if (ValidationUtil.isNullOrBlank(fullName)) throw new IllegalArgumentException("Full name cannot be empty.");
        return registerPlacementOfficer(username, password, email);
    }

    public User registerPlacementOfficer(String username, String password, String email) throws SQLException {
        if (!ValidationUtil.isValidUsername(username)) throw new IllegalArgumentException("Username must be 3-30 alphanumeric characters.");
        if (!ValidationUtil.isValidPassword(password)) throw new IllegalArgumentException("Password must be at least 6 characters.");
        if (!ValidationUtil.isValidEmail(email)) throw new IllegalArgumentException("Invalid email format.");
        if (userDAO.existsByUsername(username)) throw new IllegalArgumentException("Username is already taken.");
        if (userDAO.existsByEmail(email)) throw new IllegalArgumentException("Email is already registered.");

        User officer = new User(username.trim(), hashPassword(password), Role.PLACEMENT_OFFICER, email.trim());
        if (!userDAO.createUser(officer) || officer.getId() == 0) {
            throw new SQLException("Failed to create placement officer account.");
        }
        return officer;
    }

    public boolean hasAnyPlacementOfficer() throws SQLException {
        return userDAO.existsByRole(Role.PLACEMENT_OFFICER);
    }

    public User login(String identifier, String password) throws SQLException {
        if (ValidationUtil.isNullOrBlank(identifier) || ValidationUtil.isNullOrBlank(password)) {
            return null;
        }
        String trimmed = identifier.trim();
        User user = userDAO.findByUsername(trimmed);
        if (user == null) {
            Student student = studentDAO.findByStudentId(trimmed);
            if (student != null) {
                user = userDAO.findById(student.getUserId());
            }
        }
        if (user != null && verifyPassword(password, user.getPasswordHash())) {
            return user;
        }
        return null;
    }
}
