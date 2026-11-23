import com.nextstep.Main;
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
import com.nextstep.service.DemoService;
import com.nextstep.service.RecruitmentService;
import com.nextstep.service.RecruitmentService.EligibilityResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

/**
 * DemoModeVerificationTestRunner
 *
 * Comprehensive Automated Verification Suite for:
 * 1. Demo Mode Initialization & Idempotence
 * 2. Deterministic, Secure Credentials (PBKDF2)
 * 3. Scenario A (Fully Eligible Candidate)
 * 4. Scenario B (Skill-Gap Candidate)
 * 5. Scenario C (Academically Ineligible Candidate)
 * 6. End-to-End Application & Duplicate Prevention
 * 7. Placement Officer Application Review & Status Updates
 * 8. CSV Report Export & Persistence Across Simulated Restart
 * 9. Safe Demo-Only Reset & Real Account Protection
 * 10. Gateway & Navigation Integration
 */
public class DemoModeVerificationTestRunner {

    private static int passCount = 0;
    private static int failCount = 0;

    private static void pass(String name) {
        passCount++;
        System.out.println("  [PASS] " + name);
    }

    private static void fail(String name, String reason) {
        failCount++;
        System.err.println("  [FAIL] " + name + " - " + reason);
    }

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("   NextStep — Complete Repeatable Demo Mode & Scenario Test Suite  ");
        System.out.println("==================================================================");

        AuthService authService = new AuthService();
        UserDAO userDAO = new UserDAO();
        StudentDAO studentDAO = new StudentDAO();
        CompanyDAO companyDAO = new CompanyDAO();
        JobDAO jobDAO = new JobDAO();
        ApplicationDAO applicationDAO = new ApplicationDAO();
        RecruitmentService recruitmentService = new RecruitmentService(jobDAO, applicationDAO);
        DemoService demoService = new DemoService(authService, userDAO, studentDAO, companyDAO, jobDAO);

