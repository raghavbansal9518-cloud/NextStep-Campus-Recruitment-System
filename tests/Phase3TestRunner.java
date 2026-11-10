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
import com.nextstep.service.AuthService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.List;

public class Phase3TestRunner {

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

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("     NextStep — Phase 3 Comprehensive Test Suite   ");
        System.out.println("==================================================");

        AuthService authService = new AuthService();
        StudentDAO studentDAO = new StudentDAO();
        UserDAO userDAO = new UserDAO();
        CompanyDAO companyDAO = new CompanyDAO();
        JobDAO jobDAO = new JobDAO();

        String testUsername = "student_test_" + System.currentTimeMillis();
        String testUsn = "USN_" + (System.currentTimeMillis() % 100000);
        String testEmail = testUsername + "@campus.example.com";
        String testPassword = "SecretPassword123";

        try {
            // -----------------------------------------------------------------
            // TEST SUITE A: STUDENT REGISTRATION & VALIDATION
            // -----------------------------------------------------------------
            System.out.println("\n--- TEST SUITE A: Student Registration & Validation ---");

            // 1. Valid registration
            Student registeredStudent = authService.registerStudent(
                    testUsername, testPassword, testEmail, testUsn,
                    "Raghav Bansal", "9876543210", "Computer Science",
                    8.85, 0, 2026, Arrays.asList("Java", "SQL", "Git")
            );
            assertTrue("A1: Valid student registration succeeds", registeredStudent != null && registeredStudent.getId() > 0);
            assertTrue("A1b: Student skills saved and retrieved", registeredStudent.getSkills().contains("Java"));

            // 2. Duplicate username
            boolean dupUserCaught = false;
            try {
                authService.registerStudent(
                        testUsername, "AnotherPass", "other@example.com", "USN_DIFFERENT",
                        "Another Name", "9876543211", "IT", 7.5, 0, 2026, null
                );
            } catch (IllegalArgumentException e) {
                dupUserCaught = true;
            }
            assertTrue("A2: Duplicate username rejected", dupUserCaught);

            // 3. Duplicate USN
            boolean dupUsnCaught = false;
            try {
                authService.registerStudent(
                        "unique_user_999", "AnotherPass", "other2@example.com", testUsn,
                        "Another Name", "9876543211", "IT", 7.5, 0, 2026, null
                );
            } catch (IllegalArgumentException e) {
                dupUsnCaught = true;
            }
            assertTrue("A3: Duplicate student ID (USN) rejected", dupUsnCaught);

            // 4. Missing required fields
            boolean missingFieldCaught = false;
            try {
                authService.registerStudent(
                        "", testPassword, testEmail, "USN_X", "Name", "123", "CSE", 8.0, 0, 2026, null
                );
            } catch (IllegalArgumentException e) {
                missingFieldCaught = true;
            }
            assertTrue("A4: Blank username rejected", missingFieldCaught);

            // 5. Invalid email
            boolean invalidEmailCaught = false;
            try {
                authService.registerStudent(
                        "validuser_88", testPassword, "invalid-email-string", "USN_Y", "Name", "123", "CSE", 8.0, 0, 2026, null
                );
            } catch (IllegalArgumentException e) {
                invalidEmailCaught = true;
            }
            assertTrue("A5: Invalid email format rejected", invalidEmailCaught);

            // 6. Invalid CGPA (> 10.0 or < 0.0)
            boolean invalidCgpaCaught = false;
            try {
                authService.registerStudent(
                        "validuser_88", testPassword, "valid@test.com", "USN_Z", "Name", "123", "CSE", 11.5, 0, 2026, null
                );
            } catch (IllegalArgumentException e) {
                invalidCgpaCaught = true;
            }
            assertTrue("A6: Out-of-bounds CGPA (>10.0) rejected", invalidCgpaCaught);

            // 7. Negative backlogs
            boolean negBacklogsCaught = false;
            try {
                authService.registerStudent(
                        "validuser_88", testPassword, "valid@test.com", "USN_W", "Name", "123", "CSE", 7.0, -1, 2026, null
                );
            } catch (IllegalArgumentException e) {
                negBacklogsCaught = true;
            }
            assertTrue("A7: Negative backlogs rejected", negBacklogsCaught);

            // 8. Invalid graduation year
            boolean invalidYearCaught = false;
            try {
                authService.registerStudent(
                        "validuser_88", testPassword, "valid@test.com", "USN_V", "Name", "123", "CSE", 7.0, 0, 1995, null
                );
            } catch (IllegalArgumentException e) {
                invalidYearCaught = true;
            }
            assertTrue("A8: Unrealistic graduation year rejected", invalidYearCaught);

            // -----------------------------------------------------------------
            // TEST SUITE B: AUTHENTICATION & PASSWORD HASHING
            // -----------------------------------------------------------------
            System.out.println("\n--- TEST SUITE B: Authentication & Password Security ---");

            // 1. Correct credentials login
            User loggedInUser = authService.login(testUsername, testPassword);
            assertTrue("B1: Login succeeds with correct credentials", loggedInUser != null && loggedInUser.getUsername().equals(testUsername));
            assertTrue("B2: User role is STUDENT", loggedInUser.getRole() == Role.STUDENT);

            // 2. Incorrect password
            User wrongPassUser = authService.login(testUsername, "WrongPasswordHere");
            assertTrue("B3: Login fails with wrong password", wrongPassUser == null);

            // 3. Non-existent username
            User nonExistent = authService.login("ghost_user_9999", "AnyPassword");
            assertTrue("B4: Login fails with non-existent username", nonExistent == null);

            // 4. Blank username/password
            User blankLogin = authService.login("", "");
            assertTrue("B5: Blank credentials return null safely", blankLogin == null);

            // 5. Password hash format inspection (PBKDF2$10000$salt$hash)
            User fetchedUser = userDAO.findByUsername(testUsername);
            assertTrue("B6: Password is encrypted with PBKDF2 (not plaintext)",
                    fetchedUser.getPasswordHash().startsWith("PBKDF2$10000$"));

            // -----------------------------------------------------------------
            // TEST SUITE C: STUDENT DAO OPERATIONS
            // -----------------------------------------------------------------
            System.out.println("\n--- TEST SUITE C: StudentDAO Operations ---");

            Student studentById = studentDAO.findById(registeredStudent.getId());
            assertTrue("C1: Find student by ID", studentById != null && studentById.getFullName().equals("Raghav Bansal"));

            Student studentByUserId = studentDAO.findByUserId(registeredStudent.getUserId());
            assertTrue("C2: Find student by User ID", studentByUserId != null && studentByUserId.getStudentId().equals(testUsn));

            Student studentByUsn = studentDAO.findByStudentId(testUsn);
            assertTrue("C3: Find student by USN", studentByUsn != null && studentByUsn.getCgpa() == 8.85);

            studentById.setPhone("9123456780");
            studentById.setCgpa(9.10);
            boolean updated = studentDAO.updateStudent(studentById);
            Student reloadedStudent = studentDAO.findById(studentById.getId());
            assertTrue("C4: Update student profile details", updated && reloadedStudent.getCgpa() == 9.10);

            // -----------------------------------------------------------------
            // TEST SUITE D: COMPANY DAO OPERATIONS
            // -----------------------------------------------------------------
            System.out.println("\n--- TEST SUITE D: CompanyDAO Operations ---");

            List<Company> companies = companyDAO.findAllCompanies();
            assertTrue("D1: Retrieve all partner companies", companies.size() >= 3);

            Company c1 = companyDAO.findById(companies.get(0).getId());
            assertTrue("D2: Retrieve company by ID", c1 != null && c1.getName().equals(companies.get(0).getName()));

            Company cByName = companyDAO.findByName("Nexis Cloud Technologies");
            assertTrue("D3: Retrieve company by Name", cByName != null && cByName.getIndustry().equals("Cloud & DevOps"));

            // -----------------------------------------------------------------
            // TEST SUITE E: JOB DAO OPERATIONS
            // -----------------------------------------------------------------
            System.out.println("\n--- TEST SUITE E: JobDAO Operations ---");

            List<Job> allJobs = jobDAO.findAllJobs();
            assertTrue("E1: Retrieve all available jobs", allJobs.size() >= 3);

            Job j1 = jobDAO.findById(allJobs.get(0).getId());
            assertTrue("E2: Retrieve job by ID", j1 != null && j1.getTitle() != null);
            assertTrue("E3: Company name is populated on job", j1.getCompanyName() != null);
            assertTrue("E4: Eligible branches are populated on job", !j1.getEligibleBranches().isEmpty());
            assertTrue("E5: Required skills are populated on job", !j1.getRequiredSkills().isEmpty());

            List<Job> nexisJobs = jobDAO.findByCompanyId(cByName.getId());
            assertTrue("E6: Retrieve jobs by Company ID", !nexisJobs.isEmpty());

        } finally {
            // Cleanup test student and user
            cleanupUser(testUsername);
            System.out.println("\n[CLEANUP] Fictional test student account cleaned up from MySQL.");
        }

        // Summary
        System.out.println("\n==================================================");
        System.out.println("Test Results: " + testsPassed + " Passed, " + testsFailed + " Failed.");
        System.out.println("==================================================");

        if (testsFailed > 0) {
            System.exit(1);
        }
    }
}
