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
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.Collections;
import java.util.List;

/**
 * AuthService
 * 
 * Central business service handling user registration, authentication,
 * and secure password management.
 * 
 * College Viva Concepts:
 * - PBKDF2 (Password-Based Key Derivation Function 2): Industry standard hashing.
 * - Salt: Unique random bytes preventing rainbow table attacks.
 * - Constant-time comparison: MessageDigest.isEqual prevents timing attacks.
 * - Transaction Management: setAutoCommit(false), commit(), rollback() ensure atomic registration.
 */
public class AuthService {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 10000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;

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

    /**
     * Hashes a plaintext password using PBKDF2WithHmacSHA256 with a random salt.
     * Encoded format: PBKDF2$10000$<saltHex>$<hashHex>
     */
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_BYTES];
            random.nextBytes(salt);

            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, HASH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();

            HexFormat hex = HexFormat.of();
            return "PBKDF2$" + ITERATIONS + "$" + hex.formatHex(salt) + "$" + hex.formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error hashing password with PBKDF2", e);
        }
    }

    /**
     * Verifies a plaintext password against a stored PBKDF2 encoded hash.
     */
    public static boolean verifyPassword(String password, String storedHash) {
        if (password == null || storedHash == null || !storedHash.startsWith("PBKDF2$")) {
            return false;
        }

        try {
            String[] parts = storedHash.split("\\$");
            if (parts.length != 4) {
                return false;
            }

            int iterations = Integer.parseInt(parts[1]);
            HexFormat hex = HexFormat.of();
            byte[] salt = hex.parseHex(parts[2]);
            byte[] expectedHash = hex.parseHex(parts[3]);

            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, expectedHash.length * 8);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] actualHash = factory.generateSecret(spec).getEncoded();

            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Registers a student using Page 3 (Student Sign Up) form inputs,
     * applying standard defaults (phone = "N/A", activeBacklogs = 0, skills = empty).
     * 
     * @param username student account username
     * @param password student account plaintext password (hashed with PBKDF2)
     * @param email student email address
     * @param studentId University Roll No / USN
     * @param fullName student full name
     * @param branch degree branch
     * @param cgpa cumulative GPA (0.00 - 10.00)
     * @param graduationYear expected graduation year (2020 - 2035)
     * @return Registered Student profile
     * @throws IllegalArgumentException on validation failure
     * @throws SQLException on database error
     */
    public Student registerStudent(String username, String password, String email,
                                   String studentId, String fullName,
                                   String branch, double cgpa, int graduationYear) throws SQLException {
        return registerStudent(username, password, email, studentId, fullName, "N/A",
                               branch, cgpa, 0, graduationYear, Collections.emptyList());
    }

    public Student registerStudent(String username, String password, String email,
                                   String studentId, String fullName, String phone,
                                   String branch, double cgpa, int activeBacklogs,
                                   int graduationYear, List<String> skills) throws SQLException {

        // 1. Input Validation
        if (!ValidationUtil.isValidUsername(username)) {
            throw new IllegalArgumentException("Username must be between 3 and 30 alphanumeric characters.");
        }
        if (!ValidationUtil.isValidPassword(password)) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }
        if (!ValidationUtil.isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format.");
        }
        if (ValidationUtil.isNullOrBlank(studentId)) {
            throw new IllegalArgumentException("Student ID / USN cannot be empty.");
        }
        if (ValidationUtil.isNullOrBlank(fullName)) {
            throw new IllegalArgumentException("Full name cannot be empty.");
        }
        if (ValidationUtil.isNullOrBlank(phone)) {
            throw new IllegalArgumentException("Phone number cannot be empty.");
        }
        if (ValidationUtil.isNullOrBlank(branch)) {
            throw new IllegalArgumentException("Branch cannot be empty.");
        }
        if (!ValidationUtil.isValidCgpa(cgpa)) {
            throw new IllegalArgumentException("CGPA must be between 0.00 and 10.00.");
        }
        if (!ValidationUtil.isValidBacklogs(activeBacklogs)) {
            throw new IllegalArgumentException("Active backlogs cannot be negative.");
        }
        if (!ValidationUtil.isValidGraduationYear(graduationYear)) {
            throw new IllegalArgumentException("Graduation year must be between 2020 and 2035.");
        }

        // 2. Uniqueness Checks
        if (userDAO.existsByUsername(username)) {
            throw new IllegalArgumentException("Username is already taken.");
        }
        if (studentDAO.existsByStudentId(studentId)) {
            throw new IllegalArgumentException("Student ID / USN is already registered.");
        }

        // 3. Hash Password
        String passwordHash = hashPassword(password);

        // 4. Atomic Transaction: Insert User + Student + Skills
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Begin transaction

            try {
                // A. Insert User Account
                User user = new User(username, passwordHash, Role.STUDENT, email);
                boolean userCreated = userDAO.createUser(conn, user);
                if (!userCreated || user.getId() == 0) {
                    throw new SQLException("Failed to create user record.");
                }

                // B. Insert Student Profile
                Student student = new Student(user.getId(), studentId, fullName, phone,
                                              branch, cgpa, activeBacklogs, graduationYear);
                boolean studentCreated = studentDAO.createStudent(conn, student);
                if (!studentCreated || student.getId() == 0) {
                    throw new SQLException("Failed to create student profile record.");
                }

                // C. Save Skills
                if (skills != null && !skills.isEmpty()) {
                    studentDAO.saveStudentSkills(conn, student.getId(), skills);
                    for (String skill : skills) {
                        student.addSkill(skill);
                    }
                }

                conn.commit(); // Transaction successful!
                return student;
            } catch (Exception e) {
                conn.rollback(); // Roll back on any failure
                throw new SQLException("Registration transaction failed: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * System property and environment variable keys for authorized officer activation.
     */
    public static final String ACTIVATION_CODE_PROP = "nextstep.officer.activation.code";
    public static final String ACTIVATION_CODE_ENV = "NEXTSTEP_OFFICER_ACTIVATION_CODE";

    /**
     * Verifies whether the provided activation code authorizes placement officer registration.
     * Restricted to configured administrative activation code (via system property or environment variable).
     * Falls back to institutional default 'CAMPUS_OFFICER_2026' to allow authorized academic testing
     * while rejecting unauthorized public registration with arbitrary codes.
     */
    public boolean verifyOfficerActivationCode(String activationCode) {
        if (ValidationUtil.isNullOrBlank(activationCode)) {
            return false;
        }

        String authorizedCode = System.getProperty(ACTIVATION_CODE_PROP);
        if (authorizedCode == null || authorizedCode.trim().isEmpty()) {
            authorizedCode = System.getenv(ACTIVATION_CODE_ENV);
        }
        if (authorizedCode == null || authorizedCode.trim().isEmpty()) {
            authorizedCode = "CAMPUS_OFFICER_2026";
        }

        return authorizedCode.trim().equals(activationCode.trim());
    }

    /**
     * Overloaded method for Page 5 (Officer Sign Up) validating the activation code,
     * ensuring username uniqueness, and persisting the Placement Officer user account.
     */
    public User registerPlacementOfficer(String officerId, String fullName, String email,
                                         String username, String password, String activationCode) throws SQLException {
        if (!verifyOfficerActivationCode(activationCode)) {
            throw new IllegalArgumentException("Invalid activation code. Placement officer registration unauthorized.");
        }
        if (ValidationUtil.isNullOrBlank(officerId)) {
            throw new IllegalArgumentException("Officer ID cannot be empty.");
        }
        if (ValidationUtil.isNullOrBlank(fullName)) {
            throw new IllegalArgumentException("Full name cannot be empty.");
        }
        return registerPlacementOfficer(username, password, email);
    }

    /**
     * Registers a new placement officer account.
     * For administrative setup; not accessible via public student registration menu.
     * 
     * @param username officer username
     * @param password plaintext password (will be hashed with PBKDF2)
     * @param email officer email
     * @return User created placement officer user record
     * @throws IllegalArgumentException on validation failures
     * @throws SQLException on database errors
     */
    public User registerPlacementOfficer(String username, String password, String email) throws SQLException {
        if (!ValidationUtil.isValidUsername(username)) {
            throw new IllegalArgumentException("Username must be between 3 and 30 alphanumeric characters.");
        }
        if (!ValidationUtil.isValidPassword(password)) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }
        if (!ValidationUtil.isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format.");
        }
        if (userDAO.existsByUsername(username)) {
            throw new IllegalArgumentException("Username is already taken.");
        }

        String passwordHash = hashPassword(password);
        User officer = new User(username.trim(), passwordHash, Role.PLACEMENT_OFFICER, email.trim());
        boolean created = userDAO.createUser(officer);
        if (!created || officer.getId() == 0) {
            throw new SQLException("Failed to create placement officer account.");
        }
        return officer;
    }

    /**
     * Checks if any placement officer is currently registered in the database.
     */
    public boolean hasAnyPlacementOfficer() throws SQLException {
        return userDAO.existsByRole(Role.PLACEMENT_OFFICER);
    }

    /**
     * Authenticates an existing user using username and password.
     * 
     * @param username username
     * @param password plaintext password
     * @return User object if authenticated, null otherwise
     * @throws SQLException if a database error occurs
     */
    public User login(String identifier, String password) throws SQLException {
        if (ValidationUtil.isNullOrBlank(identifier) || ValidationUtil.isNullOrBlank(password)) {
            return null;
        }

        String trimmed = identifier.trim();
        User user = userDAO.findByUsername(trimmed);
        if (user == null) {
            // Support student authentication using Student ID / University Roll No (USN)
            Student student = studentDAO.findByStudentId(trimmed);
            if (student != null) {
                user = userDAO.findById(student.getUserId());
            }
        }

        if (user == null) {
            return null; // Return null generically to prevent identifier enumeration
        }

        if (verifyPassword(password, user.getPasswordHash())) {
            return user;
        }

        return null; // Invalid password
    }
}
