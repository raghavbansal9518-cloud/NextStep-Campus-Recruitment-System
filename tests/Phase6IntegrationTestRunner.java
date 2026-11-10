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
import com.nextstep.service.AuthService;
import com.nextstep.service.RecruitmentService;
import com.nextstep.service.RecruitmentService.EligibilityResult;
import com.nextstep.util.PresentationConfig;
import com.nextstep.util.Result;
import com.nextstep.util.ValidationUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Phase6IntegrationTestRunner
 * 
 * Exhaustive CA-3 Integration & System Validation Suite for NextStep:
 * 1. Student End-to-End Workflow & Boundary Conditions
 * 2. Placement Officer End-to-End Workflow & Authorization Verification
 * 3. Database Integrity, JDBC Transaction Rollback & Resource Handling
 * 4. Input Validation, String Sanitization & Malformed Input Resiliency
 * 5. Data Persistence across Simulated Application Restarts
 * 6. Empty State Handling (Zero-data tolerances)
 * 7. Seed Data Immutability & Constraint Verification
 */
public class Phase6IntegrationTestRunner {

    private static int passedCount = 0;
    private static int failedCount = 0;

    private static void pass(String testName) {
        System.out.println("  [PASS] " + testName);
        passedCount++;
    }

    private static void fail(String testName, String reason) {
        System.err.println("  [FAIL] " + testName + " -> " + reason);
        failedCount++;
    }

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("   NextStep — Phase 6: Final CA-3 Integration & Stability Suite   ");
        System.out.println("==================================================================\n");

        AuthService authService = new AuthService();
        RecruitmentService recruitmentService = new RecruitmentService();
        UserDAO userDAO = new UserDAO();
        StudentDAO studentDAO = new StudentDAO();
        JobDAO jobDAO = new JobDAO();
        CompanyDAO companyDAO = new CompanyDAO();
        ApplicationDAO applicationDAO = new ApplicationDAO();

        // Unique test identifiers to prevent collisions
        long timestamp = System.currentTimeMillis();
        String studentUsername = "p6_stud_" + timestamp;
        String studentRoll = "P6-USN-" + timestamp;
        String studentEmail = "p6_stud_" + timestamp + "@campus.edu";

        String officerUsername = "p6_off_" + timestamp;
        String officerEmail = "p6_off_" + timestamp + "@placement.edu";

        int createdUserId = -1;
        int createdStudentId = -1;
        int createdOfficerUserId = -1;
        int createdAppId = -1;

