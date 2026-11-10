import com.nextstep.dao.ApplicationDAO;
import com.nextstep.dao.CompanyDAO;
import com.nextstep.dao.JobDAO;
import com.nextstep.dao.StudentDAO;
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Phase4TestRunner {

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
        System.out.println("     NextStep — Phase 4 Comprehensive Test Suite   ");
        System.out.println("==================================================");

        AuthService authService = new AuthService();
        StudentDAO studentDAO = new StudentDAO();
        JobDAO jobDAO = new JobDAO();
        RecruitmentService recruitmentService = new RecruitmentService();
        ApplicationDAO applicationDAO = new ApplicationDAO();

        String testUsername1 = "student_p4_a_" + System.currentTimeMillis();
        String testUsn1 = "USN_4A_" + (System.currentTimeMillis() % 100000);

        String testUsername2 = "student_p4_b_" + (System.currentTimeMillis() + 1);
        String testUsn2 = "USN_4B_" + ((System.currentTimeMillis() + 1) % 100000);

        Student student1 = null;
        Student student2 = null;
        List<Job> allJobs = null;
        Job backendJob = null;

        try {
            // Setup: Register 2 fictional test students
            // Student 1: Computer Science, CGPA 8.50, 0 Backlogs, Skills: Java, SQL, OOP, Git
            student1 = authService.registerStudent(
                    testUsername1, "Password123", testUsername1 + "@test.com", testUsn1,
                    "Alice Tester", "9876543201", "Computer Science",
                    8.50, 0, 2026, Arrays.asList("Java", "SQL", "OOP", "Git")
            );

            // Student 2: Mechanical, CGPA 6.20, 3 Backlogs, Skills: C
            student2 = authService.registerStudent(
                    testUsername2, "Password123", testUsername2 + "@test.com", testUsn2,
                    "Bob Tester", "9876543202", "Mechanical",
                    6.20, 3, 2026, Collections.singletonList("C")
            );

            // -----------------------------------------------------------------
            // TEST 1 & 2: JOB BROWSING & DETAILS
            // -----------------------------------------------------------------
            System.out.println("\n--- 1. JOB BROWSING & DETAILS ---");
            allJobs = jobDAO.findAllJobs();
            assertTrue("Test 1: Retrieve available jobs", allJobs.size() >= 3);

            Job job1 = jobDAO.findById(allJobs.get(0).getId());
            assertTrue("Test 2a: Retrieve individual job details", job1 != null);
            assertTrue("Test 2b: Job has company name", job1.getCompanyName() != null);
            assertTrue("Test 2c: Job has CTC", job1.getCtcLpa() != null);
            assertTrue("Test 2d: Job has eligible branches", !job1.getEligibleBranches().isEmpty());
            assertTrue("Test 2e: Job has required skills", !job1.getRequiredSkills().isEmpty());

            // Pick Nexis Cloud Backend Job (Min CGPA: 7.50, Max Backlogs: 0, Branch: CSE/IT, Skills: Java, SQL, OOP, Git)
            for (Job j : allJobs) {
                if (j.getTitle().contains("Backend")) {
                    backendJob = j;
                    break;
                }
            }
            if (backendJob == null) backendJob = allJobs.get(0);

            // -----------------------------------------------------------------
            // TEST 3 - 7: ELIGIBILITY ENGINE
            // -----------------------------------------------------------------
            System.out.println("\n--- 2. STUDENT ELIGIBILITY CHECKS ---");

            // 3. Eligible student passes
            EligibilityResult eligibleResult = recruitmentService.checkEligibility(student1, backendJob);
            assertTrue("Test 3: Eligible student passes all criteria", eligibleResult.isEligible());
            assertTrue("Test 3b: Matched skills list is populated", eligibleResult.getMatchedSkills().contains("Java"));

            // 4. Low CGPA rejected
            Student lowCgpaStudent = new Student(0, student1.getUserId(), "LOW_CGPA", "Low CGPA", "123",
                    "Computer Science", 6.50, 0, 2026, Arrays.asList("Java", "SQL"));
            EligibilityResult lowCgpaResult = recruitmentService.checkEligibility(lowCgpaStudent, backendJob);
            assertTrue("Test 4: Low CGPA student is rejected", !lowCgpaResult.isEligible() &&
                    lowCgpaResult.getReasons().get(0).contains("CGPA"));

            // 5. Excessive backlogs rejected
            Student backlogsStudent = new Student(0, student1.getUserId(), "BACKLOGS", "Backlogs", "123",
                    "Computer Science", 8.50, 2, 2026, Arrays.asList("Java", "SQL"));
            EligibilityResult backlogsResult = recruitmentService.checkEligibility(backlogsStudent, backendJob);
            assertTrue("Test 5: Student with excessive backlogs is rejected", !backlogsResult.isEligible() &&
                    backlogsResult.getReasons().get(0).contains("backlogs"));

            // 6. Ineligible branch rejected
            Student wrongBranchStudent = new Student(0, student1.getUserId(), "CIVIL", "Civil Eng", "123",
                    "Civil Engineering", 8.50, 0, 2026, Arrays.asList("Java", "SQL"));
            EligibilityResult branchResult = recruitmentService.checkEligibility(wrongBranchStudent, backendJob);
            assertTrue("Test 6: Ineligible branch is rejected", !branchResult.isEligible() &&
                    branchResult.getReasons().get(0).contains("Branch"));

            // 7. Missing required skills detected
            Student unskilledStudent = new Student(0, student1.getUserId(), "UNSKILLED", "Unskilled", "123",
                    "Computer Science", 8.50, 0, 2026, Collections.singletonList("HTML"));
            EligibilityResult skillResult = recruitmentService.checkEligibility(unskilledStudent, backendJob);
            assertTrue("Test 7a: Missing required skills are detected", !skillResult.getMissingSkills().isEmpty() &&
                    skillResult.getMissingSkills().contains("Java"));
            assertTrue("Test 7b: Student lacking required skills is marked ineligible", !skillResult.isEligible());

            // -----------------------------------------------------------------
            // TEST 8 & 9: JOB APPLICATION & DUPLICATE PREVENTION
            // -----------------------------------------------------------------
            System.out.println("\n--- 3. JOB APPLICATION & DUPLICATE PREVENTION ---");

            // 8. Eligible student applies successfully
            Application app1 = recruitmentService.applyForJob(student1, backendJob.getId());
            assertTrue("Test 8a: Eligible student can apply", app1 != null && app1.getId() > 0);
            assertTrue("Test 8b: Initial status is APPLIED", app1.getStatus() == ApplicationStatus.APPLIED);

            // Ineligible student application blocked
            boolean ineligibleBlocked = false;
            try {
                recruitmentService.applyForJob(student2, backendJob.getId());
            } catch (IllegalStateException e) {
                ineligibleBlocked = true;
            }
            assertTrue("Test 8c: Ineligible student cannot apply", ineligibleBlocked);

            // 9. Duplicate application rejected gracefully
            boolean duplicateBlocked = false;
            try {
                recruitmentService.applyForJob(student1, backendJob.getId());
            } catch (IllegalStateException e) {
                duplicateBlocked = true;
            }
            assertTrue("Test 9: Duplicate application is rejected gracefully", duplicateBlocked);

            // -----------------------------------------------------------------
            // TEST 10 - 12: APPLICATION HISTORY & STATUS
            // -----------------------------------------------------------------
            System.out.println("\n--- 4. APPLICATION HISTORY & STATUS ---");

            // 10. Student views their own history
            List<Application> student1Apps = recruitmentService.getStudentApplications(student1.getId());
            assertTrue("Test 10a: Student can view their application history", !student1Apps.isEmpty());
            assertTrue("Test 10b: Associated job title is populated", student1Apps.get(0).getJobTitle().equals(backendJob.getTitle()));
            assertTrue("Test 10c: Associated company name is populated", student1Apps.get(0).getCompanyName() != null);

            // 11. Student isolation (Student 2 has 0 applications and cannot view Student 1's apps)
            List<Application> student2Apps = recruitmentService.getStudentApplications(student2.getId());
            assertTrue("Test 11: One student cannot view another student's applications", student2Apps.isEmpty());

            // 12. Application status update & authorization
            User officerUser = new User("placement_officer", "hash", Role.PLACEMENT_OFFICER, "tpo@college.edu");
            User studentUser = new User("student_actor", "hash", Role.STUDENT, "student@college.edu");

            // 12a: Student attempting status update is blocked by SecurityException
            boolean studentBlocked = false;
            try {
                recruitmentService.updateApplicationStatus(studentUser, app1.getId(), ApplicationStatus.SHORTLISTED);
            } catch (SecurityException e) {
                studentBlocked = true;
            }
            assertTrue("Test 12a: Student cannot update application status (SecurityException)", studentBlocked);

            // 12b: Unauthenticated caller (null user) is blocked
            boolean nullBlocked = false;
            try {
                recruitmentService.updateApplicationStatus(null, app1.getId(), ApplicationStatus.SHORTLISTED);
            } catch (SecurityException e) {
                nullBlocked = true;
            }
            assertTrue("Test 12b: Unauthenticated user cannot update application status", nullBlocked);

            // 12c: Placement officer can update status
            boolean statusUpdated = recruitmentService.updateApplicationStatus(officerUser, app1.getId(), ApplicationStatus.SHORTLISTED);
            Application reloadedApp = applicationDAO.findById(app1.getId());
            assertTrue("Test 12c: Placement officer status update succeeds", statusUpdated);
            assertTrue("Test 12d: Application status is displayed correctly as SHORTLISTED",
                    reloadedApp.getStatus() == ApplicationStatus.SHORTLISTED);

            // -----------------------------------------------------------------
            // TEST 13: REGRESSION OF PHASE 1-3
            // -----------------------------------------------------------------
            System.out.println("\n--- 5. REGRESSION CHECK ---");
            CompanyDAO companyDAO = new CompanyDAO();
            assertTrue("Test 13a: CompanyDAO still functions", companyDAO.findAllCompanies().size() >= 3);
            assertTrue("Test 13b: StudentDAO findByUserId functions", studentDAO.findByUserId(student1.getUserId()) != null);

        } finally {
            // Cleanup: remove test applications first, then test users (cascades to students & student_skills)
            if (student1 != null && backendJob != null) {
                cleanupApplication(student1.getId(), backendJob.getId());
            }
            cleanupUser(testUsername1);
            cleanupUser(testUsername2);
            System.out.println("\n[CLEANUP] Temporary test students and applications cleaned up from MySQL.");
        }

        System.out.println("\n==================================================");
        System.out.println("Phase 4 Test Results: " + testsPassed + " Passed, " + testsFailed + " Failed.");
        System.out.println("==================================================");

        if (testsFailed > 0) {
            System.exit(1);
        }
    }
}
