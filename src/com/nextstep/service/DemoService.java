package com.nextstep.service;

import com.nextstep.dao.CompanyDAO;
import com.nextstep.dao.JobDAO;
import com.nextstep.dao.StudentDAO;
import com.nextstep.dao.UserDAO;
import com.nextstep.database.DatabaseConnection;
import com.nextstep.model.Company;
import com.nextstep.model.Job;
import com.nextstep.model.Role;
import com.nextstep.model.Student;
import com.nextstep.model.User;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * DemoService
 *
 * Central service for repeatable demonstration mode, safe demo data management,
 * and deterministic scenario preparation for the NextStep Campus Recruitment System.
 *
 * College CA-3 Principles:
 * - Pure Java SE & JDBC architecture.
 * - Idempotent initialization: Reuses existing demo accounts without duplication.
 * - Safe demo reset: Cleans only identified demo accounts; never touches real records.
 * - Deterministic, documented credentials using existing PBKDF2WithHmacSHA256 hashing.
 */
public class DemoService {

    // Documented, stable demonstration credentials
    public static final String DEMO_PASSWORD = "NextStep@2026";
    public static final String DEMO_OFFICER_ACTIVATION_CODE = "CAMPUS_OFFICER_2026";

    // Scenario A: Fully Eligible Student
    public static final String DEMO_STUDENT_USERNAME = "vihaan_demo";
    public static final String DEMO_STUDENT_USN = "2510990750";
    public static final String DEMO_STUDENT_NAME = "Vihaan Mehta";
    public static final String DEMO_STUDENT_EMAIL = "vihaan.demo@nextstep.campus";
    public static final String DEMO_STUDENT_PHONE = "9876543210";
    public static final String DEMO_STUDENT_BRANCH = "Computer Science";
    public static final double DEMO_STUDENT_CGPA = 8.75;
    public static final int DEMO_STUDENT_BACKLOGS = 0;
    public static final int DEMO_STUDENT_GRAD_YEAR = 2026;
    public static final List<String> DEMO_STUDENT_SKILLS = Arrays.asList("Java", "SQL", "OOP", "Git");

    // Scenario B: Skill-Gap Student
    public static final String DEMO_STUDENT2_USERNAME = "kabir_demo";
    public static final String DEMO_STUDENT2_USN = "2510990751";
    public static final String DEMO_STUDENT2_NAME = "Kabir Malhotra";
    public static final String DEMO_STUDENT2_EMAIL = "kabir.demo@nextstep.campus";
    public static final String DEMO_STUDENT2_PHONE = "9876543211";
    public static final String DEMO_STUDENT2_BRANCH = "Information Technology";
    public static final double DEMO_STUDENT2_CGPA = 8.10;
    public static final int DEMO_STUDENT2_BACKLOGS = 0;
    public static final int DEMO_STUDENT2_GRAD_YEAR = 2026;
    public static final List<String> DEMO_STUDENT2_SKILLS = Arrays.asList("Java", "SQL");

    // Scenario C: Academically Ineligible Student
    public static final String DEMO_STUDENT3_USERNAME = "arjun_demo";
    public static final String DEMO_STUDENT3_USN = "2510990752";
    public static final String DEMO_STUDENT3_NAME = "Arjun Khanna";
    public static final String DEMO_STUDENT3_EMAIL = "arjun.demo@nextstep.campus";
    public static final String DEMO_STUDENT3_PHONE = "9876543212";
    public static final String DEMO_STUDENT3_BRANCH = "Mechanical Engineering";
    public static final double DEMO_STUDENT3_CGPA = 6.20;
    public static final int DEMO_STUDENT3_BACKLOGS = 3;
    public static final int DEMO_STUDENT3_GRAD_YEAR = 2026;
    public static final List<String> DEMO_STUDENT3_SKILLS = Arrays.asList("Java", "Python");

    // Placement Officer
    public static final String DEMO_OFFICER_USERNAME = "officer_vihaan";
    public static final String DEMO_OFFICER_ID = "TPO-2026-001";
    public static final String DEMO_OFFICER_NAME = "Prof. Vihaan Mehta";
    public static final String DEMO_OFFICER_EMAIL = "officer.vihaan@nextstep.campus";

    private final AuthService authService;
    private final UserDAO userDAO;
    private final StudentDAO studentDAO;
    private final CompanyDAO companyDAO;
    private final JobDAO jobDAO;