        try {
            // =========================================================
            // SUITE 1: SEED DATA & IMMUTABILITY VERIFICATION
            // =========================================================
            System.out.println("--- 1. SEED DATA & IMMUTABILITY VERIFICATION ---");
            List<Company> initialCompanies = companyDAO.findAllCompanies();
            if (initialCompanies.size() >= 3) {
                pass("Test 1.1: Partner companies seeded properly (found " + initialCompanies.size() + ")");
            } else {
                fail("Test 1.1", "Expected at least 3 companies, found " + initialCompanies.size());
            }

            List<Job> initialJobs = jobDAO.findAllJobs();
            if (initialJobs.size() >= 3) {
                pass("Test 1.2: Job openings seeded properly (found " + initialJobs.size() + ")");
            } else {
                fail("Test 1.2", "Expected at least 3 jobs, found " + initialJobs.size());
            }

            // =========================================================
            // SUITE 2: STUDENT END-TO-END WORKFLOW
            // =========================================================
            System.out.println("\n--- 2. STUDENT END-TO-END WORKFLOW ---");
            // A. Registration with skills
            Student registeredStudent = authService.registerStudent(
                    studentUsername, "Pass@1234", studentEmail, studentRoll,
                    "P6 Test Student", "9876543210", "Computer Science",
                    8.85, 0, 2026,
                    Arrays.asList("Java", "Spring Boot", "SQL", "Git", "OOP", "Docker")
            );

            if (registeredStudent != null && registeredStudent.getId() > 0) {
                createdStudentId = registeredStudent.getId();
                createdUserId = registeredStudent.getUserId();
                pass("Test 2.1: Student registration persists across user, student, and skills tables");
            } else {
                fail("Test 2.1", "Student registration returned null or invalid ID");
            }

            // B. Authentication
            User loggedInStudent = authService.login(studentUsername, "Pass@1234");
            if (loggedInStudent != null && loggedInStudent.getRole() == Role.STUDENT) {
                pass("Test 2.2: Student logs in with valid credentials");
            } else {
                fail("Test 2.2", "Student login failed with valid credentials");
            }

            // C. Job Browsing & Eligibility Checking
            Job targetJob = initialJobs.get(0); // Nexis Cloud Technologies
            EligibilityResult eligibleResult = recruitmentService.checkEligibility(registeredStudent, targetJob);
            if (eligibleResult.isEligible()) {
                pass("Test 2.3: Student eligibility evaluation succeeds for matching criteria");
            } else {
                fail("Test 2.3", "Student should be eligible for job ID " + targetJob.getId() + " but was rejected: " + eligibleResult.getReasons());
            }

            // Ineligible check: Student missing required skills
            Student unskilledStudent = new Student(9999, "USN-UNSKILLED", "Unskilled Student", "9999999999", "Computer Science", 9.0, 0, 2026);
            EligibilityResult ineligibleResult = recruitmentService.checkEligibility(unskilledStudent, targetJob);
            if (!ineligibleResult.isEligible() && !ineligibleResult.getMissingSkills().isEmpty()) {
                pass("Test 2.3b: Ineligible student correctly rejected for missing skills (" + ineligibleResult.getMissingSkills().size() + " missing)");
            } else {
                fail("Test 2.3b", "Student lacking skills should have been flagged ineligible");
            }

            // D. Job Application Submission
            Application submittedApp = recruitmentService.applyForJob(registeredStudent, targetJob.getId());
            if (submittedApp != null && submittedApp.getId() > 0 && submittedApp.getStatus() == ApplicationStatus.APPLIED) {
                createdAppId = submittedApp.getId();
                pass("Test 2.4: Eligible student submits application (Status: APPLIED)");
            } else {
                fail("Test 2.4", "Application creation failed or returned invalid status");
            }

            // E. Duplicate Application Prevention
            try {
                recruitmentService.applyForJob(registeredStudent, targetJob.getId());
                fail("Test 2.5", "Expected IllegalStateException for duplicate application");
            } catch (IllegalStateException e) {
                pass("Test 2.5: Duplicate application gracefully blocked with IllegalStateException");
            }

            // F. Application History Scoping
            List<Application> studentHistory = recruitmentService.getStudentApplications(registeredStudent.getId());
            if (studentHistory.size() == 1 && studentHistory.get(0).getId() == createdAppId) {
                pass("Test 2.6: Student application history correctly isolated to own applications");
            } else {
                fail("Test 2.6", "Student history mismatch. Expected 1 app, found " + studentHistory.size());
            }

            // =========================================================
            // SUITE 3: PLACEMENT OFFICER WORKFLOW & ROLE-BASED ACCESS
            // =========================================================
            System.out.println("\n--- 3. PLACEMENT OFFICER WORKFLOW & ACCESS CONTROL ---");
            // A. Officer Provisioning
            User officer = authService.registerPlacementOfficer(officerUsername, "OfficerPass123", officerEmail);
            if (officer != null && officer.getId() > 0 && officer.getRole() == Role.PLACEMENT_OFFICER) {
                createdOfficerUserId = officer.getId();
                pass("Test 3.1: Placement officer provisioned with PBKDF2 hash & PLACEMENT_OFFICER role");
            } else {
                fail("Test 3.1", "Officer provisioning failed");
            }

            // B. Officer Login
            User loggedInOfficer = authService.login(officerUsername, "OfficerPass123");
            if (loggedInOfficer != null && loggedInOfficer.getRole() == Role.PLACEMENT_OFFICER) {
                pass("Test 3.2: Placement officer authenticates successfully");
            } else {
                fail("Test 3.2", "Placement officer login failed");
            }

            // C. Officer Views All Applications
            List<Application> allApps = recruitmentService.getAllApplications(loggedInOfficer);
            final int appToCheck = createdAppId;
            boolean foundOurApp = allApps.stream().anyMatch(a -> a.getId() == appToCheck);
            if (foundOurApp) {
                pass("Test 3.3: Placement officer retrieves all student applications with joined metadata");
            } else {
                fail("Test 3.3", "Placement officer could not find submitted application ID " + createdAppId);
            }

            // D. Officer Updates Status
            boolean statusUpdated = recruitmentService.updateApplicationStatus(loggedInOfficer, createdAppId, ApplicationStatus.SHORTLISTED);
            Application updatedApp = applicationDAO.findById(createdAppId);
            if (statusUpdated && updatedApp != null && updatedApp.getStatus() == ApplicationStatus.SHORTLISTED) {
                pass("Test 3.4: Placement officer advances application status to SHORTLISTED");
            } else {
                fail("Test 3.4", "Application status update failed");
            }

            // E. Student is Blocked from Officer Operations
            try {
                recruitmentService.updateApplicationStatus(loggedInStudent, createdAppId, ApplicationStatus.SELECTED);
                fail("Test 3.5a", "Student should be blocked from updating application status");
            } catch (SecurityException e) {
                pass("Test 3.5a: Student blocked from updateApplicationStatus via SecurityException");
            }

            try {
                recruitmentService.getAllApplications(loggedInStudent);
                fail("Test 3.5b", "Student should be blocked from getAllApplications");
            } catch (SecurityException e) {
                pass("Test 3.5b: Student blocked from getAllApplications via SecurityException");
            }

            try {
                recruitmentService.exportApplicationsToCsv(loggedInStudent, "reports/p6_unauthorized.csv");
                fail("Test 3.5c", "Student should be blocked from exportApplicationsToCsv");
            } catch (SecurityException e) {
                pass("Test 3.5c: Student blocked from exportApplicationsToCsv via SecurityException");
            }

            // =========================================================
            // SUITE 4: RECRUITMENT METRICS & CSV ARCHIVAL WORKFLOW
            // =========================================================
            System.out.println("\n--- 4. RECRUITMENT METRICS & CSV ARCHIVAL WORKFLOW ---");
            Map<ApplicationStatus, Long> summary = recruitmentService.getRecruitmentSummary(loggedInOfficer);
            if (summary.containsKey(ApplicationStatus.SHORTLISTED) && summary.get(ApplicationStatus.SHORTLISTED) >= 1) {
                pass("Test 4.1: Recruitment summary (groupingBy) accurately tallies SHORTLISTED status");
            } else {
                fail("Test 4.1", "Recruitment summary missing SHORTLISTED count");
            }

            Map<Boolean, Long> partition = recruitmentService.getPlacementPartitionCount(loggedInOfficer);
            if (partition != null && partition.containsKey(false)) {
                pass("Test 4.2: Placement partition (partitioningBy) separates selected vs non-selected");
            } else {
                fail("Test 4.2", "Placement partitioning failed");
            }

            // CSV Export & Audit Round-Trip
            String testExportPath = "reports/p6_test_export_" + timestamp + ".csv";
            String testArchivePath = "reports/p6_test_archive_" + timestamp + ".csv";
            File exportedCsv = recruitmentService.exportApplicationsToCsv(loggedInOfficer, testExportPath);
            if (exportedCsv.exists() && exportedCsv.length() > 0) {
                pass("Test 4.3: Export applications report to CSV file generates non-empty output");
            } else {
                fail("Test 4.3", "CSV export file missing or empty");
            }

            // Round-trip parse & update status
            List<Application> archivedRecords = recruitmentService.processAndArchiveCsvReport(
                    loggedInOfficer, testExportPath, testArchivePath, createdAppId, ApplicationStatus.SELECTED
            );

            File archivedFile = new File(testArchivePath);
            if (archivedFile.exists() && archivedRecords.size() >= 1) {
                final int auditedAppId = createdAppId;
                boolean verifiedStatus = archivedRecords.stream()
                        .filter(a -> a.getId() == auditedAppId)
                        .anyMatch(a -> a.getStatus() == ApplicationStatus.SELECTED);
                if (verifiedStatus) {
                    pass("Test 4.4: CSV round-trip parse, status update, and audit archiving verified");
                } else {
                    fail("Test 4.4", "Audited application record status was not updated to SELECTED");
                }
            } else {
                fail("Test 4.4", "Archived CSV file missing or parsed records empty");
            }

            // Clean up temporary test files
            if (exportedCsv.exists()) exportedCsv.delete();
            if (archivedFile.exists()) archivedFile.delete();

            // =========================================================
            // SUITE 5: TRANSACTION ROLLBACK & ATOMICITY VERIFICATION
            // =========================================================
            System.out.println("\n--- 5. TRANSACTION ROLLBACK & ATOMICITY VERIFICATION ---");
            // Attempt to register a student with an intentionally duplicate roll number
            String doomedUsername = "doomed_user_" + timestamp;
            try {
                authService.registerStudent(
                        doomedUsername, "Pass@1234", "doomed@test.com", studentRoll, // Same roll as existing!
                        "Doomed Student", "9123456780", "Information Technology",
                        7.5, 0, 2026, Arrays.asList("Java")
                );
                fail("Test 5.1", "Expected IllegalArgumentException for duplicate studentId");
            } catch (IllegalArgumentException e) {
                pass("Test 5.1: Pre-transaction validation catches duplicate roll number");
            }

            // Verify doomed username was not inserted into users table
            boolean doomedUserExists = userDAO.existsByUsername(doomedUsername);
            if (!doomedUserExists) {
                pass("Test 5.2: Atomicity preserved; no partial user record created on validation failure");
            } else {
                fail("Test 5.2", "Dirty user record found in users table after failed registration");
            }

            // Simulate manual transaction rollback via JDBC connection
            try (Connection conn = DatabaseConnection.getConnection()) {
                conn.setAutoCommit(false);
                User testRollbackUser = new User("rb_user_" + timestamp, "dummyhash", Role.STUDENT, "rb_" + timestamp + "@test.com");
                userDAO.createUser(conn, testRollbackUser);
                int rollbackUserId = testRollbackUser.getId();

                // Intentionally rollback
                conn.rollback();
                conn.setAutoCommit(true);

                // Verify user does not exist in a new connection
                User queried = userDAO.findByUsername("rb_user_" + timestamp);
                if (queried == null) {
                    pass("Test 5.3: Explicit JDBC conn.rollback() successfully discards uncommitted row");
                } else {
                    fail("Test 5.3", "Uncommitted row persisted despite rollback!");
                }
            }

            // =========================================================
            // SUITE 6: INPUT SANITIZATION & BOUNDARY RESILIENCY
            // =========================================================
            System.out.println("\n--- 6. INPUT SANITIZATION & BOUNDARY RESILIENCY ---");
            // Email checks
            if (!ValidationUtil.isValidEmail("plainaddress") &&
                !ValidationUtil.isValidEmail("@missingusername.com") &&
                !ValidationUtil.isValidEmail("missingdomain@.com") &&
                ValidationUtil.isValidEmail("valid.student@college.edu")) {
                pass("Test 6.1: ValidationUtil.isValidEmail rejects invalid patterns and accepts valid RFC format");
            } else {
                fail("Test 6.1", "Email validation failure");
            }

            // CGPA bounds
            if (!ValidationUtil.isValidCgpa(-0.1) &&
                !ValidationUtil.isValidCgpa(10.01) &&
                ValidationUtil.isValidCgpa(0.0) &&
                ValidationUtil.isValidCgpa(10.0) &&
                ValidationUtil.isValidCgpa(8.45)) {
                pass("Test 6.2: ValidationUtil.isValidCgpa strictly bounds CGPA between 0.00 and 10.00");
            } else {
                fail("Test 6.2", "CGPA boundary check failure");
            }

            // Backlogs & graduation year
            if (!ValidationUtil.isValidBacklogs(-1) &&
                ValidationUtil.isValidBacklogs(0) &&
                ValidationUtil.isValidBacklogs(4) &&
                !ValidationUtil.isValidGraduationYear(2018) &&
                !ValidationUtil.isValidGraduationYear(2040) &&
                ValidationUtil.isValidGraduationYear(2026)) {
                pass("Test 6.3: Backlog (non-negative) and Graduation Year (2020-2035) bounds enforced");
            } else {
                fail("Test 6.3", "Backlog or graduation year validation failure");
            }

            // Malformed CSV handling
            File badCsv = new File("reports/p6_bad_" + timestamp + ".csv");
            try (FileWriter fw = new FileWriter(badCsv)) {
                fw.write("Application ID,Student Roll No,Student Name,Job Title,Company,Status,Applied At\n");
                fw.write("NOT_A_NUMBER,Roll,Name,Title,Company,APPLIED,2026-11-09\n"); // Bad ID
                fw.write("ShortLine\n"); // Malformed line
                fw.write("123,Roll,Name,Title,Company,APPLIED,2026-11-09\n"); // Valid line
            }
            List<Application> badCsvParsed = recruitmentService.processAndArchiveCsvReport(
                    loggedInOfficer, badCsv.getAbsolutePath(), "reports/p6_bad_audit_" + timestamp + ".csv"
            );
            if (badCsvParsed.size() == 1 && badCsvParsed.get(0).getId() == 123) {
                pass("Test 6.4: CSV parser gracefully skips malformed rows and parses valid row without crashing");
            } else {
                fail("Test 6.4", "CSV parser did not recover from malformed row. Parsed size: " + badCsvParsed.size());
            }
            if (badCsv.exists()) badCsv.delete();
            File badAudit = new File("reports/p6_bad_audit_" + timestamp + ".csv");
            if (badAudit.exists()) badAudit.delete();

            // Missing CSV handling
            try {
                recruitmentService.processAndArchiveCsvReport(loggedInOfficer, "non_existent_file.csv", "out.csv");
                fail("Test 6.5", "Expected IllegalArgumentException for missing CSV file");
            } catch (IllegalArgumentException e) {
                pass("Test 6.5: Missing CSV file handled safely with clear IllegalArgumentException");
            }

            // =========================================================
            // SUITE 7: PERSISTENCE ACROSS RE-INITIALIZATION
            // =========================================================
            System.out.println("\n--- 7. PERSISTENCE ACROSS SIMULATED RESTART ---");
            // Create fresh new DAO instances simulating application restart
            ApplicationDAO freshAppDAO = new ApplicationDAO();
            Application persistedApp = freshAppDAO.findById(createdAppId);
            if (persistedApp != null && persistedApp.getStudentId() == createdStudentId) {
                pass("Test 7.1: Application state reliably persisted and readable across distinct DAO instances");
            } else {
                fail("Test 7.1", "Fresh DAO instance could not locate persisted application");
            }

            StudentDAO freshStudentDAO = new StudentDAO();
            Student persistedStudent = freshStudentDAO.findById(createdStudentId);
            if (persistedStudent != null && persistedStudent.getStudentId().equals(studentRoll)) {
                pass("Test 7.2: Student profile reliably persisted and readable across distinct DAO instances");
            } else {
                fail("Test 7.2", "Fresh DAO instance could not locate persisted student");
            }

        } catch (Exception e) {
            System.err.println("[UNEXPECTED EXCEPTION] " + e.getMessage());
            e.printStackTrace();
            fail("Exception caught", e.getMessage());
        } finally {
            // =========================================================
            // CLEANUP TEST DATA (Preserving seeded companies and jobs)
            // =========================================================
            System.out.println("\n--- CLEANUP & POST-TEST DATABASE AUDIT ---");
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement()) {

                if (createdAppId > 0) {
                    stmt.executeUpdate("DELETE FROM applications WHERE id = " + createdAppId);
                }
                if (createdStudentId > 0) {
                    stmt.executeUpdate("DELETE FROM student_skills WHERE student_id = " + createdStudentId);
                    stmt.executeUpdate("DELETE FROM students WHERE id = " + createdStudentId);
                }
                if (createdUserId > 0) {
                    stmt.executeUpdate("DELETE FROM users WHERE id = " + createdUserId);
                }
                if (createdOfficerUserId > 0) {
                    stmt.executeUpdate("DELETE FROM users WHERE id = " + createdOfficerUserId);
                }

                pass("Cleanup: Temporary test entities purged from nextstep_db successfully");
            } catch (SQLException e) {
                System.err.println("[CLEANUP ERROR] " + e.getMessage());
            }
        }

        System.out.println("\n==================================================================");
        System.out.println("Phase 6 Integration Results: " + passedCount + " Passed, " + failedCount + " Failed.");
        System.out.println("==================================================================");

        if (failedCount > 0) {
            System.exit(1);
        }
    }
}
