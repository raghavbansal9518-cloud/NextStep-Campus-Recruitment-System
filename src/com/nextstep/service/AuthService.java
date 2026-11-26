package com.nextstep.service;

import com.nextstep.dao.StudentDAO;
import com.nextstep.dao.UserDAO;
import com.nextstep.database.DatabaseConnection;
import com.nextstep.model.Role;
import com.nextstep.model.Student;
import com.nextstep.model.User;
import com.nextstep.util.ValidationUtil;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;

/**
 * AuthService — Handles Registration, Login, Password Hashing (PBKDF2), and Activation Security.
 */
public class AuthService {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 10000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;

    public static final String ACTIVATION_CODE_PROP = "nextstep.officer.activation.code";
    public static final String ACTIVATION_CODE_ENV = "NEXTSTEP_OFFICER_ACTIVATION_CODE";
    public static final String DEFAULT_ACTIVATION_CODE = "CAMPUS_OFFICER_2026";

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

    // ==================== 1. PASSWORD SECURITY (PBKDF2) ====================

    /** Hashes password with random salt. Format: PBKDF2$10000$salt$hash */
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        try {
            byte[] salt = new byte[SALT_BYTES];
            new SecureRandom().nextBytes(salt);

            byte[] hash = pbkdf2(password, salt, ITERATIONS, HASH_BITS);
            HexFormat hex = HexFormat.of();
            return "PBKDF2$" + ITERATIONS + "$" + hex.formatHex(salt) + "$" + hex.formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    /** Verifies plain password against stored PBKDF2 hash. */
    public static boolean verifyPassword(String password, String storedHash) {
        if (password == null || storedHash == null || !storedHash.startsWith("PBKDF2$")) {
            return false;
        }
        try {
            String[] parts = storedHash.split("\\$");
            if (parts.length != 4) return false;

            int iterations = Integer.parseInt(parts[1]);
            HexFormat hex = HexFormat.of();
            byte[] salt = hex.parseHex(parts[2]);
            byte[] expectedHash = hex.parseHex(parts[3]);

            byte[] actualHash = pbkdf2(password, salt, iterations, expectedHash.length * 8);
            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations, int bits) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, bits);
        return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
    }

    // ==================== 2. STUDENT REGISTRATION (TRANSACTION) ====================

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
        // Step 1: Validate inputs
        validateStudentInput(username, password, email, studentId, fullName, phone, branch, cgpa, activeBacklogs, graduationYear);

        // Step 2: Check duplicates
        if (userDAO.existsByUsername(username)) throw new IllegalArgumentException("Username is already taken.");
        if (studentDAO.existsByStudentId(studentId)) throw new IllegalArgumentException("Student ID / USN is already registered.");

        // Step 3: Hash password
        String passwordHash = hashPassword(password);

        // Step 4: Atomic MySQL Transaction (Insert User + Student + Skills)
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

                List<String> cleanSkills = deduplicateSkills(skills);
                if (!cleanSkills.isEmpty()) {
                    studentDAO.saveStudentSkills(conn, student.getId(), cleanSkills);
                    cleanSkills.forEach(student::addSkill);
                }

                conn.commit();
                return student;
            } catch (Exception e) {
                conn.rollback();
                throw new SQLException("Registration failed (rolled back): " + e.getMessage(), e);
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

    private List<String> deduplicateSkills(List<String> skills) {
        if (skills == null || skills.isEmpty()) return Collections.emptyList();
        return ValidationUtil.parseCommaSeparatedList(String.join(",", skills));
    }

    // ==================== 3. PLACEMENT OFFICER SECURITY ====================

    public String getAuthorizedActivationCode() {
        String code = System.getProperty(ACTIVATION_CODE_PROP);
        if (ValidationUtil.isNullOrBlank(code)) code = System.getenv(ACTIVATION_CODE_ENV);
        if (ValidationUtil.isNullOrBlank(code)) code = DEFAULT_ACTIVATION_CODE;
        return code.trim();
    }

    public boolean isOfficerActivationConfigured() {
        return true;
    }

    public boolean verifyOfficerActivationCode(String activationCode) {
        return !ValidationUtil.isNullOrBlank(activationCode)
                && getAuthorizedActivationCode().equalsIgnoreCase(activationCode.trim());
    }

    public User registerPlacementOfficer(String officerId, String fullName, String email,
                                         String username, String password, String activationCode) throws SQLException {
        if (!verifyOfficerActivationCode(activationCode)) {
            throw new IllegalArgumentException("Invalid activation code. Officer registration unauthorized.");
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

        User officer = new User(username.trim(), hashPassword(password), Role.PLACEMENT_OFFICER, email.trim());
        if (!userDAO.createUser(officer) || officer.getId() == 0) {
            throw new SQLException("Failed to create placement officer account.");
        }
        return officer;
    }

    public boolean hasAnyPlacementOfficer() throws SQLException {
        return userDAO.existsByRole(Role.PLACEMENT_OFFICER);
    }

    // ==================== 4. LOGIN (USERNAME OR USN) ====================

    public User login(String identifier, String password) throws SQLException {
        if (ValidationUtil.isNullOrBlank(identifier) || ValidationUtil.isNullOrBlank(password)) {
            return null;
        }
        String trimmed = identifier.trim();

        // Try finding by username first; if not found, try Student ID (USN)
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
