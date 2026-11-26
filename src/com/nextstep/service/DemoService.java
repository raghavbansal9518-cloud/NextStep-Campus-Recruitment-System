package com.nextstep.service;

import com.nextstep.dao.ApplicationDAO;
import com.nextstep.dao.CompanyDAO;
import com.nextstep.dao.JobDAO;
import com.nextstep.dao.StudentDAO;
import com.nextstep.dao.UserDAO;
import com.nextstep.database.DatabaseConnection;
import com.nextstep.model.Application;
import com.nextstep.model.ApplicationStatus;
import com.nextstep.model.Company;
import com.nextstep.model.Job;
import com.nextstep.model.Role;
import com.nextstep.model.Student;
import com.nextstep.model.User;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * DemoService
 *
 * Centralized, Editable Demonstration Data Management System and Scenario Setup
 * for the NextStep Campus Recruitment System.
 *
 * College CA-3 Principles:
 * - Pure Java SE & JDBC architecture.
 * - Centralized demo data management: Configurable, editable, and saved to MySQL.
 * - Idempotent initialization: Reuses existing demo accounts without duplication.
 * - Safe demo reset: Cleans only identified demo accounts; never touches real records.
 * - Deterministic, documented credentials using existing PBKDF2WithHmacSHA256 hashing.
 */
public class DemoService {

    // Documented, stable demonstration credentials
    public static final String DEMO_PASSWORD = "NextStep@2026";
    public static final String DEMO_OFFICER_ACTIVATION_CODE = "123456";

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
    private final ApplicationDAO applicationDAO;

    // Centralized, editable demonstration profiles
    private final List<DemoStudentProfile> studentProfiles = new ArrayList<>();
    private DemoOfficerProfile officerProfile;
    private DemoJobProfile jobProfile;

    public DemoService() {
        this(new AuthService(), new UserDAO(), new StudentDAO(), new CompanyDAO(), new JobDAO(), new ApplicationDAO());
    }

    public DemoService(AuthService authService, UserDAO userDAO, StudentDAO studentDAO,
                       CompanyDAO companyDAO, JobDAO jobDAO) {
        this(authService, userDAO, studentDAO, companyDAO, jobDAO, new ApplicationDAO());
    }

    public DemoService(AuthService authService, UserDAO userDAO, StudentDAO studentDAO,
                       CompanyDAO companyDAO, JobDAO jobDAO, ApplicationDAO applicationDAO) {
        this.authService = authService;
        this.userDAO = userDAO;
        this.studentDAO = studentDAO;
        this.companyDAO = companyDAO;
        this.jobDAO = jobDAO;
        this.applicationDAO = applicationDAO;
        initDefaultProfiles();
        syncFromDatabaseSafely();
    }