    public DemoService() {
        this(new AuthService(), new UserDAO(), new StudentDAO(), new CompanyDAO(), new JobDAO());
    }

    public DemoService(AuthService authService, UserDAO userDAO, StudentDAO studentDAO,
                       CompanyDAO companyDAO, JobDAO jobDAO) {
        this.authService = authService;
        this.userDAO = userDAO;
        this.studentDAO = studentDAO;
        this.companyDAO = companyDAO;
        this.jobDAO = jobDAO;
    }

    /**
     * Checks if the baseline demonstration accounts are present in the database.
     */
    public boolean isDemoDataPrepared() {
        try {
            boolean hasStudent = userDAO.existsByUsername(DEMO_STUDENT_USERNAME) &&
                    studentDAO.existsByStudentId(DEMO_STUDENT_USN);
            boolean hasOfficer = userDAO.existsByUsername(DEMO_OFFICER_USERNAME);
            boolean hasJobs = !jobDAO.findAllJobs().isEmpty();
            return hasStudent && hasOfficer && hasJobs;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Prepares demonstration data idempotently.
     * If records already exist, they are reused without modification or duplicate creation.
     */
    public DemoSetupResult prepareDemoData() throws SQLException {
        boolean existedPrior = isDemoDataPrepared();
        int createdStudents = 0;
        int createdOfficers = 0;
        int createdCompanies = 0;
        int createdJobs = 0;

        // 1. Ensure sample companies & jobs exist in database
        try (Connection conn = DatabaseConnection.getConnection()) {
            List<Company> companies = companyDAO.findAllCompanies();
            List<Job> jobs = jobDAO.findAllJobs();
            if (companies.isEmpty() || jobs.isEmpty()) {
                File sampleFile = new File("database/sample_data.sql");
                if (sampleFile.exists()) {
                    DatabaseConnection.executeSqlScript(conn, "database/sample_data.sql");
                    createdCompanies = companyDAO.findAllCompanies().size();
                    createdJobs = jobDAO.findAllJobs().size();
                }
            }
        } catch (Exception e) {
            // Non-fatal if sample_data already executed
        }

        // Ensure officer activation code is set in environment for registration
        if (!authService.isOfficerActivationConfigured()) {
            System.setProperty(AuthService.ACTIVATION_CODE_PROP, DEMO_OFFICER_ACTIVATION_CODE);
        }

        // 2. Provision Placement Officer if missing
        if (!userDAO.existsByUsername(DEMO_OFFICER_USERNAME)) {
            authService.registerPlacementOfficer(
                    DEMO_OFFICER_ID,
                    DEMO_OFFICER_NAME,
                    DEMO_OFFICER_EMAIL,
                    DEMO_OFFICER_USERNAME,
                    DEMO_PASSWORD,
                    DEMO_OFFICER_ACTIVATION_CODE
            );
            createdOfficers++;
        }

        // 3. Provision Student 1: Vihaan Mehta (Eligible)
        if (!userDAO.existsByUsername(DEMO_STUDENT_USERNAME) && !studentDAO.existsByStudentId(DEMO_STUDENT_USN)) {
            authService.registerStudent(
                    DEMO_STUDENT_USERNAME,
                    DEMO_PASSWORD,
                    DEMO_STUDENT_EMAIL,
                    DEMO_STUDENT_USN,
                    DEMO_STUDENT_NAME,
                    DEMO_STUDENT_PHONE,
                    DEMO_STUDENT_BRANCH,
                    DEMO_STUDENT_CGPA,
                    DEMO_STUDENT_BACKLOGS,
                    DEMO_STUDENT_GRAD_YEAR,
                    DEMO_STUDENT_SKILLS
            );
            createdStudents++;
        }

        // 4. Provision Student 2: Kabir Malhotra (Skill Gap)
        if (!userDAO.existsByUsername(DEMO_STUDENT2_USERNAME) && !studentDAO.existsByStudentId(DEMO_STUDENT2_USN)) {
            authService.registerStudent(
                    DEMO_STUDENT2_USERNAME,
                    DEMO_PASSWORD,
                    DEMO_STUDENT2_EMAIL,
                    DEMO_STUDENT2_USN,
                    DEMO_STUDENT2_NAME,
                    DEMO_STUDENT2_PHONE,
                    DEMO_STUDENT2_BRANCH,
                    DEMO_STUDENT2_CGPA,
                    DEMO_STUDENT2_BACKLOGS,
                    DEMO_STUDENT2_GRAD_YEAR,
                    DEMO_STUDENT2_SKILLS
            );
            createdStudents++;
        }

        // 5. Provision Student 3: Arjun Khanna (Academically Ineligible)
        if (!userDAO.existsByUsername(DEMO_STUDENT3_USERNAME) && !studentDAO.existsByStudentId(DEMO_STUDENT3_USN)) {
            authService.registerStudent(
                    DEMO_STUDENT3_USERNAME,
                    DEMO_PASSWORD,
                    DEMO_STUDENT3_EMAIL,
                    DEMO_STUDENT3_USN,
                    DEMO_STUDENT3_NAME,
                    DEMO_STUDENT3_PHONE,
                    DEMO_STUDENT3_BRANCH,
                    DEMO_STUDENT3_CGPA,
                    DEMO_STUDENT3_BACKLOGS,
                    DEMO_STUDENT3_GRAD_YEAR,
                    DEMO_STUDENT3_SKILLS
            );
            createdStudents++;
        }

        String message;
        if (existedPrior && createdStudents == 0 && createdOfficers == 0) {
            message = "Demo data already exists. Existing demonstration accounts and records will be reused.";
        } else {
            message = String.format("Demonstration data prepared successfully (Accounts provisioned: %d students, %d officer).",
                    createdStudents, createdOfficers);
        }

        return new DemoSetupResult(true, existedPrior, createdStudents, createdOfficers, createdJobs, message);
    }

    /**
     * Safely resets ONLY records positively identified as belonging to the demo dataset.
     * Never removes real user registrations, companies, or other jobs.
     */
    public DemoResetResult resetDemoDataOnly() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Delete applications belonging to demo students (respects ON DELETE RESTRICT on applications)
                int appsDeleted = 0;
                String deleteAppsSql = "DELETE FROM applications WHERE student_id IN (" +
                        "  SELECT id FROM students WHERE student_id IN (?, ?, ?)" +
                        ")";
                try (PreparedStatement stmt = conn.prepareStatement(deleteAppsSql)) {
                    stmt.setString(1, DEMO_STUDENT_USN);
                    stmt.setString(2, DEMO_STUDENT2_USN);
                    stmt.setString(3, DEMO_STUDENT3_USN);
                    appsDeleted = stmt.executeUpdate();
                }

                // 2. Delete demo users (ON DELETE CASCADE removes students and student_skills automatically)
                int usersDeleted = 0;
                String deleteUsersSql = "DELETE FROM users WHERE username IN (?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(deleteUsersSql)) {
                    stmt.setString(1, DEMO_STUDENT_USERNAME);
                    stmt.setString(2, DEMO_STUDENT2_USERNAME);
                    stmt.setString(3, DEMO_STUDENT3_USERNAME);
                    stmt.setString(4, DEMO_OFFICER_USERNAME);
                    usersDeleted = stmt.executeUpdate();
                }

                conn.commit();
                return new DemoResetResult(true, appsDeleted, usersDeleted,
                        "Demonstration data reset successfully. Real user accounts were preserved.");
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Result payload for Demo Setup operation.
     */
    public static class DemoSetupResult {
        private final boolean success;
        private final boolean alreadyExisted;
        private final int studentsCreated;
        private final int officersCreated;
        private final int jobsCreated;
        private final String message;

        public DemoSetupResult(boolean success, boolean alreadyExisted, int studentsCreated,
                               int officersCreated, int jobsCreated, String message) {
            this.success = success;
            this.alreadyExisted = alreadyExisted;
            this.studentsCreated = studentsCreated;
            this.officersCreated = officersCreated;
            this.jobsCreated = jobsCreated;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public boolean isAlreadyExisted() { return alreadyExisted; }
        public int getStudentsCreated() { return studentsCreated; }
        public int getOfficersCreated() { return officersCreated; }
        public int getJobsCreated() { return jobsCreated; }
        public String getMessage() { return message; }
    }

    /**
     * Result payload for Demo Reset operation.
     */
    public static class DemoResetResult {
        private final boolean success;
        private final int applicationsDeleted;
        private final int usersDeleted;
        private final String message;

        public DemoResetResult(boolean success, int applicationsDeleted, int usersDeleted, String message) {
            this.success = success;
            this.applicationsDeleted = applicationsDeleted;
            this.usersDeleted = usersDeleted;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public int getApplicationsDeleted() { return applicationsDeleted; }
        public int getUsersDeleted() { return usersDeleted; }
        public String getMessage() { return message; }
    }
}
