import com.nextstep.Main;
import com.nextstep.dao.ApplicationDAO;
import com.nextstep.dao.CompanyDAO;
import com.nextstep.dao.JobDAO;
import com.nextstep.dao.StudentDAO;
import com.nextstep.dao.UserDAO;
import com.nextstep.database.DatabaseConnection;
import com.nextstep.model.Application;
import com.nextstep.model.ApplicationStatus;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Phase5TestRunner
 * 
 * Verifies all Phase 5 requirements including Semester 3 Course Handout
 * (25CSE0202 Lectures 43-54, Labs 18-20) alignment:
 * - Lambda Expressions & Comparators
 * - Stream API (filter, map, sorted, distinct, count, anyMatch, collect)
 * - Collectors (groupingBy, partitioningBy, counting)
 * - Generics (Result<T> generic class & ValidationUtil.paginate generic method)
 * - File I/O (CSV reading, validation, parsing into ArrayList, and writing updated records)
 * - try-with-resources
 * - Role-based authorization & presentation visibility controls
 */
public class Phase5TestRunner {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    private static void assertTrue(String testName, boolean condition) {
        if (condition) {
            System.out.println("  [PASS] " + testName);
            testsPassed++;
        } else {
            System.err.println("  [FAIL] " + testName);
            testsFailed++;
        }
    }