    /**
     * Initializes default baseline profiles for students, officer, and job drive.
     */
    public final void initDefaultProfiles() {
        studentProfiles.clear();

        // Profile 0: Scenario A — Fully Eligible
        studentProfiles.add(new DemoStudentProfile(
                "Scenario A — Fully Eligible Candidate",
                "ELIGIBLE on Job 1 (100% Skill Match). Ready to apply & receive instant digital receipt.",
                DEMO_STUDENT_USN,
                DEMO_STUDENT_NAME,
                DEMO_STUDENT_EMAIL,
                DEMO_STUDENT_PHONE,
                DEMO_STUDENT_BRANCH,
                DEMO_STUDENT_CGPA,
                DEMO_STUDENT_BACKLOGS,
                DEMO_STUDENT_GRAD_YEAR,
                DEMO_STUDENT_SKILLS,
                DEMO_STUDENT_USERNAME,
                DEMO_PASSWORD
        ));

        // Profile 1: Scenario B — Skill-Gap Candidate
        studentProfiles.add(new DemoStudentProfile(
                "Scenario B — Skill-Gap Candidate",
                "INELIGIBLE on Job 1 (Missing skills: OOP, Git). Demonstrates skill-gap matrix.",
                DEMO_STUDENT2_USN,
                DEMO_STUDENT2_NAME,
                DEMO_STUDENT2_EMAIL,
                DEMO_STUDENT2_PHONE,
                DEMO_STUDENT2_BRANCH,
                DEMO_STUDENT2_CGPA,
                DEMO_STUDENT2_BACKLOGS,
                DEMO_STUDENT2_GRAD_YEAR,
                DEMO_STUDENT2_SKILLS,
                DEMO_STUDENT2_USERNAME,
                DEMO_PASSWORD
        ));

        // Profile 2: Scenario C — Academically Ineligible Candidate
        studentProfiles.add(new DemoStudentProfile(
                "Scenario C — Academically Ineligible Candidate",
                "INELIGIBLE on Job 1 (CGPA < 7.50, 3 Backlogs > 0, Branch mismatch). Demonstrates academic gating.",
                DEMO_STUDENT3_USN,
                DEMO_STUDENT3_NAME,
                DEMO_STUDENT3_EMAIL,
                DEMO_STUDENT3_PHONE,
                DEMO_STUDENT3_BRANCH,
                DEMO_STUDENT3_CGPA,
                DEMO_STUDENT3_BACKLOGS,
                DEMO_STUDENT3_GRAD_YEAR,
                DEMO_STUDENT3_SKILLS,
                DEMO_STUDENT3_USERNAME,
                DEMO_PASSWORD
        ));

        // Placement Officer Profile
        officerProfile = new DemoOfficerProfile(
                DEMO_OFFICER_ID,
                DEMO_OFFICER_NAME,
                DEMO_OFFICER_EMAIL,
                DEMO_OFFICER_USERNAME,
                DEMO_PASSWORD,
                DEMO_OFFICER_ACTIVATION_CODE
        );

        // Job Drive Specifications Profile (corresponds to default demo job drive)
        jobProfile = new DemoJobProfile(
                0,
                1,
                "Nexis Cloud Technologies",
                "Associate Cloud Engineer",
                "Design, build, and maintain cloud infrastructure on AWS and GCP.",
                BigDecimal.valueOf(12.50),
                "Bengaluru, KA",
                7.20,
                0,
                Arrays.asList("Computer Science", "Information Technology"),
                Arrays.asList("Java", "SQL", "Linux", "Git", "Docker"),
                2026
        );
    }

    /**
     * Safely synchronizes in-memory profiles with live MySQL database records without throwing exceptions.
     */
    public void syncFromDatabaseSafely() {
        try {
            syncFromDatabase();
        } catch (Exception ignored) {
            // Keep default baseline values if DB is temporarily unavailable
        }
    }

    /**
     * Reads canonical records from MySQL database into in-memory demonstration profiles.
     */
    public void syncFromDatabase() throws SQLException {
        // Synchronize students
        for (DemoStudentProfile profile : studentProfiles) {
            Student s = studentDAO.findByStudentId(profile.getStudentId());
            if (s != null) {
                profile.setFullName(s.getFullName());
                profile.setPhone(s.getPhone());
                profile.setBranch(s.getBranch());
                profile.setCgpa(s.getCgpa());
                profile.setActiveBacklogs(s.getActiveBacklogs());
                profile.setGraduationYear(s.getGraduationYear());
                if (s.getSkills() != null && !s.getSkills().isEmpty()) {
                    profile.setSkills(s.getSkills());
                }
                User u = userDAO.findByUsername(profile.getUsername());
                if (u != null) {
                    profile.setEmail(u.getEmail());
                }
            }
        }

        // Synchronize officer
        User off = userDAO.findByUsername(officerProfile.getUsername());
        if (off != null) {
            officerProfile.setEmail(off.getEmail());
        }

        // Synchronize job
        if (jobProfile.getJobId() > 0) {
            Job j = jobDAO.findById(jobProfile.getJobId());
            if (j != null) {
                jobProfile.setCompanyId(j.getCompanyId());
                jobProfile.setCompanyName(j.getCompanyName());
                jobProfile.setTitle(j.getTitle());
                jobProfile.setDescription(j.getDescription());
                jobProfile.setCtcLpa(j.getCtcLpa());
                jobProfile.setLocation(j.getLocation());
                jobProfile.setMinCgpa(j.getMinCgpa());
                jobProfile.setMaxBacklogs(j.getMaxBacklogs());
                jobProfile.setGraduationYear(j.getGraduationYear());
                if (j.getEligibleBranches() != null && !j.getEligibleBranches().isEmpty()) {
                    jobProfile.setEligibleBranches(j.getEligibleBranches());
                }
                if (j.getRequiredSkills() != null && !j.getRequiredSkills().isEmpty()) {
                    jobProfile.setRequiredSkills(j.getRequiredSkills());
                }
            }
        }
    }