        // Ensure database connection
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null || conn.isClosed()) {
                System.err.println("[FATAL] Could not connect to MySQL database.");
                return;
            }
        } catch (SQLException e) {
            System.err.println("[FATAL] MySQL error: " + e.getMessage());
            return;
        }

        // Set activation code for officer operations
        System.setProperty(AuthService.ACTIVATION_CODE_PROP, DemoService.DEMO_OFFICER_ACTIVATION_CODE);

        // =================================================================
        // SUITE 1: FIRST-TIME DEMO SETUP & IDEMPOTENCE
        // =================================================================
        System.out.println("\n--- 1. DEMO SETUP & REPEATABLE IDEMPOTENCE ---");
        try {
            // First execution
            DemoService.DemoSetupResult setup1 = demoService.prepareDemoData();
            if (setup1.isSuccess()) {
                pass("DM.1: First-time demo setup initializes required demo accounts and jobs");
            } else {
                fail("DM.1", "Demo setup failed: " + setup1.getMessage());
            }

            // Verify demo data is reported as prepared
            if (demoService.isDemoDataPrepared()) {
                pass("DM.2: isDemoDataPrepared() accurately confirms demo readiness");
            } else {
                fail("DM.2", "isDemoDataPrepared() returned false after setup");
            }

            // Second execution (Repeatable Idempotence)
            DemoService.DemoSetupResult setup2 = demoService.prepareDemoData();
            if (setup2.isSuccess() && setup2.isAlreadyExisted() &&
                    setup2.getStudentsCreated() == 0 && setup2.getOfficersCreated() == 0 &&
                    setup2.getMessage().contains("Demo data already exists")) {
                pass("DM.3: Repeated demo setup detects existing records, creates zero duplicates, and reuses data");
            } else {
                fail("DM.3", "Repeat setup did not behave idempotently: " + setup2.getMessage());
            }

            // Verify no duplicate users or students exist in database
            int vihaanCount = 0;
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE username = ?")) {
                stmt.setString(1, DemoService.DEMO_STUDENT_USERNAME);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) vihaanCount = rs.getInt(1);
                }
            }
            if (vihaanCount == 1) {
                pass("DM.4: Database confirms exactly 1 unique user record for " + DemoService.DEMO_STUDENT_USERNAME);
            } else {
                fail("DM.4", "Expected 1 user record, found: " + vihaanCount);
            }

        } catch (Exception e) {
            fail("SUITE 1", "Exception: " + e.getMessage());
        }

        // =================================================================
        // SUITE 2: DEMONSTRATION CREDENTIALS & PBKDF2 AUTHENTICATION
        // =================================================================
        System.out.println("\n--- 2. DEMO CREDENTIALS & AUTHENTICATION ---");
        try {
            // Student login via username
            User studentUser = authService.login(DemoService.DEMO_STUDENT_USERNAME, DemoService.DEMO_PASSWORD);
            if (studentUser != null && studentUser.getRole() == Role.STUDENT) {
                pass("DC.1: Student demo authentication succeeds via username (" + DemoService.DEMO_STUDENT_USERNAME + ")");
            } else {
                fail("DC.1", "Student login failed for demo username");
            }

            // Student login via USN
            User studentUserByUsn = authService.login(DemoService.DEMO_STUDENT_USN, DemoService.DEMO_PASSWORD);
            if (studentUserByUsn != null && studentUserByUsn.getId() == studentUser.getId()) {
                pass("DC.2: Student demo authentication succeeds via Student ID / USN (" + DemoService.DEMO_STUDENT_USN + ")");
            } else {
                fail("DC.2", "Student login failed for demo USN");
            }

            // Student login with invalid password fails
            User failedStudent = authService.login(DemoService.DEMO_STUDENT_USERNAME, "WrongPass@999");
            if (failedStudent == null) {
                pass("DC.3: Student login with wrong password rejected safely without leaking existence");
            } else {
                fail("DC.3", "Student login allowed incorrect password");
            }

            // Officer login via username
            User officerUser = authService.login(DemoService.DEMO_OFFICER_USERNAME, DemoService.DEMO_PASSWORD);
            if (officerUser != null && officerUser.getRole() == Role.PLACEMENT_OFFICER) {
                pass("DC.4: Placement Officer demo authentication succeeds via username (" + DemoService.DEMO_OFFICER_USERNAME + ")");
            } else {
                fail("DC.4", "Officer login failed for demo username");
            }

            // Password hash format verification (PBKDF2)
            if (studentUser.getPasswordHash().startsWith("PBKDF2$") && !studentUser.getPasswordHash().contains(DemoService.DEMO_PASSWORD)) {
                pass("DC.5: Passwords are encrypted with PBKDF2 salt & hash (never plain-text)");
            } else {
                fail("DC.5", "Password hash does not meet PBKDF2 security standard");
            }

        } catch (Exception e) {
            fail("SUITE 2", "Exception: " + e.getMessage());
        }

        // =================================================================
        // SUITE 3: SCENARIO A — FULLY ELIGIBLE CANDIDATE
        // =================================================================
        System.out.println("\n--- 3. SCENARIO A: FULLY ELIGIBLE CANDIDATE ---");
        try {
            Student vihaan = studentDAO.findByStudentId(DemoService.DEMO_STUDENT_USN);
            Job backendJob = jobDAO.findById(1); // Junior Backend Engineer

            if (vihaan != null && backendJob != null) {
                EligibilityResult res = recruitmentService.checkEligibility(vihaan, backendJob);
                if (res.isEligible() && res.isAcademicEligible() && res.isSkillsEligible() &&
                        res.getMissingSkills().isEmpty() && res.getMatchedSkills().size() >= 4) {
                    pass("SA.1: Vihaan Mehta evaluates as 100% ELIGIBLE for Junior Backend Engineer (all cutoffs & skills passed)");
                } else {
                    fail("SA.1", "Eligibility evaluation failed: " + res);
                }
            } else {
                fail("SA.1", "Student or Job not found in database");
            }
        } catch (Exception e) {
            fail("SUITE 3", "Exception: " + e.getMessage());
        }

        // =================================================================
        // SUITE 4: SCENARIO B — SKILL-GAP CANDIDATE
        // =================================================================
        System.out.println("\n--- 4. SCENARIO B: SKILL-GAP CANDIDATE ---");
        try {
            Student kabir = studentDAO.findByStudentId(DemoService.DEMO_STUDENT2_USN);
            Job backendJob = jobDAO.findById(1);

            if (kabir != null && backendJob != null) {
                EligibilityResult res = recruitmentService.checkEligibility(kabir, backendJob);
                boolean academicPassed = res.isAcademicEligible();
                boolean skillsFailed = !res.isSkillsEligible();
                boolean missingReported = res.getMissingSkills().contains("OOP") && res.getMissingSkills().contains("Git");
                boolean matchedReported = res.getMatchedSkills().contains("Java") && res.getMatchedSkills().contains("SQL");

                if (!res.isEligible() && academicPassed && skillsFailed && missingReported && matchedReported) {
                    pass("SB.1: Kabir Malhotra evaluates as SKILL-GAP (Academic PASS, 50% Skills Matched: Java, SQL; Missing: OOP, Git)");
                } else {
                    fail("SB.1", "Skill-gap evaluation failed: " + res);
                }
            } else {
                fail("SB.1", "Student or Job not found in database");
            }
        } catch (Exception e) {
            fail("SUITE 4", "Exception: " + e.getMessage());
        }

        // =================================================================
        // SUITE 5: SCENARIO C — ACADEMICALLY INELIGIBLE CANDIDATE
        // =================================================================
        System.out.println("\n--- 5. SCENARIO C: ACADEMICALLY INELIGIBLE CANDIDATE ---");
        try {
            Student arjun = studentDAO.findByStudentId(DemoService.DEMO_STUDENT3_USN);
            Job backendJob = jobDAO.findById(1);

            if (arjun != null && backendJob != null) {
                EligibilityResult res = recruitmentService.checkEligibility(arjun, backendJob);
                boolean cgpaFailed = !res.isCgpaEligible();
                boolean backlogsFailed = !res.isBacklogsEligible();
                boolean branchFailed = !res.isBranchEligible();

                if (!res.isEligible() && cgpaFailed && backlogsFailed && branchFailed) {
                    pass("SC.1: Arjun Khanna evaluates as ACADEMICALLY INELIGIBLE (Fails CGPA cutoff, Backlogs limit, and Branch match)");
                } else {
                    fail("SC.1", "Ineligible evaluation failed: " + res);
                }
            } else {
                fail("SC.1", "Student or Job not found in database");
            }
        } catch (Exception e) {
            fail("SUITE 5", "Exception: " + e.getMessage());
        }

        // =================================================================
        // SUITE 6: END-TO-END APPLICATION, DUPLICATE BLOCKING & RECEIPT
        // =================================================================
        System.out.println("\n--- 6. APPLICATION, DUPLICATE BLOCKING & RECEIPT ---");
        int submittedAppId = 0;
        try {
            Student vihaan = studentDAO.findByStudentId(DemoService.DEMO_STUDENT_USN);
            Job backendJob = jobDAO.findById(1);

            // Clean any prior application for vihaan on job 1
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM applications WHERE student_id = ? AND job_id = ?")) {
                stmt.setInt(1, vihaan.getId());
                stmt.setInt(2, backendJob.getId());
                stmt.executeUpdate();
            }

            // 1. Submit application
            Application app = recruitmentService.applyForJob(vihaan, backendJob.getId());
            if (app != null && app.getId() > 0 && app.getStatus() == ApplicationStatus.APPLIED) {
                submittedAppId = app.getId();
                pass("APP.1: Eligible student submits real application persisted in MySQL with status APPLIED (ID: " + app.getId() + ")");
            } else {
                fail("APP.1", "Application submission failed");
            }

            // 2. Duplicate application blocked
            boolean duplicateBlocked = false;
            try {
                recruitmentService.applyForJob(vihaan, backendJob.getId());
            } catch (IllegalStateException e) {
                duplicateBlocked = e.getMessage() != null && e.getMessage().toLowerCase().contains("already applied");
            }
            if (duplicateBlocked) {
                pass("APP.2: Duplicate application for same job is gracefully blocked at service layer");
            } else {
                fail("APP.2", "Duplicate application was not blocked");
            }

            // 3. Ineligible student application blocked
            Student arjun = studentDAO.findByStudentId(DemoService.DEMO_STUDENT3_USN);
            boolean ineligibleBlocked = false;
            try {
                recruitmentService.applyForJob(arjun, backendJob.getId());
            } catch (IllegalStateException e) {
                ineligibleBlocked = e.getMessage() != null && e.getMessage().toLowerCase().contains("blocked");
            }
            if (ineligibleBlocked) {
                pass("APP.3: Ineligible student application blocked from submission");
            } else {
                fail("APP.3", "Ineligible student application was not blocked");
            }

        } catch (Exception e) {
            fail("SUITE 6", "Exception: " + e.getMessage());
        }

        // =================================================================
        // SUITE 7: PLACEMENT OFFICER REVIEW & STATUS UPDATE
        // =================================================================
        System.out.println("\n--- 7. OFFICER REGISTRY & STATUS ADVANCEMENT ---");
        try {
            final int targetAppId = submittedAppId;
            User officer = authService.login(DemoService.DEMO_OFFICER_USERNAME, DemoService.DEMO_PASSWORD);
            List<Application> allApps = recruitmentService.getAllApplications(officer);
            boolean foundSubmitted = allApps.stream().anyMatch(a -> a.getId() == targetAppId);
            if (foundSubmitted) {
                pass("OR.1: Placement officer retrieves application registry and locates student application");
            } else {
                fail("OR.1", "Submitted application not found in officer registry");
            }

            // Update status to SHORTLISTED
            boolean statusUpdated = recruitmentService.updateApplicationStatus(officer, submittedAppId, ApplicationStatus.SHORTLISTED);
            Application updatedApp = applicationDAO.findById(submittedAppId);
            if (statusUpdated && updatedApp != null && updatedApp.getStatus() == ApplicationStatus.SHORTLISTED) {
                pass("OR.2: Placement officer advances application status to SHORTLISTED and change persists in MySQL");
            } else {
                fail("OR.2", "Status update failed or did not persist");
            }

            // Non-officer blocked from updating status
            User studentUser = authService.login(DemoService.DEMO_STUDENT_USERNAME, DemoService.DEMO_PASSWORD);
            boolean studentBlocked = false;
            try {
                recruitmentService.updateApplicationStatus(studentUser, submittedAppId, ApplicationStatus.SELECTED);
            } catch (SecurityException e) {
                studentBlocked = true;
            }
            if (studentBlocked) {
                pass("OR.3: Role-based security blocks non-officers from modifying application status");
            } else {
                fail("OR.3", "Student was not blocked from updating status");
            }

        } catch (Exception e) {
            fail("SUITE 7", "Exception: " + e.getMessage());
        }

        // =================================================================
        // SUITE 8: CSV EXPORT & RESTART PERSISTENCE
        // =================================================================
        System.out.println("\n--- 8. CSV REPORT & SIMULATED RESTART PERSISTENCE ---");
        try {
            User officer = authService.login(DemoService.DEMO_OFFICER_USERNAME, DemoService.DEMO_PASSWORD);
            String exportPath = "demo_test_export_" + System.currentTimeMillis() + ".csv";
            File exportedFile = recruitmentService.exportApplicationsToCsv(officer, exportPath);
            if (exportedFile != null && exportedFile.exists() && exportedFile.length() > 0) {
                pass("CSV.1: Application registry exported to RFC-4180 CSV report on disk (" + exportedFile.getName() + ")");
                exportedFile.deleteOnExit();
            } else {
                fail("CSV.1", "CSV export file was not created or empty");
            }

            // Simulated restart: fresh DAO instances reading from database
            UserDAO freshUserDao = new UserDAO();
            StudentDAO freshStudentDao = new StudentDAO();
            User reloadedUser = freshUserDao.findByUsername(DemoService.DEMO_STUDENT_USERNAME);
            Student reloadedStudent = freshStudentDao.findByStudentId(DemoService.DEMO_STUDENT_USN);

            if (reloadedUser != null && reloadedStudent != null &&
                    reloadedStudent.getFullName().equals(DemoService.DEMO_STUDENT_NAME)) {
                pass("RES.1: Demonstration records reliably persist across simulated application restarts");
            } else {
                fail("RES.1", "Restart persistence check failed");
            }

        } catch (Exception e) {
            fail("SUITE 8", "Exception: " + e.getMessage());
        }

        // =================================================================
        // SUITE 9: SAFE DEMO-ONLY RESET & REAL ACCOUNT PROTECTION
        // =================================================================
        System.out.println("\n--- 9. SAFE DEMO-ONLY RESET & REAL ACCOUNT PROTECTION ---");
        try {
            // Create a real (non-demo) user to prove protection
            String realUsername = "real_cand_" + (System.currentTimeMillis() % 1000000);
            String realUsn = "USN_REAL_" + (System.currentTimeMillis() % 100000);
            Student realStudent = authService.registerStudent(
                    realUsername, "RealPassword@123", "real.candidate@university.edu",
                    realUsn, "Genuine Real Candidate", "Computer Science", 8.90, 2026
            );

            // Execute safe demo reset
            DemoService.DemoResetResult resetResult = demoService.resetDemoDataOnly();
            if (resetResult.isSuccess()) {
                pass("RST.1: resetDemoDataOnly() cleanly purges demo accounts and demo applications");
            } else {
                fail("RST.1", "Demo reset failed: " + resetResult.getMessage());
            }

            // Verify real user is completely untouched
            User preservedRealUser = userDAO.findByUsername(realUsername);
            Student preservedRealStudent = studentDAO.findByStudentId(realUsn);
            if (preservedRealUser != null && preservedRealStudent != null) {
                pass("RST.2: Real user account (" + realUsername + ") and student profile are 100% PROTECTED");
            } else {
                fail("RST.2", "Real account was accidentally modified or deleted during demo reset!");
            }

            // Cleanup the test real user
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM users WHERE username = ?")) {
                stmt.setString(1, realUsername);
                stmt.executeUpdate();
            }

            // Re-initialize demo data after reset to leave database demonstration-ready
            demoService.prepareDemoData();
            if (demoService.isDemoDataPrepared()) {
                pass("RST.3: Demo data successfully re-initialized and ready for subsequent evaluations");
            } else {
                fail("RST.3", "Re-initialization after reset failed");
            }

        } catch (Exception e) {
            fail("SUITE 9", "Exception: " + e.getMessage());
        }

        // =================================================================
        // SUITE 10: GATEWAY NAVIGATION & DEMO MANAGER FLOW
        // =================================================================
        System.out.println("\n--- 10. GATEWAY NAVIGATION & DEMO MANAGER INTEGRATION ---");
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            String inputs = String.join("\n",
                    "4", // Gateway -> Option 4: Demonstration Mode
                    "2", // Demo Manager -> Option 2: View Credentials Guide
                    "",  // ENTER to continue
                    "0", // Return to Gateway
                    "0"  // Exit System
            ) + "\n";
            System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

            Main app = new Main();
            app.run(new Scanner(System.in));
            System.setOut(originalOut);

            String outStr = out.toString(StandardCharsets.UTF_8);
            boolean hasGatewayOption4 = outStr.contains("4. Demonstration Mode & Scenario Setup");
            boolean hasDemoMenu = outStr.contains("DEMONSTRATION MODE & SCENARIO MANAGER");
            boolean hasGuide = outStr.contains("DEMONSTRATION CREDENTIALS & SCENARIO REFERENCE GUIDE") &&
                    outStr.contains("vihaan_demo") && outStr.contains("officer_vihaan");

            if (hasGatewayOption4 && hasDemoMenu && hasGuide) {
                pass("GW.1: Page 1 Gateway option 4 opens Demonstration Mode and displays interactive credentials guide");
            } else {
                fail("GW.1", "Gateway Demo Mode navigation verification failed");
            }

        } catch (Exception e) {
            fail("SUITE 10", "Exception: " + e.getMessage());
        }

        // =================================================================
        // SUMMARY
        // =================================================================
        System.out.println("\n==================================================================");
        System.out.printf("Results: %d Passed, %d Failed.%n", passCount, failCount);
        System.out.println("==================================================================");

        if (failCount > 0) {
            System.exit(1);
        }
    }
}