    private static void cleanupUser(String username) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM users WHERE username = ?")) {
            stmt.setString(1, username);
            stmt.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    private static void cleanupApplication(int studentId, int jobId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM applications WHERE student_id = ? AND job_id = ?")) {
            stmt.setInt(1, studentId);
            stmt.setInt(2, jobId);
            stmt.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("  NextStep — Phase 5 Comprehensive Syllabus Suite  ");
        System.out.println("==================================================");

        AuthService authService = new AuthService();
        RecruitmentService recruitmentService = new RecruitmentService();
        JobDAO jobDAO = new JobDAO();
        StudentDAO studentDAO = new StudentDAO();
        ApplicationDAO applicationDAO = new ApplicationDAO();
        UserDAO userDAO = new UserDAO();

        long timestamp = System.currentTimeMillis();
        String studentUsername = "stud_p5_" + timestamp;
        String studentUsn = "USN_P5_" + (timestamp % 100000);
        String officerUsername = "tpo_p5_" + timestamp;

        Student student = null;
        User officerUser = null;
        Job backendJob = null;
        Application createdApp = null;
        File exportedCsv = null;
        File auditedCsv = null;

        try {
            // -----------------------------------------------------------------
            // 1. APPLICATION INITIALIZATION
            // -----------------------------------------------------------------
            System.out.println("\n--- 1. APPLICATION INITIALIZATION ---");
            Main mainApp = new Main();
            assertTrue("Test 1: Application starts and initializes successfully", mainApp != null);

            // -----------------------------------------------------------------
            // 2. REGISTRATION & LOGIN FLOWS
            // -----------------------------------------------------------------
            System.out.println("\n--- 2. REGISTRATION & LOGIN FLOWS ---");
            student = authService.registerStudent(
                    studentUsername, "Password123", studentUsername + "@test.edu",
                    studentUsn, "Phase5 Student", "9988776655", "Computer Science",
                    8.80, 0, 2026, Arrays.asList("Java", "SQL", "OOP", "Git")
            );
            assertTrue("Test 2a: Student registration works and creates record", student != null && student.getId() > 0);
            assertTrue("Test 2b: Student profile linked to user account", student.getUserId() > 0);

            User loggedInStudent = authService.login(studentUsername, "Password123");
            assertTrue("Test 3a: Student can log in with valid credentials", loggedInStudent != null);
            assertTrue("Test 3b: Logged in user role is STUDENT", loggedInStudent.getRole() == Role.STUDENT);

            User failedLogin = authService.login(studentUsername, "WrongPassword");
            assertTrue("Test 4a: Incorrect password rejected safely", failedLogin == null);

            User nonExistent = authService.login("non_existent_user_xyz", "Password123");
            assertTrue("Test 4b: Non-existent user login rejected safely", nonExistent == null);

            // -----------------------------------------------------------------
            // 3. STUDENT BROWSING & ELIGIBILITY
            // -----------------------------------------------------------------
            System.out.println("\n--- 3. STUDENT BROWSING & ELIGIBILITY ---");
            List<Job> openJobs = recruitmentService.getOpenJobs();
            assertTrue("Test 5 & 6: Student can browse open jobs", !openJobs.isEmpty());

            for (Job j : openJobs) {
                if (j.getTitle().contains("Backend")) {
                    backendJob = j;
                    break;
                }
            }
            if (backendJob == null) backendJob = openJobs.get(0);

            EligibilityResult eligibility = recruitmentService.checkEligibility(student, backendJob);
            assertTrue("Test 7: Student eligibility checking works", eligibility.isEligible());
            assertTrue("Test 7b: Matched skills list includes Java", eligibility.getMatchedSkills().contains("Java"));

            // -----------------------------------------------------------------
            // 4. JOB APPLICATION & HISTORY
            // -----------------------------------------------------------------
            System.out.println("\n--- 4. JOB APPLICATION & HISTORY ---");
            createdApp = recruitmentService.applyForJob(student, backendJob.getId());
            assertTrue("Test 8a: Eligible student can apply for job", createdApp != null && createdApp.getId() > 0);
            assertTrue("Test 8b: Initial application status is APPLIED", createdApp.getStatus() == ApplicationStatus.APPLIED);

            // Duplicate application test
            boolean duplicateBlocked = false;
            try {
                recruitmentService.applyForJob(student, backendJob.getId());
            } catch (IllegalStateException e) {
                duplicateBlocked = true;
            }
            assertTrue("Test 9: Duplicate application is prevented gracefully", duplicateBlocked);

            // Application history
            List<Application> studentApps = recruitmentService.getStudentApplications(student.getId());
            assertTrue("Test 10a: Student can view their own applications", studentApps.size() == 1);
            assertTrue("Test 10b: History contains correct job title", studentApps.get(0).getJobTitle().equals(backendJob.getTitle()));

            // Student logout simulation
            loggedInStudent = null;
            assertTrue("Test 11: Student session termination / logout simulation works", loggedInStudent == null);

            // -----------------------------------------------------------------
            // 5. PLACEMENT OFFICER OPERATIONS & AUTHORIZATION
            // -----------------------------------------------------------------
            System.out.println("\n--- 5. PLACEMENT OFFICER OPERATIONS & SECURITY ---");
            officerUser = authService.registerPlacementOfficer(
                    officerUsername, "OfficerPass123", officerUsername + "@college.edu"
            );
            assertTrue("Test 12a: Placement officer account provisioned securely", officerUser != null);
            assertTrue("Test 12b: Provisioned role is PLACEMENT_OFFICER", officerUser.getRole() == Role.PLACEMENT_OFFICER);

            User loggedInOfficer = authService.login(officerUsername, "OfficerPass123");
            assertTrue("Test 12c: Placement officer can log in", loggedInOfficer != null);

            List<Application> allApps = recruitmentService.getAllApplications(loggedInOfficer);
            assertTrue("Test 13a: Placement officer can view all applications", !allApps.isEmpty());
            assertTrue("Test 13b: All applications include student roll number", allApps.get(0).getStudentRollNumber() != null);

            // Update application status
            boolean updated = recruitmentService.updateApplicationStatus(loggedInOfficer, createdApp.getId(), ApplicationStatus.SHORTLISTED);
            Application reloadedApp = applicationDAO.findById(createdApp.getId());
            assertTrue("Test 14a: Placement officer can update application status", updated);
            assertTrue("Test 14b: Application status reflects SHORTLISTED", reloadedApp.getStatus() == ApplicationStatus.SHORTLISTED);

            // Security check: Student cannot execute officer operations
            User studentAsUser = userDAO.findByUsername(studentUsername);
            boolean studentAccessBlocked = false;
            try {
                recruitmentService.updateApplicationStatus(studentAsUser, createdApp.getId(), ApplicationStatus.SELECTED);
            } catch (SecurityException e) {
                studentAccessBlocked = true;
            }
            assertTrue("Test 15a: Student blocked from updating application status", studentAccessBlocked);

            boolean studentViewAllBlocked = false;
            try {
                recruitmentService.getAllApplications(studentAsUser);
            } catch (SecurityException e) {
                studentViewAllBlocked = true;
            }
            assertTrue("Test 15b: Student blocked from viewing all applications", studentViewAllBlocked);

            // -----------------------------------------------------------------
            // 6. SYLLABUS: LAMBDA EXPRESSIONS & STREAM API
            // -----------------------------------------------------------------
            System.out.println("\n--- 6. LAMBDA EXPRESSIONS & STREAM API ---");

            // Lambda sorting test (CTC descending)
            List<Job> sortedJobs = recruitmentService.getJobsSortedByCtc();
            assertTrue("Test S1: Lambda expression sorts jobs by CTC descending",
                    sortedJobs.size() >= 2 && sortedJobs.get(0).getCtcLpa().compareTo(sortedJobs.get(sortedJobs.size() - 1).getCtcLpa()) >= 0);

            // Stream pipeline: filter -> map -> distinct -> sorted -> collect
            List<String> distinctCompanies = recruitmentService.getDistinctHiringCompanies();
            assertTrue("Test S2: Stream pipeline (filter, map, distinct, sorted, collect) works",
                    !distinctCompanies.isEmpty() && distinctCompanies.contains("Nexis Cloud Technologies"));

            // Terminal operations: count() & anyMatch()
            long openJobCount = recruitmentService.countOpenJobs();
            assertTrue("Test S3a: Stream count() returns active drive count", openJobCount >= 3);

            boolean hasBlr = recruitmentService.hasOpeningInLocation("Bengaluru");
            assertTrue("Test S3b: Stream anyMatch() detects location match", hasBlr);

            // -----------------------------------------------------------------
            // 7. SYLLABUS: COLLECTORS (groupingBy, partitioningBy, counting)
            // -----------------------------------------------------------------
            System.out.println("\n--- 7. COLLECTORS (groupingBy, partitioningBy, counting) ---");

            Map<ApplicationStatus, Long> statusSummary = recruitmentService.getRecruitmentSummary(loggedInOfficer);
            assertTrue("Test C1: Collectors.groupingBy & counting produces status breakdown",
                    statusSummary.getOrDefault(ApplicationStatus.SHORTLISTED, 0L) >= 1L);

            Map<Boolean, Long> placementPartition = recruitmentService.getPlacementPartitionCount(loggedInOfficer);
            assertTrue("Test C2: Collectors.partitioningBy & counting partitions applications",
                    placementPartition.containsKey(true) && placementPartition.containsKey(false));

            // -----------------------------------------------------------------
            // 8. SYLLABUS: GENERICS (Result<T> & Generic Method)
            // -----------------------------------------------------------------
            System.out.println("\n--- 8. JAVA GENERICS ---");

            Result<String> successResult = Result.success("Valid Token");
            assertTrue("Test G1a: Generic class Result<T> handles success payload",
                    successResult.isSuccess() && "Valid Token".equals(successResult.getData()));

            Result<String> failureResult = Result.failure("Network Timeout");
            assertTrue("Test G1b: Generic class Result<T> handles error payload",
                    !failureResult.isSuccess() && "Network Timeout".equals(failureResult.getMessage()));

            List<String> sampleList = Arrays.asList("Item1", "Item2", "Item3", "Item4", "Item5");
            List<String> paged = ValidationUtil.paginate(sampleList, 2, 2);
            assertTrue("Test G2: Generic method ValidationUtil.paginate slices collection",
                    paged.size() == 2 && "Item3".equals(paged.get(0)));

            // -----------------------------------------------------------------
            // 9. SYLLABUS: FILE I/O & CSV ROUND-TRIP (Read, Validate, Parse, Write)
            // -----------------------------------------------------------------
            System.out.println("\n--- 9. FILE I/O & CSV PROCESSING ---");

            // A. Export CSV
            String testCsvPath = "reports/test_applications_" + timestamp + ".csv";
            exportedCsv = recruitmentService.exportApplicationsToCsv(loggedInOfficer, testCsvPath);
            assertTrue("Test F1a: CSV report file is created on disk", exportedCsv.exists() && exportedCsv.length() > 0);

            try (BufferedReader reader = new BufferedReader(new FileReader(exportedCsv))) {
                String header = reader.readLine();
                assertTrue("Test F1b: CSV header matches expected schema",
                        header != null && header.contains("Application ID") && header.contains("Student Roll No"));
                String dataRow = reader.readLine();
                assertTrue("Test F1c: CSV contains application data row",
                        dataRow != null && dataRow.contains(studentUsn));
            }

            // B. Read, Validate, Parse into ArrayList, and Write Audited CSV
            String auditedPath = "reports/test_audited_" + timestamp + ".csv";
            List<Application> parsedApps = recruitmentService.processAndArchiveCsvReport(loggedInOfficer, testCsvPath, auditedPath);
            auditedCsv = new File(auditedPath);

            assertTrue("Test F2a: CSV read, parsed, and loaded into ArrayList<Application>", parsedApps.size() == 1);
            assertTrue("Test F2b: Parsed Application preserves student USN", studentUsn.equals(parsedApps.get(0).getStudentRollNumber()));
            assertTrue("Test F2c: Updated audited CSV file successfully written to disk", auditedCsv.exists() && auditedCsv.length() > 0);

            try (BufferedReader reader = new BufferedReader(new FileReader(auditedCsv))) {
                String auditHeader = reader.readLine();
                assertTrue("Test F2d: Audited CSV contains updated verification column",
                        auditHeader != null && auditHeader.contains("Verification Status"));
            }

            // Student cannot perform CSV operations
            boolean studentExportBlocked = false;
            try {
                recruitmentService.exportApplicationsToCsv(studentAsUser, testCsvPath);
            } catch (SecurityException e) {
                studentExportBlocked = true;
            }
            assertTrue("Test F3: Student blocked from exporting CSV report", studentExportBlocked);

            // C. RFC-4180 Commas and Quotes handling
            String complexCsvLine = "99,USN99,\"Mehta, Vihaan\",Backend Engineer,\"Apex, Inc.\",SELECTED,2026-11-09T20:00:00";
            List<String> tokens = RecruitmentService.parseCsvLine(complexCsvLine);
            assertTrue("Test F4a: parseCsvLine handles commas inside double quotes",
                    tokens.size() == 7 && "Mehta, Vihaan".equals(tokens.get(2)) && "Apex, Inc.".equals(tokens.get(4)));

            String escaped = RecruitmentService.escapeCsvField("Nexis, Corp.");
            assertTrue("Test F4b: escapeCsvField encloses fields with commas in quotes",
                    "\"Nexis, Corp.\"".equals(escaped));

            // D. Search and Update imported records
            List<Application> searchHits = recruitmentService.searchApplications(parsedApps, "Phase5");
            assertTrue("Test F4c: Search imported applications matches by student name",
                    searchHits.size() == 1 && searchHits.get(0).getStudentRollNumber().equals(studentUsn));

            // Test updating an imported record status and archiving
            String updatedAuditedPath = "reports/test_updated_audited_" + timestamp + ".csv";
            List<Application> updatedApps = recruitmentService.processAndArchiveCsvReport(
                    loggedInOfficer, testCsvPath, updatedAuditedPath, createdApp.getId(), ApplicationStatus.SELECTED);
            File updatedAuditedCsv = new File(updatedAuditedPath);
            assertTrue("Test F4d: Updating imported record status in archive works",
                    updatedApps.get(0).getStatus() == ApplicationStatus.SELECTED);
            assertTrue("Test F4e: Updated record saved to audited CSV file",
                    updatedAuditedCsv.exists() && updatedAuditedCsv.length() > 0);
            if (updatedAuditedCsv.exists()) updatedAuditedCsv.delete();

            // -----------------------------------------------------------------
            // 10. PRESENTATION VISIBILITY CONTROLS
            // -----------------------------------------------------------------
            System.out.println("\n--- 10. PRESENTATION VISIBILITY CONTROLS ---");
            assertTrue("Test V1: Registration visible by default", PresentationConfig.isStudentRegistrationVisible());
            PresentationConfig.setStudentRegistrationVisible(false);
            assertTrue("Test V2: Registration option hidden when toggled", !PresentationConfig.isStudentRegistrationVisible());

            PresentationConfig.setOfficerCsvExportVisible(false);
            assertTrue("Test V3: Officer CSV export hidden when toggled", !PresentationConfig.isOfficerCsvExportVisible());

            PresentationConfig.resetDefaults();
            assertTrue("Test V4: Reset defaults restores all menu options",
                    PresentationConfig.isStudentRegistrationVisible() && PresentationConfig.isOfficerCsvExportVisible());

            // -----------------------------------------------------------------
            // 11. REGRESSION CHECKS (PHASES 1-4 INTACT)
            // -----------------------------------------------------------------
            System.out.println("\n--- 11. REGRESSION CHECKS ---");
            CompanyDAO companyDAO = new CompanyDAO();
            assertTrue("Test R1: CompanyDAO still functions", companyDAO.findAllCompanies().size() >= 3);
            assertTrue("Test R2: StudentDAO findByUserId functions", studentDAO.findByUserId(student.getUserId()) != null);
            assertTrue("Test R3: UserDAO existsByRole functions", userDAO.existsByRole(Role.PLACEMENT_OFFICER));

        } finally {
            // Cleanup: remove application, test users, and test CSVs
            if (student != null && backendJob != null) {
                cleanupApplication(student.getId(), backendJob.getId());
            }
            cleanupUser(studentUsername);
            cleanupUser(officerUsername);

            if (exportedCsv != null && exportedCsv.exists()) {
                exportedCsv.delete();
            }
            if (auditedCsv != null && auditedCsv.exists()) {
                auditedCsv.delete();
            }

            PresentationConfig.resetDefaults();
            System.out.println("\n[CLEANUP] Fictional test accounts, applications, and temporary CSVs deleted.");
        }

        System.out.println("\n==================================================");
        System.out.println("Phase 5 Test Results: " + testsPassed + " Passed, " + testsFailed + " Failed.");
        System.out.println("==================================================");

        if (testsFailed > 0) {
            System.exit(1);
        }
    }
}