    /**
     * Saves or updates a demo student profile in MySQL database.
     */
    public boolean saveStudentProfile(DemoStudentProfile profile) throws SQLException {
        if (profile == null) {
            return false;
        }

        Student existing = studentDAO.findByStudentId(profile.getStudentId());
        if (existing != null) {
            existing.setFullName(profile.getFullName());
            existing.setPhone(profile.getPhone());
            existing.setBranch(profile.getBranch());
            existing.setCgpa(profile.getCgpa());
            existing.setActiveBacklogs(profile.getActiveBacklogs());
            existing.setGraduationYear(profile.getGraduationYear());
            existing.setSkills(profile.getSkills());

            studentDAO.updateStudent(existing);
            studentDAO.updateStudentSkills(existing.getId(), profile.getSkills());

            User user = userDAO.findByUsername(profile.getUsername());
            if (user != null) {
                user.setEmail(profile.getEmail());
                if (!AuthService.verifyPassword(profile.getPassword(), user.getPasswordHash())) {
                    user.setPasswordHash(AuthService.hashPassword(profile.getPassword()));
                }
                userDAO.updateUser(user);
            }
            return true;
        } else {
            authService.registerStudent(
                    profile.getUsername(),
                    profile.getPassword(),
                    profile.getEmail(),
                    profile.getStudentId(),
                    profile.getFullName(),
                    profile.getPhone(),
                    profile.getBranch(),
                    profile.getCgpa(),
                    profile.getActiveBacklogs(),
                    profile.getGraduationYear(),
                    profile.getSkills()
            );
            return true;
        }
    }

    /**
     * Saves or updates the demo placement officer profile in MySQL database.
     */
    public boolean saveOfficerProfile(DemoOfficerProfile profile) throws SQLException {
        if (profile == null) {
            return false;
        }

        User existing = userDAO.findByUsername(profile.getUsername());
        if (existing != null) {
            existing.setEmail(profile.getEmail());
            if (!AuthService.verifyPassword(profile.getPassword(), existing.getPasswordHash())) {
                existing.setPasswordHash(AuthService.hashPassword(profile.getPassword()));
            }
            userDAO.updateUser(existing);
            return true;
        } else {
            authService.registerPlacementOfficer(
                    profile.getOfficerId(),
                    profile.getFullName(),
                    profile.getEmail(),
                    profile.getUsername(),
                    profile.getPassword(),
                    profile.getActivationCode()
            );
            return true;
        }
    }

    /**
     * Saves or updates the demo job drive specifications in MySQL database.
     */
    public boolean saveJobProfile(DemoJobProfile profile) throws SQLException {
        if (profile == null) {
            return false;
        }

        Job existing = jobDAO.findById(profile.getJobId());
        if (existing != null) {
            existing.setCompanyId(profile.getCompanyId());
            existing.setTitle(profile.getTitle());
            existing.setDescription(profile.getDescription());
            existing.setCtcLpa(profile.getCtcLpa());
            existing.setLocation(profile.getLocation());
            existing.setMinCgpa(profile.getMinCgpa());
            existing.setMaxBacklogs(profile.getMaxBacklogs());
            existing.setGraduationYear(profile.getGraduationYear());
            return jobDAO.updateJob(existing, profile.getEligibleBranches(), profile.getRequiredSkills());
        } else {
            Job newJob = new Job();
            newJob.setCompanyId(profile.getCompanyId());
            newJob.setTitle(profile.getTitle());
            newJob.setDescription(profile.getDescription());
            newJob.setCtcLpa(profile.getCtcLpa());
            newJob.setLocation(profile.getLocation());
            newJob.setMinCgpa(profile.getMinCgpa());
            newJob.setMaxBacklogs(profile.getMaxBacklogs());
            newJob.setGraduationYear(profile.getGraduationYear());
            newJob.setStatus("OPEN");
            Job created = jobDAO.createJob(newJob, profile.getEligibleBranches(), profile.getRequiredSkills());
            profile.setJobId(created.getId());
            return true;
        }
    }

    /**
     * Saves or updates a partner company in MySQL database.
     */
    public boolean saveCompanyProfile(Company company) throws SQLException {
        if (company == null) {
            return false;
        }
        return companyDAO.updateCompany(company);
    }

    /**
     * Retrieves all applications submitted by demo candidate accounts for review and status tracking.
     */
    public List<DemoApplicationView> getDemoApplications() throws SQLException {
        List<DemoApplicationView> list = new ArrayList<>();
        String sql = "SELECT a.id, a.student_id, s.student_id AS usn, s.full_name AS student_name, " +
                "a.job_id, j.title AS job_title, c.name AS company_name, a.status, a.applied_at " +
                "FROM applications a " +
                "JOIN students s ON a.student_id = s.id " +
                "JOIN jobs j ON a.job_id = j.id " +
                "JOIN companies c ON j.company_id = c.id " +
                "WHERE s.student_id IN (?, ?, ?) " +
                "ORDER BY a.applied_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, studentProfiles.get(0).getStudentId());
            stmt.setString(2, studentProfiles.get(1).getStudentId());
            stmt.setString(3, studentProfiles.get(2).getStudentId());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    int studentId = rs.getInt("student_id");
                    String usn = rs.getString("usn");
                    String sName = rs.getString("student_name");
                    int jobId = rs.getInt("job_id");
                    String jobTitle = rs.getString("job_title");
                    String compName = rs.getString("company_name");
                    ApplicationStatus status = ApplicationStatus.valueOf(rs.getString("status"));
                    String appliedAt = rs.getString("applied_at");

                    list.add(new DemoApplicationView(id, studentId, usn, sName, jobId, jobTitle, compName, status, appliedAt));
                }
            }
        }
        return list;
    }

    /**
     * Updates an application status in MySQL database.
     */
    public boolean updateDemoApplicationStatus(int applicationId, ApplicationStatus status) throws SQLException {
        return applicationDAO.updateStatus(applicationId, status);
    }

    /**
     * Checks if a given username or student ID belongs to the configured demo student profiles.
     */
    public boolean isDemoStudent(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return false;
        }
        String trimmed = identifier.trim();
        for (DemoStudentProfile p : studentProfiles) {
            if (trimmed.equalsIgnoreCase(p.getUsername()) ||
                    trimmed.equalsIgnoreCase(p.getStudentId()) ||
                    trimmed.equalsIgnoreCase(p.getEmail())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a given username or officer ID belongs to the configured demo placement officer.
     */
    public boolean isDemoOfficer(String identifier) {
        if (identifier == null || identifier.trim().isEmpty() || officerProfile == null) {
            return false;
        }
        String trimmed = identifier.trim();
        return trimmed.equalsIgnoreCase(officerProfile.getUsername()) ||
                trimmed.equalsIgnoreCase(officerProfile.getOfficerId()) ||
                trimmed.equalsIgnoreCase(officerProfile.getEmail());
    }

    public List<DemoStudentProfile> getDemoStudentProfiles() {
        return Collections.unmodifiableList(studentProfiles);
    }

    public DemoStudentProfile getDemoStudentProfile(int index) {
        if (index >= 0 && index < studentProfiles.size()) {
            return studentProfiles.get(index);
        }
        return studentProfiles.get(0);
    }

    public DemoOfficerProfile getDemoOfficerProfile() {
        return officerProfile;
    }

    public DemoJobProfile getDemoJobProfile() {
        return jobProfile;
    }

    /**
     * Checks if the baseline demonstration accounts are present in the database.
     */
    public boolean isDemoDataPrepared() {
        try {
            boolean hasStudent = userDAO.existsByUsername(studentProfiles.get(0).getUsername()) &&
                    studentDAO.existsByStudentId(studentProfiles.get(0).getStudentId());
            boolean hasOfficer = userDAO.existsByUsername(officerProfile.getUsername());
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
        } catch (Exception ignored) {
            // Non-fatal if sample_data already executed
        }

        // 2. Provision Placement Officer if missing (or update password hash if format changed)
        User existingOfficer = userDAO.findByUsername(officerProfile.getUsername());
        if (existingOfficer == null) {
            authService.registerPlacementOfficer(
                    officerProfile.getOfficerId(),
                    officerProfile.getFullName(),
                    officerProfile.getEmail(),
                    officerProfile.getUsername(),
                    officerProfile.getPassword(),
                    officerProfile.getActivationCode()
            );
            createdOfficers++;
        } else if (!AuthService.verifyPassword(officerProfile.getPassword(), existingOfficer.getPasswordHash())) {
            userDAO.updatePassword(officerProfile.getUsername(), AuthService.hashPassword(officerProfile.getPassword()));
        }

        // 3. Provision Student Profiles (Scenario A, B, C)
        for (DemoStudentProfile profile : studentProfiles) {
            User existingStudentUser = userDAO.findByUsername(profile.getUsername());
            if (existingStudentUser == null) {
                if (studentDAO.existsByStudentId(profile.getStudentId())) {
                    try (Connection conn = DatabaseConnection.getConnection();
                         PreparedStatement stmt = conn.prepareStatement("DELETE FROM students WHERE student_id = ?")) {
                        stmt.setString(1, profile.getStudentId());
                        stmt.executeUpdate();
                    }
                }
                authService.registerStudent(
                        profile.getUsername(),
                        profile.getPassword(),
                        profile.getEmail(),
                        profile.getStudentId(),
                        profile.getFullName(),
                        profile.getPhone(),
                        profile.getBranch(),
                        profile.getCgpa(),
                        profile.getActiveBacklogs(),
                        profile.getGraduationYear(),
                        profile.getSkills()
                );
                createdStudents++;
            } else {
                if (!AuthService.verifyPassword(profile.getPassword(), existingStudentUser.getPasswordHash())) {
                    userDAO.updatePassword(profile.getUsername(), AuthService.hashPassword(profile.getPassword()));
                }
                Student s = studentDAO.findByStudentId(profile.getStudentId());
                if (s != null && (s.getSkills() == null || s.getSkills().size() < profile.getSkills().size())) {
                    try (Connection conn = DatabaseConnection.getConnection()) {
                        studentDAO.saveStudentSkills(conn, s.getId(), profile.getSkills());
                    }
                }
            }
        }

        // Synchronize in-memory profiles with live database state
        syncFromDatabaseSafely();

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
                // Collect USNs to clean up
                List<String> targetUsns = Arrays.asList(
                        DEMO_STUDENT_USN, DEMO_STUDENT2_USN, DEMO_STUDENT3_USN,
                        studentProfiles.get(0).getStudentId(),
                        studentProfiles.get(1).getStudentId(),
                        studentProfiles.get(2).getStudentId()
                );

                // Collect Usernames to clean up
                List<String> targetUsernames = Arrays.asList(
                        DEMO_STUDENT_USERNAME, DEMO_STUDENT2_USERNAME, DEMO_STUDENT3_USERNAME,
                        DEMO_OFFICER_USERNAME,
                        studentProfiles.get(0).getUsername(),
                        studentProfiles.get(1).getUsername(),
                        studentProfiles.get(2).getUsername(),
                        officerProfile.getUsername()
                );

                // 1. Delete applications belonging to demo students
                int appsDeleted = 0;
                StringBuilder inClause = new StringBuilder();
                for (int i = 0; i < targetUsns.size(); i++) {
                    inClause.append(i == 0 ? "?" : ", ?");
                }
                String deleteAppsSql = "DELETE FROM applications WHERE student_id IN (" +
                        "  SELECT id FROM students WHERE student_id IN (" + inClause + ")" +
                        ")";
                try (PreparedStatement stmt = conn.prepareStatement(deleteAppsSql)) {
                    for (int i = 0; i < targetUsns.size(); i++) {
                        stmt.setString(i + 1, targetUsns.get(i));
                    }
                    appsDeleted = stmt.executeUpdate();
                }

                // 2. Delete student skills for demo students
                String deleteSkillsSql = "DELETE FROM student_skills WHERE student_id IN (" +
                        "  SELECT id FROM students WHERE student_id IN (" + inClause + ")" +
                        ")";
                try (PreparedStatement stmt = conn.prepareStatement(deleteSkillsSql)) {
                    for (int i = 0; i < targetUsns.size(); i++) {
                        stmt.setString(i + 1, targetUsns.get(i));
                    }
                    stmt.executeUpdate();
                }

                // 3. Delete demo students by USN
                String deleteStudentsSql = "DELETE FROM students WHERE student_id IN (" + inClause + ")";
                try (PreparedStatement stmt = conn.prepareStatement(deleteStudentsSql)) {
                    for (int i = 0; i < targetUsns.size(); i++) {
                        stmt.setString(i + 1, targetUsns.get(i));
                    }
                    stmt.executeUpdate();
                }

                // 4. Delete demo users
                StringBuilder uInClause = new StringBuilder();
                for (int i = 0; i < targetUsernames.size(); i++) {
                    uInClause.append(i == 0 ? "?" : ", ?");
                }
                int usersDeleted = 0;
                String deleteUsersSql = "DELETE FROM users WHERE username IN (" + uInClause + ")";
                try (PreparedStatement stmt = conn.prepareStatement(deleteUsersSql)) {
                    for (int i = 0; i < targetUsernames.size(); i++) {
                        stmt.setString(i + 1, targetUsernames.get(i));
                    }
                    usersDeleted = stmt.executeUpdate();
                }

                conn.commit();

                // Reset profiles back to clean baseline
                initDefaultProfiles();

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

    // =========================================================================
    // INNER CLASSES & DATA HOLDERS
    // =========================================================================

    /**
     * Managed Demonstration Student Profile entity.
     */
    public static class DemoStudentProfile {
        private String scenarioName;
        private String expectedOutcome;
        private String studentId;
        private String fullName;
        private String email;
        private String phone;
        private String branch;
        private double cgpa;
        private int activeBacklogs;
        private int graduationYear;
        private List<String> skills;
        private String username;
        private String password;

        public DemoStudentProfile(String scenarioName, String expectedOutcome, String studentId,
                                  String fullName, String email, String phone, String branch,
                                  double cgpa, int activeBacklogs, int graduationYear,
                                  List<String> skills, String username, String password) {
            this.scenarioName = scenarioName;
            this.expectedOutcome = expectedOutcome;
            this.studentId = studentId;
            this.fullName = fullName;
            this.email = email;
            this.phone = phone;
            this.branch = branch;
            this.cgpa = cgpa;
            this.activeBacklogs = activeBacklogs;
            this.graduationYear = graduationYear;
            this.skills = new ArrayList<>(skills);
            this.username = username;
            this.password = password;
        }

        public String[] toFormValues() {
            return new String[] {
                    studentId,
                    fullName,
                    email,
                    phone,
                    branch,
                    String.format(Locale.US, "%.2f", cgpa),
                    String.join(", ", skills),
                    String.valueOf(activeBacklogs),
                    String.valueOf(graduationYear),
                    username,
                    password,
                    password
            };
        }

        public String getScenarioName() { return scenarioName; }
        public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }
        public String getExpectedOutcome() { return expectedOutcome; }
        public void setExpectedOutcome(String expectedOutcome) { this.expectedOutcome = expectedOutcome; }
        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getBranch() { return branch; }
        public void setBranch(String branch) { this.branch = branch; }
        public double getCgpa() { return cgpa; }
        public void setCgpa(double cgpa) { this.cgpa = cgpa; }
        public int getActiveBacklogs() { return activeBacklogs; }
        public void setActiveBacklogs(int activeBacklogs) { this.activeBacklogs = activeBacklogs; }
        public int getGraduationYear() { return graduationYear; }
        public void setGraduationYear(int graduationYear) { this.graduationYear = graduationYear; }
        public List<String> getSkills() { return skills; }
        public void setSkills(List<String> skills) { this.skills = new ArrayList<>(skills); }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    /**
     * Managed Demonstration Placement Officer Profile entity.
     */
    public static class DemoOfficerProfile {
        private String officerId;
        private String fullName;
        private String email;
        private String username;
        private String password;
        private String activationCode;

        public DemoOfficerProfile(String officerId, String fullName, String email,
                                  String username, String password, String activationCode) {
            this.officerId = officerId;
            this.fullName = fullName;
            this.email = email;
            this.username = username;
            this.password = password;
            this.activationCode = activationCode;
        }

        public String[] toFormValues() {
            return new String[] {
                    officerId,
                    fullName,
                    email,
                    username,
                    password,
                    password,
                    AuthService.OFFICER_ACTIVATION_CODE
            };
        }

        public String getOfficerId() { return officerId; }
        public void setOfficerId(String officerId) { this.officerId = officerId; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getActivationCode() { return activationCode; }
        public void setActivationCode(String activationCode) { this.activationCode = activationCode; }
    }

    /**
     * Managed Demonstration Job Drive Specifications entity.
     */
    public static class DemoJobProfile {
        private int jobId;
        private int companyId;
        private String companyName;
        private String title;
        private String description;
        private BigDecimal ctcLpa;
        private String location;
        private double minCgpa;
        private int maxBacklogs;
        private List<String> eligibleBranches;
        private List<String> requiredSkills;
        private int graduationYear;

        public DemoJobProfile(int jobId, int companyId, String companyName, String title,
                              String description, BigDecimal ctcLpa, String location,
                              double minCgpa, int maxBacklogs, List<String> eligibleBranches,
                              List<String> requiredSkills, int graduationYear) {
            this.jobId = jobId;
            this.companyId = companyId;
            this.companyName = companyName;
            this.title = title;
            this.description = description;
            this.ctcLpa = ctcLpa;
            this.location = location;
            this.minCgpa = minCgpa;
            this.maxBacklogs = maxBacklogs;
            this.eligibleBranches = new ArrayList<>(eligibleBranches);
            this.requiredSkills = new ArrayList<>(requiredSkills);
            this.graduationYear = graduationYear;
        }

        public String[] toFormValues() {
            return new String[] {
                    String.valueOf(companyId),
                    title,
                    description,
                    ctcLpa != null ? String.format(Locale.US, "%.2f", ctcLpa) : "12.00",
                    location != null ? location : "Bengaluru, KA",
                    String.format(Locale.US, "%.2f", minCgpa),
                    String.valueOf(maxBacklogs),
                    String.join(", ", eligibleBranches),
                    String.join(", ", requiredSkills),
                    String.valueOf(graduationYear)
            };
        }

        public int getJobId() { return jobId; }
        public void setJobId(int jobId) { this.jobId = jobId; }
        public int getCompanyId() { return companyId; }
        public void setCompanyId(int companyId) { this.companyId = companyId; }
        public String getCompanyName() { return companyName; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public BigDecimal getCtcLpa() { return ctcLpa; }
        public void setCtcLpa(BigDecimal ctcLpa) { this.ctcLpa = ctcLpa; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public double getMinCgpa() { return minCgpa; }
        public void setMinCgpa(double minCgpa) { this.minCgpa = minCgpa; }
        public int getMaxBacklogs() { return maxBacklogs; }
        public void setMaxBacklogs(int maxBacklogs) { this.maxBacklogs = maxBacklogs; }
        public List<String> getEligibleBranches() { return eligibleBranches; }
        public void setEligibleBranches(List<String> eligibleBranches) { this.eligibleBranches = new ArrayList<>(eligibleBranches); }
        public List<String> getRequiredSkills() { return requiredSkills; }
        public void setRequiredSkills(List<String> requiredSkills) { this.requiredSkills = new ArrayList<>(requiredSkills); }
        public int getGraduationYear() { return graduationYear; }
        public void setGraduationYear(int graduationYear) { this.graduationYear = graduationYear; }
    }

    /**
     * Read-friendly view entity for demo candidate applications.
     */
    public static class DemoApplicationView {
        private final int id;
        private final int studentId;
        private final String studentUsn;
        private final String studentName;
        private final int jobId;
        private final String jobTitle;
        private final String companyName;
        private final ApplicationStatus status;
        private final String appliedAt;

        public DemoApplicationView(int id, int studentId, String studentUsn, String studentName,
                                   int jobId, String jobTitle, String companyName,
                                   ApplicationStatus status, String appliedAt) {
            this.id = id;
            this.studentId = studentId;
            this.studentUsn = studentUsn;
            this.studentName = studentName;
            this.jobId = jobId;
            this.jobTitle = jobTitle;
            this.companyName = companyName;
            this.status = status;
            this.appliedAt = appliedAt;
        }

        public int getId() { return id; }
        public int getStudentId() { return studentId; }
        public String getStudentUsn() { return studentUsn; }
        public String getStudentName() { return studentName; }
        public int getJobId() { return jobId; }
        public String getJobTitle() { return jobTitle; }
        public String getCompanyName() { return companyName; }
        public ApplicationStatus getStatus() { return status; }
        public String getAppliedAt() { return appliedAt; }
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
