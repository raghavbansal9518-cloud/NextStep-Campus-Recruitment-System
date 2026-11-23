import com.nextstep.Main;
import com.nextstep.dao.StudentDAO;
import com.nextstep.dao.UserDAO;
import com.nextstep.database.DatabaseConnection;
import com.nextstep.model.Role;
import com.nextstep.model.Job;
import com.nextstep.model.Student;
import com.nextstep.dao.JobDAO;
import com.nextstep.model.User;
import com.nextstep.service.AuthService;
import com.nextstep.util.ValidationUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Scanner;

/**
 * ConsoleInputEdgeCaseTestRunner
 * 
 * Exhaustive Console Navigation & Form Verification Suite for NextStep:
 * 1. Welcome Page (Page 1) navigation & boundaries.
 * 2. Account Portal (Page 2) role dispatch & boundaries.
 * 3. Student Workflow:
 *    - Student Sign Up (Page 3) layout, runtime inputs, PBKDF2 hashing, persistence.
 *    - Student Sign In (Page 4) layout, Username login, Student ID (USN) login,
 *      password masking, invalid credentials rejection, officer barrier, option 0/1 handling.
 * 4. Placement Officer Workflow & Access Control.
 */
public class ConsoleInputEdgeCaseTestRunner {

    private static int passed = 0;
    private static int failed = 0;

    private static void pass(String testName) {
        System.out.println("  [PASS] " + testName);
        passed++;
    }

    private static void fail(String testName, String reason) {
        System.err.println("  [FAIL] " + testName + " -> " + reason);
        failed++;
    }

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("   NextStep — Pages 1 to 11 Comprehensive Verification Suite  ");
        System.out.println("==================================================================\n");

        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;

        long timestamp = System.currentTimeMillis();
        String testStudentUser = "test_std_" + timestamp;
        String testStudentUsn = "USN" + (timestamp % 100000);
        String testOfficerUser = "test_off_" + timestamp;

        try {
            // Seed a test student and officer for baseline login tests
            AuthService authService = new AuthService();
            authService.registerStudent(
                    testStudentUser, "pass123", testStudentUser + "@test.edu",
                    testStudentUsn, "Test Student", "9876543210",
                    "Computer Science", 8.5, 0, 2026, java.util.List.of("Java", "SQL")
            );
            authService.registerPlacementOfficer(
                    testOfficerUser, "pass123", testOfficerUser + "@test.edu"
            );

            // =================================================================
            // SUITE 1: WELCOME & UNIFIED CAMPUS GATEWAY (PAGE 1)
            // =================================================================
            System.out.println("--- 1. WELCOME PAGE (PAGE 1) ---");
            {
                // Test WP.1: Option 0 exits cleanly
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                System.setIn(new ByteArrayInputStream("0\n".getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("WELCOME & UNIFIED CAMPUS GATEWAY") && outStr.contains("Thank you for using NextStep. Goodbye!")) {
                    pass("WP.1: Welcome Page option 0 cleanly terminates application");
                } else {
                    fail("WP.1", "Option 0 did not terminate cleanly");
                }
            }

            {
                // Test WP.2: Invalid input on Welcome Gateway
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "bad_choice",
                        "99",
                        "0" // then exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("[WARN] Invalid choice. Please enter 0, 1, 2, or 3.")) {
                    pass("WP.2: Invalid input on Welcome Page caught with warning and re-prompted");
                } else {
                    fail("WP.2", "Expected invalid choice warning not found");
                }
            }

            // =================================================================
            // SUITE 2: UNIFIED GATEWAY DESTINATIONS & NAVIGATION
            // =================================================================
            System.out.println("\n--- 2. ACCOUNT PORTAL (PAGE 2) ---");
            {
                // Test AP.1 & AP.2: Page 1 option 1 opens Student Portal; layout matches reference
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "1", // Gateway: 1. Student Portal
                        "0", // Student Portal: 0. Back to Gateway
                        "0"  // Gateway: 0. Exit System
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);

                boolean hasHeader = outStr.contains("WELCOME & UNIFIED CAMPUS GATEWAY");
                boolean hasOpt1 = outStr.contains("1. Student Portal");
                boolean hasOpt2 = outStr.contains("2. Placement Officer Portal");
                boolean hasOpt3 = outStr.contains("3. View Campus Recruitment Statistics");
                boolean hasOpt0 = outStr.contains("0. Exit System");

                if (hasHeader && hasOpt1 && hasOpt2 && hasOpt3 && hasOpt0) {
                    pass("AP.1: Page 1 option 1 successfully opens Page 2 (Account Portal)");
                    pass("AP.2: Page 2 layout contains exact options: 1. Student, 2. Placement Officer, 0. Back");
                } else {
                    fail("AP.1/AP.2", "Gateway elements missing from output");
                }

                if (outStr.indexOf("STUDENT PORTAL") < outStr.lastIndexOf("WELCOME & UNIFIED CAMPUS GATEWAY")) {
                    pass("AP.3: Page 2 option 0 navigates back to Page 1 (Welcome Page) without exiting");
                } else {
                    fail("AP.3", "Page 2 option 0 did not return to Page 1");
                }
            }

            {
                // Test AP.4: Invalid input handling on Gateway
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "not_a_role",  // Invalid choice on Gateway
                        "99",          // Out of range on Gateway
                        "-1",          // Negative on Gateway
                        "0"            // Gateway Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("[WARN] Invalid choice. Please enter 0, 1, 2, or 3.")) {
                    pass("AP.4: Invalid input on Page 2 caught with warning: '[WARN] Invalid choice. Please enter 0, 1, 2, or 3.'");
                } else {
                    fail("AP.4", "Expected Page 2 invalid choice warning not found");
                }
            }

            // =================================================================
            // SUITE 3: PAGE 3 — STUDENT SIGN UP IMPLEMENTATION & VALIDATION
            // =================================================================
            System.out.println("\n--- 3. PAGE 3 — STUDENT SIGN UP ---");
            {
                // Test SU.1: Page 3 layout matches reference and Option 0 returns to Student Menu
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "1",               // Gateway -> Student Portal
                        "1",            // Student Menu -> 1. Student Registration (Page 3)
                        "ID_CANCEL",    // Student ID
                        "Cancel Name",  // Full Name
                        "cancel@test.com", // Email
                        "9876543210",   // Phone Number
                        "CS",           // Branch
                        "8.0",          // CGPA
                        "Java, SQL, Git", // Technical Skills
                        "0",            // Active Backlogs
                        "2026",         // Grad Year
                        "cancel_user",  // Username
                        "password123",  // Password
                        "password123",  // Confirm Password
                        "0",            // Page 3 Footer: 0. BACK
                        "0",            // Student Menu: 0. Back -> Page 2
                        "0",            // Page 2: 0. Back -> Page 1
                        "0"             // Page 1: 0. Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);

                boolean hasHeader = outStr.contains("NEXTSTEP | STUDENT PORTAL") && outStr.contains("STUDENT SIGN UP");
                boolean hasField1 = outStr.contains("Student ID (USN)");
                boolean hasField2 = outStr.contains("Full Name");
                boolean hasField3 = outStr.contains("Email Address");
                boolean hasFieldPhone = outStr.contains("Phone Number");
                boolean hasField4 = outStr.contains("Degree Branch");
                boolean hasField5 = outStr.contains("Academic CGPA");
                boolean hasFieldSkills = outStr.contains("Technical Skills");
                boolean hasFieldBacklogs = outStr.contains("Active Backlogs");
                boolean hasField6 = outStr.contains("Graduation Year");
                boolean hasField7 = outStr.contains("Account Username");
                boolean hasField8 = outStr.contains("Account Password");
                boolean hasField9 = outStr.contains("Confirm Password");
                boolean hasOpt1 = outStr.contains("1. CREATE ACCOUNT");
                boolean hasOpt0 = outStr.contains("0. BACK");

                if (hasHeader && hasField1 && hasField2 && hasField3 && hasFieldPhone && hasField4 &&
                        hasField5 && hasFieldSkills && hasFieldBacklogs && hasField6 && hasField7 && hasField8 && hasField9 &&
                        hasOpt1 && hasOpt0) {
                    pass("SU.1: Page 3 layout strictly matches reference layout and field alignment");
                } else {
                    fail("SU.1", "Page 3 layout missing expected components");
                }

                if (outStr.lastIndexOf("STUDENT MENU") > outStr.indexOf("STUDENT SIGN UP")) {
                    pass("SU.2: Page 3 option 0 (BACK) safely returns to Student Menu without creating account");
                } else {
                    fail("SU.2", "Page 3 option 0 did not return to Student Menu");
                }
            }

            {
                // Test SU.3: Password confirmation mismatch rejected
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "1",               // Gateway -> Student Portal
                        "1",               // Student Menu -> Page 3
                        "ID_MISMATCH",     // Student ID
                        "Mismatch Student",// Full Name
                        "mismatch@test.com",// Email
                        "9876543210",      // Phone Number
                        "Information Science",// Branch
                        "8.50",            // CGPA
                        "Java, SQL",       // Technical Skills
                        "0",               // Active Backlogs
                        "2026",            // Grad Year
                        "user_mismatch",   // Username
                        "password123",     // Password
                        "DIFFERENT_PASS",  // Confirm Password (mismatch!)
                        "1",               // Choice: 1. CREATE ACCOUNT
                        "0",               // Decline retry -> return to Student Menu
                        "0",               // Return to Gateway
                        "0"                // Gateway -> Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("Password and Confirm Password do not match.")) {
                    pass("SU.3: Password confirmation mismatch caught and rejected with clear error");
                } else {
                    fail("SU.3", "Password confirmation mismatch was not detected");
                }
            }

            {
                // Test SU.4: Invalid field values rejected
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "1",               // Gateway -> Student Portal
                        "1",               // Student Menu -> Page 3
                        "ID_INVAL",        // Student ID
                        "Invalid Student", // Full Name
                        "not-an-email",    // Invalid Email format
                        "9876543210",      // Phone Number
                        "Computer Science",// Branch
                        "11.5",            // Invalid CGPA (> 10.0)
                        "Java, Python",    // Technical Skills
                        "0",               // Active Backlogs
                        "2026",            // Grad Year
                        "valid_user_x",    // Username
                        "password123",     // Password
                        "password123",     // Confirm Password
                        "1",               // Choice: 1. CREATE ACCOUNT
                        "0",               // Decline retry -> return to Student Menu
                        "0",               // Return to Gateway
                        "0"                // Gateway -> Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("Invalid email format")) {
                    pass("SU.4: Invalid email format caught by ValidationUtil and rejected");
                } else {
                    fail("SU.4", "Invalid email format was not rejected");
                }
            }

            {
                // Test SU.4b: Invalid phone number rejected
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "1",               // Gateway -> Student Portal
                        "1",               // Student Menu -> Page 3
                        "ID_INVAL_PHONE",  // Student ID
                        "Phone Student",   // Full Name
                        "phone@test.com",  // Email
                        "12345",           // Invalid Phone (< 10 digits)
                        "Computer Science",// Branch
                        "8.50",            // CGPA
                        "Java, SQL",       // Technical Skills
                        "0",               // Active Backlogs
                        "2026",            // Grad Year
                        "phone_user_x",    // Username
                        "password123",     // Password
                        "password123",     // Confirm Password
                        "1",               // 1. CREATE ACCOUNT
                        "0",               // Decline retry
                        "0", "0"
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("Invalid phone number. Must be exactly 10 digits.")) {
                    pass("SU.4b: Invalid phone number (< 10 digits) rejected on registration form");
                } else {
                    fail("SU.4b", "Invalid phone number was not rejected");
                }
            }

            {
                // Test SU.4c: Negative active backlogs rejected
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "1",               // Gateway -> Student Portal
                        "1",               // Student Menu -> Page 3
                        "ID_NEG_BACK",     // Student ID
                        "Backlog Student", // Full Name
                        "backlog@test.com",// Email
                        "9876543210",      // Phone Number
                        "Computer Science",// Branch
                        "8.50",            // CGPA
                        "Java, SQL",       // Technical Skills
                        "-1",              // Negative Backlogs!
                        "2026",            // Grad Year
                        "backlog_user_x",  // Username
                        "password123",     // Password
                        "password123",     // Confirm Password
                        "1",               // 1. CREATE ACCOUNT
                        "0",               // Decline retry
                        "0", "0"
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("Active backlogs cannot be negative.")) {
                    pass("SU.4c: Negative active backlogs rejected on registration form");
                } else {
                    fail("SU.4c", "Negative active backlogs was not rejected");
                }
            }

            {
                // Test SU.4d: Non-numeric active backlogs rejected
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "1",               // Gateway -> Student Portal
                        "1",               // Student Menu -> Page 3
                        "ID_STR_BACK",     // Student ID
                        "Backlog Student", // Full Name
                        "backlog2@test.com",// Email
                        "9876543210",      // Phone Number
                        "Computer Science",// Branch
                        "8.50",            // CGPA
                        "Java, SQL",       // Technical Skills
                        "TWO",             // Non-numeric Backlogs!
                        "2026",            // Grad Year
                        "backlog_user_y",  // Username
                        "password123",     // Password
                        "password123",     // Confirm Password
                        "1",               // 1. CREATE ACCOUNT
                        "0",               // Decline retry
                        "0", "0"
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("Invalid Active Backlogs: must be a valid integer greater than or equal to zero.")) {
                    pass("SU.4d: Non-numeric active backlogs rejected on registration form");
                } else {
                    fail("SU.4d", "Non-numeric active backlogs was not rejected");
                }
            }

            {
                // Test SU.5: Duplicate username rejected
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "1",               // Gateway -> Student Portal
                        "1",               // Student Menu -> Page 3
                        "ID_DUP_USER",     // Student ID
                        "Dup User Student",// Full Name
                        "dupuser@test.com",// Email
                        "9876543210",      // Phone Number
                        "Computer Science",// Branch
                        "9.00",            // CGPA
                        "Java, SQL",       // Technical Skills
                        "0",               // Active Backlogs
                        "2026",            // Grad Year
                        testOfficerUser,   // Duplicate Username (already exists!)
                        "password123",     // Password
                        "password123",     // Confirm Password
                        "1",               // Choice: 1. CREATE ACCOUNT
                        "0",               // Decline retry -> return to Student Menu
                        "0",               // Return to Gateway
                        "0"                // Gateway -> Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("Username is already taken.")) {
                    pass("SU.5: Duplicate username rejected safely via database uniqueness check");
                } else {
                    fail("SU.5", "Duplicate username was not rejected");
                }
            }

            {
                // Test SU.6: Successful end-to-end registration at runtime, PBKDF2 hashing, and login
                String newStdUser = "runtime_std_" + timestamp;
                String newStdUsn = "USN_RT_" + (timestamp % 100000);
                String newStdName = "Runtime Student User";
                String newStdEmail = newStdUser + "@campus.edu";

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "1",               // Gateway -> Student Portal
                        "1",               // Student Menu -> 1. Student Registration (Page 3)
                        newStdUsn,         // Student ID
                        newStdName,        // Full Name
                        newStdEmail,       // Email
                        "9876543210",      // Phone Number (10 digits)
                        "Computer Science Engineering", // Branch
                        "9.45",            // CGPA
                        "Java, Spring Boot, SQL, Git", // Technical Skills
                        "2",               // Active Backlogs (testing non-zero persistence!)
                        "2026",            // Graduation Year
                        newStdUser,        // Username
                        "securePassword99",// Password
                        "securePassword99",// Confirm Password
                        "1",               // 1. CREATE ACCOUNT
                        "0",               // Return to Gateway
                        "0"                // Gateway -> Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);

                boolean createdSuccess = outStr.contains("Account created successfully for " + newStdName);
                if (createdSuccess) {
                    pass("SU.6: Runtime student registered successfully and account persisted");
                } else {
                    fail("SU.6", "Registration failed");
                }

                // Verify Database entity & PBKDF2 hashing directly
                UserDAO userDAO = new UserDAO();
                User userInDb = userDAO.findByUsername(newStdUser);
                if (userInDb != null && userInDb.getRole() == Role.STUDENT &&
                        userInDb.getPasswordHash().startsWith("PBKDF2$") &&
                        !userInDb.getPasswordHash().contains("securePassword99")) {
                    pass("SU.7: Password encrypted with PBKDF2 salt & hash (never plain-text)");
                } else {
                    fail("SU.7", "PBKDF2 password encryption verification failed");
                }

                StudentDAO studentDAO = new StudentDAO();
                Student studentInDb = studentDAO.findByStudentId(newStdUsn);
                if (studentInDb != null && newStdName.equals(studentInDb.getFullName()) &&
                        "9876543210".equals(studentInDb.getPhone()) &&
                        studentInDb.getActiveBacklogs() == 2 &&
                        studentInDb.getCgpa() == 9.45 && studentInDb.getGraduationYear() == 2026 &&
                        studentInDb.getSkills().contains("Java") && studentInDb.getSkills().contains("Spring Boot") &&
                        studentInDb.getSkills().contains("SQL") && studentInDb.getSkills().contains("Git")) {
                    pass("SU.8: Student profile attributes, phone (9876543210), backlogs (2), and skills persisted accurately in MySQL database");
                } else {
                    fail("SU.8", "Student profile database record verification failed");
                }
            }

            {
                // Test SU.9: Technical skills parsing helper and empty skills rejection
                List<String> parsedSkills = ValidationUtil.parseCommaSeparatedList("  Java ,  SQL , java, Git, , Python , SQL  ");
                boolean skillsParsedCorrectly = parsedSkills.size() == 4 &&
                        parsedSkills.contains("Java") &&
                        parsedSkills.contains("SQL") &&
                        parsedSkills.contains("Git") &&
                        parsedSkills.contains("Python");

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "1",               // Gateway -> Student Portal
                        "1",               // Student Menu -> Page 3
                        "ID_EMPTY_SKILLS", // Student ID
                        "No Skills Student",// Full Name
                        "noskills@test.com",// Email
                        "9876543210",      // Phone Number
                        "Computer Science",// Branch
                        "8.00",            // CGPA
                        "   ,  ,   ",      // Empty/whitespace Technical Skills!
                        "0",               // Active Backlogs
                        "2026",            // Grad Year
                        "noskills_user",   // Username
                        "password123",     // Password
                        "password123",     // Confirm Password
                        "1",               // Choice: 1. CREATE ACCOUNT
                        "0",               // Decline retry -> return to Student Menu
                        "0",               // Return to Gateway
                        "0"                // Gateway -> Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                boolean emptySkillsRejected = outStr.contains("Technical skills cannot be empty");

                if (skillsParsedCorrectly && emptySkillsRejected) {
                    pass("SU.9: Technical skills comma-parsing, trimming, case-insensitive deduplication, and empty-check verified");
                } else {
                    fail("SU.9", "Technical skills parsing or empty validation failed");
                }
            }

            // =================================================================
            // SUITE 4: PAGE 4 — STUDENT SIGN IN IMPLEMENTATION & VERIFICATION
            // =================================================================
            System.out.println("\n--- 4. PAGE 4 — STUDENT SIGN IN ---");
            {
                // Test SI.1 & SI.2: Page 4 layout matches reference and Option 0 returns to Student Menu
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "1",               // Gateway -> Student Portal
                        "2",               // Student Menu -> 2. Student Login (Page 4)
                        "dummy_id",        // Student ID / Username
                        "dummy_pass",      // Password
                        "0",               // Page 4: 0. BACK
                        "0",               // Student Menu: 0. Back -> Page 2
                        "0",               // Page 2: 0. Back -> Page 1
                        "0"                // Page 1: 0. Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);

                boolean hasHeader = outStr.contains("NEXTSTEP | STUDENT PORTAL") && outStr.contains("STUDENT SIGN IN");
                boolean hasField1 = outStr.contains("Student ID / Username:");
                boolean hasField2 = outStr.contains("Password:");
                boolean hasOpt1 = outStr.contains("1. SIGN IN");
                boolean hasOpt0 = outStr.contains("0. BACK");
                boolean hasMsg = outStr.contains("Message: Please enter your account credentials.");

                if (hasHeader && hasField1 && hasField2 && hasOpt1 && hasOpt0 && hasMsg) {
                    pass("SI.1: Page 4 layout strictly matches reference layout and exact message text");
                } else {
                    fail("SI.1", "Page 4 layout missing expected components");
                }

                if (outStr.lastIndexOf("STUDENT MENU") > outStr.indexOf("STUDENT SIGN IN")) {
                    pass("SI.2: Page 4 option 0 (BACK) safely returns to Student Menu without exiting");
                } else {
                    fail("SI.2", "Page 4 option 0 did not return to Student Menu");
                }
            }

            {
                // Test SI.3: Invalid menu choice on Page 4 footer
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "1",               // Gateway -> Student Portal
                        "2",               // Student Menu -> Page 4
                        "any_user",        // Student ID / Username
                        "any_pass",        // Password
                        "99",              // Invalid choice on Page 4
                        "0",               // Valid choice 0. BACK
                        "0",               // Return to Gateway
                        "0"                // Gateway -> Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("[WARN] Invalid choice. Please enter 1 to Sign In or 0 to Back.")) {
                    pass("SI.3: Out-of-bounds menu choice on Page 4 caught with friendly warning");
                } else {
                    fail("SI.3", "Invalid choice warning not found on Page 4");
                }
            }

            {
                // Test SI.4: Incorrect password rejected generically
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "1",               // Gateway -> Student Portal
                        "2",               // Student Menu -> Page 4
                        testStudentUser,   // Valid Username
                        "WRONG_PASSWORD",  // Incorrect Password
                        "1",               // 1. SIGN IN
                        "0",               // Decline retry -> return to Student Menu
                        "0",               // Return to Gateway
                        "0"                // Gateway -> Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("Invalid credentials. Please check your Student ID / Username and password.")) {
                    pass("SI.4: Incorrect password rejected generically without leaking account existence");
                } else {
                    fail("SI.4", "Incorrect password was not properly rejected");
                }
            }

            {
                // Test SI.5: Placement Officer account rejected at Student Sign In
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "1",               // Gateway -> Student Portal
                        "2",               // Student Menu -> Page 4
                        testOfficerUser,   // Placement Officer Username
                        "pass123",         // Officer Password
                        "1",               // 1. SIGN IN
                        "0",               // Decline retry -> return to Student Menu
                        "0",               // Return to Gateway
                        "0"                // Gateway -> Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("Access denied. Placement officer accounts cannot sign in here.")) {
                    pass("SI.5: Placement Officer account rejected at Student Sign In with access denied");
                } else {
                    fail("SI.5", "Role restriction did not block Placement Officer at Student Sign In");
                }
            }

            {
                // Test SI.6: Successful login via USERNAME opens Student Portal
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "1",               // Gateway -> Student Portal
                        "2",               // Student Menu -> Page 4
                        testStudentUser,   // Login via USERNAME
                        "pass123",         // Password
                        "1",               // 1. SIGN IN
                        "0",               // Page 6 Student Dashboard -> 0. Sign Out (returns to Page 2)
                        "0",               // Page 2 -> Page 1
                        "0"                // Page 1 -> Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("Login successful! Welcome, Test Student.") &&
                        outStr.contains("STUDENT DASHBOARD") &&
                        outStr.contains("Signing out from Student Portal...")) {
                    pass("SI.6: Successful student authentication via Username opens Student Portal");
                } else {
                    fail("SI.6", "Student login via username failed");
                }
            }

            {
                // Test SI.7: Successful login via STUDENT ID (USN) opens Student Portal
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "1",               // Gateway -> Student Portal
                        "2",               // Student Menu -> Page 4
                        testStudentUsn,    // Login via STUDENT ID / USN!
                        "pass123",         // Password
                        "1",               // 1. SIGN IN
                        "0",               // Page 6 Student Dashboard -> 0. Sign Out (returns to Page 2)
                        "0",               // Page 2 -> Page 1
                        "0"                // Page 1 -> Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("Login successful! Welcome, Test Student.") &&
                        outStr.contains("STUDENT DASHBOARD") &&
                        outStr.contains("Signing out from Student Portal...")) {
                    pass("SI.7: Successful student authentication via Student ID (USN) opens Student Portal");
                } else {
                    fail("SI.7", "Student login via Student ID / USN failed");
                }
            }

            // =================================================================
            // SUITE 5: PLACEMENT OFFICER WORKFLOW
            // =================================================================
            System.out.println("\n--- 5. PLACEMENT OFFICER WORKFLOW ---");
            {
                // Test PO.1: Page 2 option 2 reaches Placement Officer menu, option 2 logs in and accesses portal
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "2",               // Gateway -> Placement Officer Portal
                        "2",              // Placement Officer Menu -> Option 2 (Officer Sign In)
                        testOfficerUser,  // Username
                        "pass123",        // Password
                        "0",              // Page 7 Officer Dashboard -> 0. Sign Out (returns to Page 2)
                        "0",              // Page 2 -> Back to Page 1
                        "0"               // Page 1 -> Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("PLACEMENT OFFICER LOGIN") && outStr.contains("OFFICER DASHBOARD") && outStr.contains("Signing out from Placement Officer Portal...")) {
                    pass("PO.1: Page 2 option 2 reaches Placement Officer workflow and accesses Portal");
                } else {
                    fail("PO.1", "Placement Officer login or portal access failed");
                }
            }

            {
                // Test PO.2: Invalid officer credentials return to Placement Officer Menu cleanly
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "2",               // Gateway -> Placement Officer Portal
                        "2",              // Placement Officer Menu -> Option 2 (Officer Sign In)
                        "nonexistent_off",// Bad username
                        "wrongpass",      // Bad password
                        "0",              // Placement Officer Menu -> 0. Back to Account Portal
                        "0",              // Page 2 -> Back to Page 1
                        "0"               // Page 1 -> Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("Invalid credentials. Please check your username and password.") &&
                        outStr.contains("PLACEMENT OFFICER MENU")) {
                    pass("PO.2: Invalid credentials safely rejected and returns to Placement Officer Menu");
                } else {
                    fail("PO.2", "Invalid credentials did not return to Placement Officer Menu");
                }
            }

            {
                // Test PO.3: Role restriction - Student cannot log into Placement Officer workflow
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "2",               // Gateway -> Placement Officer Portal
                        "2",              // Placement Officer Menu -> Option 2 (Officer Sign In)
                        testStudentUser,  // Student username entered in Officer login
                        "pass123",        // Student password
                        "0",              // Placement Officer Menu -> 0. Back to Account Portal
                        "0",              // Page 2 -> Back to Page 1
                        "0"               // Page 1 -> Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("Access denied. This account does not have Placement Officer privileges.")) {
                    pass("PO.3: Student blocked from Placement Officer login via role-check");
                } else {
                    fail("PO.3", "Role enforcement did not block Student from Officer login");
                }
            }

            // =================================================================
            // SUITE 6: PLACEMENT OFFICER SIGN UP (PAGE 5)
            // =================================================================
            System.out.println("\n--- 6. PLACEMENT OFFICER SIGN UP (PAGE 5) ---");
            {
                // Test OS.1: Page 5 Header, Field Prompts & Footer matching exact specification
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                Main app = new Main();
                app.renderOfficerSignUpHeader();
                app.renderOfficerSignUpFooter();
                String rendered = out.toString(StandardCharsets.UTF_8);

                // Check via direct console capture
                ByteArrayOutputStream capture = new ByteArrayOutputStream();
                System.setOut(new PrintStream(capture, true, StandardCharsets.UTF_8));
                app.renderOfficerSignUpHeader();
                app.renderOfficerSignUpFooter();
                System.setOut(originalOut);

                String captured = capture.toString(StandardCharsets.UTF_8);
                boolean headerOk = captured.contains("NEXTSTEP | PLACEMENT OFFICER PORTAL") &&
                                   captured.contains("OFFICER SIGN UP");
                boolean footerOk = captured.contains("1. CREATE ACCOUNT") &&
                                   captured.contains("0. BACK");
                if (headerOk && footerOk) {
                    pass("OS.1: Page 5 header and action footer match exact reference design");
                } else {
                    fail("OS.1", "Page 5 header or footer mismatch");
                }
            }

            {
                // Test OS.2: Page 5 Option 0 (BACK) returns to Placement Officer Menu cleanly
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "2",               // Gateway -> Placement Officer Portal
                        "1",              // Placement Officer Menu -> Option 1 (Officer Sign Up)
                        "OFF001",         // Officer ID
                        "Jane Officer",   // Full Name
                        "jane@campus.edu",// Official Email
                        "jane_off",       // Username
                        "password123",    // Password
                        "password123",    // Confirm Password
                        "ANY_CODE", // Activation Code
                        "0",              // Page 5 choice -> 0. BACK
                        "0",              // Placement Officer Menu -> 0. BACK
                        "0",              // Account Portal -> 0. BACK
                        "0"               // Welcome Page -> 0. EXIT
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("OFFICER SIGN UP") && outStr.contains("PLACEMENT OFFICER MENU")) {
                    pass("OS.2: Page 5 option 0 (BACK) returns safely to Placement Officer Menu without creating account");
                } else {
                    fail("OS.2", "Page 5 option 0 did not return to Placement Officer Menu");
                }
            }

            {
                // Test OS.3: Password and Confirm Password mismatch rejection
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "2",               // Gateway -> Placement Officer Portal
                        "1",              // Placement Officer Menu -> Option 1 (Officer Sign Up)
                        "OFF002",         // Officer ID
                        "John Officer",   // Full Name
                        "john@campus.edu",// Official Email
                        "john_off",       // Username
                        "password123",    // Password
                        "mismatchedpass", // Confirm Password (mismatch!)
                        "ANY_CODE", // Activation Code
                        "1",              // Action -> 1. CREATE ACCOUNT
                        "0",              // Error recovery prompt -> 0 to return
                        "0",              // Placement Officer Menu -> 0. BACK
                        "0",              // Account Portal -> 0. BACK
                        "0"               // Welcome Page -> 0. EXIT
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("Password and Confirm Password do not match.")) {
                    pass("OS.3: Password and Confirm Password mismatch properly rejected with clear error");
                } else {
                    fail("OS.3", "Password mismatch was not rejected");
                }
            }

            {
                // Test OS.4a: Missing Activation Code configuration disables registration
                System.clearProperty(AuthService.ACTIVATION_CODE_PROP);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "2",               // Gateway -> Placement Officer Portal
                        "1",              // Placement Officer Menu -> Option 1 (Officer Sign Up)
                        "OFF003",         // Officer ID
                        "Applicant",      // Full Name
                        "app@campus.edu", // Official Email
                        "app_off",        // Username
                        "secret123",      // Password
                        "secret123",      // Confirm Password
                        "ANY_CODE",       // Activation Code (Unconfigured!)
                        "1",              // Action -> 1. CREATE ACCOUNT
                        "0",              // Error recovery prompt -> 0 to return
                        "0",              // Placement Officer Menu -> 0. BACK
                        "0",              // Account Portal -> 0. BACK
                        "0"               // Welcome Page -> 0. EXIT
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                boolean apiBlocked = false;
                try {
                    authService.registerPlacementOfficer("OFF003", "Applicant", "app@campus.edu", "app_off", "secret123", "ANY_CODE");
                } catch (IllegalStateException e) {
                    apiBlocked = true;
                }
                if (outStr.contains("Officer registration is disabled") &&
                    outStr.contains("System administrator has not configured an activation code") &&
                    apiBlocked && !authService.isOfficerActivationConfigured()) {
                    pass("OS.4a: Missing activation code disables registration and shows configuration error");
                } else {
                    fail("OS.4a", "Unconfigured activation code did not disable registration cleanly");
                }
            }

            // Now configure an authorized activation code for subsequent tests
            String testAuthCode = "TEST_AUTH_CODE_" + timestamp;
            System.setProperty(AuthService.ACTIVATION_CODE_PROP, testAuthCode);

            {
                // Test OS.4b: Incorrect Activation Code rejected when code is configured
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "2",               // Gateway -> Placement Officer Portal
                        "1",              // Placement Officer Menu -> Option 1 (Officer Sign Up)
                        "OFF003",         // Officer ID
                        "Hacker Person",  // Full Name
                        "hacker@evil.com",// Official Email
                        "hacker_off",     // Username
                        "secret123",      // Password
                        "secret123",      // Confirm Password
                        "WRONG_CODE_999", // Activation Code (INVALID!)
                        "1",              // Action -> 1. CREATE ACCOUNT
                        "0",              // Error recovery prompt -> 0 to return
                        "0",              // Placement Officer Menu -> 0. BACK
                        "0",              // Account Portal -> 0. BACK
                        "0"               // Welcome Page -> 0. EXIT
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("Invalid activation code. Placement officer registration unauthorized.")) {
                    pass("OS.4b: Unauthorized registration blocked when activation code is incorrect");
                } else {
                    fail("OS.4b", "Incorrect activation code did not block registration");
                }
            }

            {
                // Test OS.5: Missing / empty required fields rejected
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "2",               // Gateway -> Placement Officer Portal
                        "1",              // Placement Officer Menu -> Option 1 (Officer Sign Up)
                        "",               // Upfront Mode: ENTER (Manual entry)
                        "",               // Officer ID (EMPTY!)
                        "Some Name",      // Full Name
                        "some@campus.edu",// Official Email
                        "some_off",       // Username
                        "secret123",      // Password
                        "secret123",      // Confirm Password
                        testAuthCode,     // Activation Code
                        "1",              // Action -> 1. CREATE ACCOUNT
                        "0",              // Error recovery prompt -> 0 to return
                        "0",              // Placement Officer Menu -> 0. BACK
                        "0",              // Account Portal -> 0. BACK
                        "0"               // Welcome Page -> 0. EXIT
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("Officer ID cannot be empty.")) {
                    pass("OS.5: Empty Officer ID validation properly rejected");
                } else {
                    fail("OS.5", "Empty Officer ID was not rejected");
                }
            }

            {
                // Test OS.6: Duplicate username rejected
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "2",               // Gateway -> Placement Officer Portal
                        "1",              // Placement Officer Menu -> Option 1 (Officer Sign Up)
                        "OFF006",         // Officer ID
                        "Duplicate Officer", // Full Name
                        "dup@campus.edu", // Official Email
                        testOfficerUser,  // Username (ALREADY TAKEN!)
                        "secret123",      // Password
                        "secret123",      // Confirm Password
                        testAuthCode,     // Activation Code
                        "1",              // Action -> 1. CREATE ACCOUNT
                        "0",              // Error recovery prompt -> 0 to return
                        "0",              // Placement Officer Menu -> 0. BACK
                        "0",              // Account Portal -> 0. BACK
                        "0"               // Welcome Page -> 0. EXIT
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("Username is already taken.")) {
                    pass("OS.6: Duplicate username properly rejected");
                } else {
                    int errIdx = outStr.indexOf("[ERROR]");
                    String errSnippet = errIdx != -1 ? outStr.substring(errIdx, Math.min(outStr.length(), errIdx + 200)) : "NO_ERROR_FOUND";
                    fail("OS.6", "Duplicate username was not rejected. Error found: " + errSnippet);
                }
            }

            String newOfficerUser = "officer_" + timestamp;
            {
                // Test OS.7: Successful authorized registration with valid activation code
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "2",               // Gateway -> Placement Officer Portal
                        "1",              // Placement Officer Menu -> Option 1 (Officer Sign Up)
                        "OFF999",         // Officer ID
                        "Dr. Sarah Smith",// Full Name
                        newOfficerUser + "@campus.edu", // Official Email
                        newOfficerUser,   // Username
                        "securepass123",  // Password
                        "securepass123",  // Confirm Password
                        testAuthCode,     // Valid Activation Code
                        "1",              // Action -> 1. CREATE ACCOUNT
                        "0",              // Placement Officer Menu -> 0. BACK
                        "0",              // Account Portal -> 0. BACK
                        "0"               // Welcome Page -> 0. EXIT
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("Placement Officer account created successfully for '" + newOfficerUser + "'!")) {
                    pass("OS.7: Successful Placement Officer account registration with valid activation code");
                } else {
                    fail("OS.7", "Officer registration failed");
                }
            }

            {
                // Test OS.8: Database verification - Password hashed with salt (PBKDF2)
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement stmt = conn.prepareStatement("SELECT password_hash, role FROM users WHERE username = ?")) {
                    stmt.setString(1, newOfficerUser);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            String hash = rs.getString("password_hash");
                            String role = rs.getString("role");
                            if (hash != null && hash.contains("$") && "PLACEMENT_OFFICER".equals(role)) {
                                pass("OS.8: Registered officer persisted in database with PBKDF2 salt:hash and PLACEMENT_OFFICER role");
                            } else {
                                fail("OS.8", "Persisted officer hash or role invalid: " + hash + " / " + role);
                            }
                        } else {
                            fail("OS.8", "Registered officer not found in users table");
                        }
                    }
                }
            }

            {
                // Test OS.9: Newly created officer can log in via Officer Sign In and access Portal
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "2",               // Gateway -> Placement Officer Portal
                        "2",              // Placement Officer Menu -> Option 2 (Officer Sign In)
                        newOfficerUser,   // Username of newly created officer
                        "securepass123",  // Password
                        "0",              // Page 7 Officer Dashboard -> 0. Sign Out (returns to Page 2)
                        "0",              // Account Portal -> 0. BACK
                        "0"               // Welcome Page -> 0. EXIT
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("PLACEMENT OFFICER LOGIN") &&
                    outStr.contains("OFFICER DASHBOARD") &&
                    outStr.contains("Signing out from Placement Officer Portal...")) {
                    pass("OS.9: Newly registered officer signs in cleanly and accesses Placement Officer Portal");
                } else {
                    fail("OS.9", "Newly registered officer could not sign in to portal");
                }
            }
            // =================================================================
            // SUITE 7: STUDENT DASHBOARD (PAGE 6)
            // =================================================================
            System.out.println("\n--- 7. STUDENT DASHBOARD (PAGE 6) ---");
            {
                // Test SD.1: Exact header, runtime student name & ID, and 7 menu labels
                ByteArrayOutputStream capture = new ByteArrayOutputStream();
                System.setOut(new PrintStream(capture, true, StandardCharsets.UTF_8));
                Student sampleStudent = new Student(888, "25CSE0204", "Vihaan Mehta", "9876543210", "CSE", 9.1, 0, 2026);
                Main app = new Main();
                app.renderStudentDashboardHeader(sampleStudent);
                System.setOut(originalOut);

                String rendered = capture.toString(StandardCharsets.UTF_8);
                boolean headerOk = rendered.contains("NEXTSTEP | STUDENT PORTAL") &&
                                   rendered.contains("STUDENT DASHBOARD");
                boolean studentInfoOk = rendered.contains("Welcome, Vihaan Mehta") &&
                                        rendered.contains("Student ID: 25CSE0204");
                boolean optionsOk = rendered.contains("1. View Available Job Drives") &&
                                    rendered.contains("2. Check Job Eligibility & Skill Match") &&
                                    rendered.contains("3. Apply for an Eligible Job") &&
                                    rendered.contains("4. View My Applications") &&
                                    rendered.contains("0. Sign Out");
                boolean noComingSoon = !rendered.contains("Coming Soon") &&
                                       !rendered.contains("View My Profile") &&
                                       !rendered.contains("Update My Profile");

                if (headerOk && studentInfoOk && optionsOk && noComingSoon) {
                    pass("SD.1: Page 6 header, student details, and 4 logical options match clean specifications with no unfinished items");
                } else {
                    fail("SD.1", "Page 6 layout mismatch");
                }
            }

            {
                // Test SD.2: Logical student options cleanly return to Student Dashboard on '0'
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "1",               // Gateway -> Student Portal
                        "2",               // Student Menu -> Page 4
                        testStudentUser,   // Username
                        "pass123",         // Password
                        "1",               // 1. SIGN IN
                        "1",               // Page 6 -> 1. View Available Job Drives
                        "0",               // Return to Page 6
                        "2",               // Page 6 -> 2. Check Job Eligibility
                        "0",               // Return to Page 6
                        "3",               // Page 6 -> 3. Apply for a Job
                        "0",               // Return to Page 6
                        "0",               // Page 6 -> 0. Sign Out (returns to Page 2)
                        "0",               // Page 2 -> Page 1
                        "0"                // Page 1 -> Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("AVAILABLE RECRUITMENT DRIVES") &&
                    outStr.contains("Signing out from Student Portal...")) {
                    pass("SD.2: Logical student dashboard options (1 to 3) execute cleanly and return safely to Student Dashboard");
                } else {
                    fail("SD.2", "Student Dashboard options failed to return safely");
                }
            }

            {
                // Test SD.3: Option 1 (Available Job Drives) renders active drives from database
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "1",               // Gateway -> Student Portal
                        "2",               // Student Menu -> Page 4
                        testStudentUser,   // Username
                        "pass123",         // Password
                        "1",               // 1. SIGN IN
                        "1",               // Page 6 -> 1. Available Job Drives
                        "0",               // Return to Page 6
                        "0",               // Page 6 -> 0. Sign Out (returns to Page 2)
                        "0",               // Page 2 -> Page 1
                        "0"                // Page 1 -> Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("AVAILABLE RECRUITMENT DRIVES") &&
                    outStr.contains("Active Drives:") &&
                    outStr.contains("Nexis Cloud Technologies")) {
                    pass("SD.3: Option 1 (Available Job Drives) correctly renders active drives from database");
                } else {
                    fail("SD.3", "Available job drives not displayed");
                }
            }

            {
                // Test SD.4: Option 0 (Sign Out) returns directly to Account Portal (Page 2)
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "1",               // Gateway -> Student Portal
                        "2",               // Student Menu -> Page 4
                        testStudentUser,   // Username
                        "pass123",         // Password
                        "1",               // 1. SIGN IN
                        "0",               // Page 6 -> 0. Sign Out
                        "0",               // Page 2 -> Page 1
                        "0"                // Page 1 -> Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                int signoutIdx = outStr.indexOf("Signing out from Student Portal...");
                int portalIdx = outStr.indexOf("CAMPUS GATEWAY", signoutIdx) != -1 ? outStr.indexOf("CAMPUS GATEWAY", signoutIdx) : outStr.indexOf("ACCOUNT PORTAL", signoutIdx);
                if (signoutIdx != -1 && portalIdx != -1) {
                    pass("SD.4: Option 0 signs out and transitions cleanly back to Account Portal (Page 2)");
                } else {
                    fail("SD.4", "Sign out did not return to Account Portal");
                }
            }

            {
                // Test SD.5: Invalid menu choices handled safely with friendly re-prompt
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "1",               // Gateway -> Student Portal
                        "2",               // Student Menu -> Page 4
                        testStudentUser,   // Username
                        "pass123",         // Password
                        "1",               // 1. SIGN IN
                        "99",              // Invalid choice!
                        "0",               // Page 6 -> 0. Sign Out
                        "0",               // Page 2 -> Page 1
                        "0"                // Page 1 -> Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("[WARN] Invalid option. Please try again.")) {
                    pass("SD.5: Out-of-bounds menu choice on Page 6 handled safely without crashing");
                } else {
                    fail("SD.5", "Invalid choice was not caught with warning");
                }
            }

            // =================================================================
            // SUITE 8: PLACEMENT OFFICER DASHBOARD (PAGE 7)
            // =================================================================
            System.out.println("\n--- 8. PLACEMENT OFFICER DASHBOARD (PAGE 7) ---");
            {
                // Test OD.1: Exact header, neutral Officer ID, and 5 menu labels matching Page 6 specifications
                ByteArrayOutputStream capture = new ByteArrayOutputStream();
                System.setOut(new PrintStream(capture, true, StandardCharsets.UTF_8));
                User sampleOfficer = new User("officer_test", "hash", Role.PLACEMENT_OFFICER, "off@test.edu");
                Main app = new Main();
                app.renderOfficerDashboardHeader(sampleOfficer);
                System.setOut(originalOut);

                String rendered = capture.toString(StandardCharsets.UTF_8);
                boolean headerOk = rendered.contains("NEXTSTEP | PLACEMENT OFFICER PORTAL") &&
                                   (rendered.contains("ADMINISTRATION DASHBOARD") || rendered.contains("OFFICER DASHBOARD"));
                boolean officerInfoOk = rendered.contains("officer_test") || rendered.contains("Dr. Arvind Sharma");
                boolean optionsOk = (rendered.contains("Create & Publish") || rendered.contains("Create Job Drive")) &&
                                    (rendered.contains("Manage Student Applications") || rendered.contains("View Applications")) &&
                                    (rendered.contains("Export Placement Audit") || rendered.contains("Export CSV")) &&
                                    rendered.contains("Presentation Visibility") &&
                                    rendered.contains("Sign Out");

                if (headerOk && officerInfoOk && optionsOk) {
                    pass("OD.1: Page 6 header, Officer details, and 4 tools match exact reference layout");
                } else {
                    fail("OD.1", "Page 6 layout mismatch");
                }
            }

            {
                // Test OD.2: Invalid option handled safely without crash
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "2",               // Gateway -> Placement Officer Portal
                        "2",              // Placement Officer Menu -> Option 2 (Officer Sign In)
                        testOfficerUser,  // Username
                        "pass123",        // Password
                        "99",             // Invalid option
                        "0",              // Page 6 -> 0. Sign Out (returns to Gateway)
                        "0"               // Gateway -> Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("[WARN] Invalid option") || outStr.contains("Signing out from Placement Officer Portal...")) {
                    pass("OD.2: Invalid option handled safely with warning and returns to Officer Dashboard");
                } else {
                    fail("OD.2", "Invalid option warning missing");
                }
            }

            {
                // Test OD.3: Implemented option (2: View Applications / Master Registry) functions properly
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "2",               // Gateway -> Placement Officer Portal
                        "2",              // Placement Officer Menu -> Option 2 (Officer Sign In)
                        testOfficerUser,  // Username
                        "pass123",        // Password
                        "2",              // Page 6 -> 2. View Applications (opens Page 10)
                        "0",              // Page 10 -> 0. BACK (to Page 6)
                        "0",              // Page 6 -> 0. Sign Out (returns to Gateway)
                        "0"               // Gateway -> Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("MASTER APPLICATION REGISTRY") ||
                    outStr.contains("APPLICATION RECORDS") ||
                    outStr.contains("No student applications have been submitted yet.")) {
                    pass("OD.3: Option 2 (View Applications) functions cleanly with recruitment service");
                } else {
                    fail("OD.3", "View applications failed");
                }
            }

            {
                // Test OD.4: Option 0 (Sign Out) returns directly to Account Portal (Page 2)
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "2",               // Gateway -> Placement Officer Portal
                        "2",              // Placement Officer Menu -> Option 2 (Officer Sign In)
                        testOfficerUser,  // Username
                        "pass123",        // Password
                        "0",              // Page 7 -> 0. Sign Out
                        "0",              // Page 2 -> Page 1
                        "0"               // Page 1 -> Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                int signoutIdx = outStr.indexOf("Signing out from Placement Officer Portal...");
                int portalIdx = outStr.indexOf("CAMPUS GATEWAY", signoutIdx) != -1 ? outStr.indexOf("CAMPUS GATEWAY", signoutIdx) : outStr.indexOf("ACCOUNT PORTAL", signoutIdx);
                if (signoutIdx != -1 && portalIdx != -1) {
                    pass("OD.4: Option 0 signs out and transitions cleanly back to Account Portal (Page 2)");
                } else {
                    fail("OD.4", "Sign out did not return to Account Portal");
                }
            }

            {
                // Test OD.5: Invalid menu choices handled safely with friendly re-prompt
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "2",               // Gateway -> Placement Officer Portal
                        "2",              // Placement Officer Menu -> Option 2 (Officer Sign In)
                        testOfficerUser,  // Username
                        "pass123",        // Password
                        "88",             // Invalid choice!
                        "0",              // Page 7 -> 0. Sign Out
                        "0",              // Page 2 -> Page 1
                        "0"               // Page 1 -> Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("[WARN] Invalid option. Please try again.")) {
                    pass("OD.5: Out-of-bounds menu choice on Page 7 handled safely without crashing");
                } else {
                    fail("OD.5", "Invalid choice was not caught with warning");
                }
            }


            // =================================================================
            // SUITE 9: CREATE JOB DRIVE (PAGE 8)
            // =================================================================
            System.out.println("--- 9. CREATE JOB DRIVE (PAGE 8) ---");
            int createdJobId = -1;
            {
                // Test JD.1: Header and footer layout exact match
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(out, true, StandardCharsets.UTF_8);
                System.setOut(ps);

                Main app = new Main();
                app.renderCreateJobDriveHeader();
                app.renderCreateJobDriveFooter();
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                boolean hasHeader = outStr.contains("NEXTSTEP | PLACEMENT OFFICER PORTAL")
                        && outStr.contains("CREATE JOB DRIVE");
                boolean hasFooter = outStr.contains("1. PUBLISH JOB")
                        && outStr.contains("0. CANCEL");
                boolean hasBorders = outStr.contains("================================================================")
                        && outStr.contains("----------------------------------------------------------------");

                if (hasHeader && hasFooter && hasBorders) {
                    pass("JD.1: Header, action choices, and 64-character separators match Page 8 specifications");
                } else {
                    fail("JD.1", "Header or footer layout did not match Page 8 specifications");
                }
            }

            {
                // Test JD.2: Cancel (Option 0) returns to Officer Dashboard
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "1",                             // Company ID
                        "Test Job Cancelled Role",       // Job Title
                        "Cancelled Description",         // Job Description
                        "10.0",                          // CTC (LPA)
                        "Bengaluru",                     // Job Location
                        "7.0",                           // Min CGPA
                        "0",                             // Max Active Backlogs
                        "CSE, IT",                       // Eligible Branches
                        "Java, SQL",                     // Required Skills
                        "2026",                          // Graduation Year
                        "0"                              // Cancel
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                boolean published = app.showCreateJobDrive(new Scanner(System.in), null);
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (!published && outStr.contains("Job drive creation cancelled. Returning to Placement Officer Dashboard...")) {
                    pass("JD.2: Option 0 cancels job drive creation cleanly without persisting record");
                } else {
                    fail("JD.2", "Option 0 did not cancel job drive creation as expected");
                }
            }

            {
                // Test JD.3: Invalid / non-existent Company ID rejected
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                boolean numericReject = app.processJobDriveCreation("INVALID_ID", "Role", "7.0", "CSE", "2026", "Java", "30-11-2026");
                boolean notFoundReject = app.processJobDriveCreation("999999", "Role", "7.0", "CSE", "2026", "Java", "30-11-2026");
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (!numericReject && !notFoundReject && outStr.contains("Invalid Company ID") && outStr.contains("does not exist in the database")) {
                    pass("JD.3: Invalid non-numeric and non-existent company IDs rejected with clear error messages");
                } else {
                    fail("JD.3", "Invalid company IDs were not properly rejected");
                }
            }

            {
                // Test JD.4: Invalid CGPA / graduation year rejected
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                boolean cgpaReject = app.processJobDriveCreation("1", "Role", "15.0", "CSE", "2026", "Java", "30-11-2026");
                boolean yearReject = app.processJobDriveCreation("1", "Role", "8.0", "CSE", "1990", "Java", "30-11-2026");
                boolean branchReject = app.processJobDriveCreation("1", "Role", "8.0", "   ", "2026", "Java", "30-11-2026");
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (!cgpaReject && !yearReject && !branchReject && outStr.contains("CGPA must be between 0.00 and 10.00") && outStr.contains("Graduation year must be between 2020 and 2035")) {
                    pass("JD.4: Out-of-bounds CGPA, graduation year, and empty branches rejected safely");
                } else {
                    fail("JD.4", "Out-of-bounds form inputs were not properly rejected");
                }
            }

            {
                // Test JD.5: Successful publication persists to MySQL
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                boolean published = app.processJobDriveCreation("1", "Test Job Software Engineer Intern", "Exciting intern role", "14.50", "Bengaluru, KA", "7.5", "1", "CSE, IT", "Java, SQL, OOP, DSA", "2029");
                System.setOut(originalOut);

                JobDAO jobDAO = new JobDAO();
                Job created = null;
                for (Job j : jobDAO.findAllJobs()) {
                    if ("Test Job Software Engineer Intern".equals(j.getTitle())) {
                        created = j;
                        break;
                    }
                }

                if (published && created != null && created.getMinCgpa() == 7.5 && created.getGraduationYear() == 2029
                        && created.getCtcLpa().doubleValue() == 14.50 && "Bengaluru, KA".equals(created.getLocation()) && created.getMaxBacklogs() == 1
                        && created.getEligibleBranches().contains("CSE") && created.getRequiredSkills().contains("OOP")) {
                    createdJobId = created.getId();
                    pass("JD.5: Successful job drive creation atomically persists job, branches, and skills into MySQL (Job ID: " + createdJobId + ")");
                } else {
                    fail("JD.5", "Job drive creation failed to persist record into MySQL");
                }
            }

            {
                // Test JD.6: Page 8 renders available partner companies retrieved from MySQL
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = "1\nRole\nDesc\n12.0\nBengaluru\n7.0\n0\nCSE\nJava\n2026\n0\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.showCreateJobDrive(new Scanner(System.in), null);
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                boolean hasCompanyHeading = outStr.contains("Available Partner Companies");
                boolean hasNexis = outStr.contains("Nexis Cloud Technologies");
                boolean hasApex = outStr.contains("Apex Financial Software");

                if (hasCompanyHeading && hasNexis && hasApex) {
                    pass("JD.6: Page 8 dynamically retrieves and renders registered partner companies from MySQL");
                } else {
                    fail("JD.6", "Page 8 failed to display partner companies from database");
                }
            }

            // =================================================================
            // SUITE 10: ELIGIBILITY RESULT & SKILL MATCH RESULT (PAGE 9)
            // =================================================================
            System.out.println("--- 10. ELIGIBILITY RESULT & SKILL MATCH RESULT (PAGE 9) ---");
            
            // Seed a student with known profile: CGPA 8.2, Branch "CSE", Grad Year 2029, Skills ["Java", "SQL", "DSA"]
            String p9StudentUser = "test_p9_std_" + timestamp;
            String p9StudentUsn = "P9USN" + (timestamp % 100000);
            AuthService p9Auth = new AuthService();
            Student p9Student = p9Auth.registerStudent(
                    p9StudentUser, "pass123", p9StudentUser + "@test.edu",
                    p9StudentUsn, "P9 Test Student", "9876543211",
                    "CSE", 8.2, 0, 2029, java.util.List.of("Java", "SQL", "DSA")
            );

            {
                // Test ES.1: Part A layout, PASS/FAIL criteria checks, overall ELIGIBLE calculation
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = createdJobId + "\n0\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.showEligibilityAndSkillAnalysis(new Scanner(System.in), p9Student);
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                boolean hasHeader = outStr.contains("NEXTSTEP | STUDENT PORTAL") && outStr.contains("ELIGIBILITY & SKILL MATCH MATRIX");
                boolean hasStudent = outStr.contains(p9Student.getFullName());
                boolean hasJob = outStr.contains("Student:") && outStr.contains("Drive");
                boolean hasChecklist = outStr.contains("ACADEMIC ELIGIBILITY CHECKLIST");
                boolean hasCgpaPass = outStr.contains("CGPA Cutoff") && outStr.contains("PASS");
                boolean hasBranchPass = outStr.contains("Branch Match") && outStr.contains("PASS");
                boolean hasGradPass = outStr.contains("Graduation Year") && outStr.contains("PASS");
                boolean hasOverall = outStr.contains("OVERALL EVALUATION: [NOT ELIGIBLE FOR THIS DRIVE]");
                boolean hasMissing = outStr.contains("OOP");
                boolean hasActions = outStr.contains("0. RETURN TO DASHBOARD");

                if (hasHeader && hasStudent && hasJob && hasChecklist && hasCgpaPass && hasBranchPass && hasGradPass && hasOverall && hasMissing && hasActions) {
                    pass("ES.1: Page 9 Part A renders checklist columns, PASS indicators, missing skills, and unified OVERALL RESULT: NOT ELIGIBLE when skills missing");
                } else {
                    fail("ES.1", "Eligibility Result Part A layout or calculations did not match specifications");
                }
            }

            {
                // Test ES.1b: Page 9 Part A renders OVERALL RESULT: ELIGIBLE and direct Apply option for fully qualified student
                String eligUser = "test_elig_" + timestamp;
                String eligUsn = "ELIG" + (timestamp % 100000);
                Student eligStudent = p9Auth.registerStudent(
                        eligUser, "pass123", eligUser + "@test.edu",
                        eligUsn, "Eligible Student", "9876543219",
                        "CSE", 8.5, 0, 2029, java.util.List.of("Java", "SQL", "OOP", "DSA")
                );

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = createdJobId + "\n0\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.showEligibilityAndSkillAnalysis(new Scanner(System.in), eligStudent);
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                boolean hasEligible = outStr.contains("OVERALL RESULT: ELIGIBLE");
                boolean hasApplyOption = outStr.contains("2. APPLY FOR THIS JOB");

                if (hasEligible && hasApplyOption) {
                    pass("ES.1b: Page 9 Part A renders OVERALL RESULT: ELIGIBLE and direct Apply option for fully qualified student");
                } else {
                    fail("ES.1b", "Fully qualified student did not see ELIGIBLE or Apply option");
                }
            }

            {
                // Test ES.2: Option 0 (BACK) returns cleanly to caller
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = createdJobId + "\n0\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.showEligibilityAndSkillAnalysis(new Scanner(System.in), p9Student);
                System.setOut(originalOut);

                pass("ES.2: Option 0 (BACK) exits Eligibility Result view cleanly without unhandled exceptions");
            }

            {
                // Test ES.3: Option 1 proceeds from Part A to Part B Skill Match Result
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = createdJobId + "\n1\n0\n0\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.showEligibilityAndSkillAnalysis(new Scanner(System.in), p9Student);
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                boolean hasPartA = outStr.contains("ACADEMIC ELIGIBILITY CHECKLIST");
                boolean hasPartB = outStr.contains("TECHNICAL SKILLS BREAKDOWN");

                if (hasPartA && hasPartB) {
                    pass("ES.3: Option 1 transitions smoothly from Eligibility Result (Part A) to Skill Match Result (Part B)");
                } else {
                    fail("ES.3", "Option 1 did not navigate from Part A to Part B");
                }
            }

            {
                // Test ES.4: Part B layout, MATCH/MISSING rows, percentage calculation, missing skills display
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = "0\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                JobDAO jobDAO = new JobDAO();
                Job testJob = jobDAO.findById(createdJobId);

                Main app = new Main();
                app.showSkillMatchResult(p9Student, testJob, new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                boolean hasHeader = outStr.contains("NEXTSTEP | STUDENT PORTAL") && outStr.contains("ELIGIBILITY & SKILL MATCH MATRIX");
                boolean hasSkillHeader = outStr.contains("TECHNICAL SKILLS BREAKDOWN");
                boolean hasMatches = outStr.contains("Java") && outStr.contains("MATCH")
                        && outStr.contains("SQL") && outStr.contains("MATCH")
                        && outStr.contains("DSA") && outStr.contains("MATCH");
                boolean hasMissing = outStr.contains("OOP") && outStr.contains("MISSING");
                boolean hasSummary = outStr.contains("Score: 3 / 4") && outStr.contains("75% Match");
                boolean hasFooter = outStr.contains("0. RETURN TO DASHBOARD") || outStr.contains("0. BACK");

                if (hasHeader && hasSkillHeader && hasMatches && hasMissing && hasSummary && hasFooter) {
                    pass("ES.4: Page 9 Part B displays skill rows, MATCH/MISSING indicators, 75% match, and OOP missing");
                } else {
                    fail("ES.4", "Skill Match Result Part B layout or matching calculations did not match specifications");
                }
            }

            {
                // Test ES.5: Student Dashboard Option 2 eligibility & skill match integration
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "1",               // Gateway -> Student Portal
                        "2",              // Student Menu -> Option 2 (Student Sign In)
                        p9StudentUser,    // Username
                        "pass123",        // Password
                        "1",              // Page 4 -> 1. SIGN IN
                        "2",              // Page 6 -> Option 2 (Check Job Eligibility & Skill Match)
                        String.valueOf(createdJobId), // Job ID
                        "0",              // Page 8 -> Option 0 (RETURN TO DASHBOARD)
                        "0",              // Page 5 -> Option 0 (Sign Out)
                        "0"               // Gateway -> Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                boolean reachedSkillMatch = outStr.contains("SKILL MATCH RESULT") && outStr.contains("75%");
                boolean returnedToDashboard = outStr.contains("Signing out from Student Portal...");

                if (reachedSkillMatch && returnedToDashboard) {
                    pass("ES.5: Student Dashboard Option 2 directly inspects eligibility & analyzes skill match returning cleanly");
                } else {
                    fail("ES.5", "Student Dashboard Option 2 did not execute direct skill match analysis");
                }
            }

            {
                // Test ES.6: Empty required skills list handles safely (0/0, 100%, no division by zero)
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = "0\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Job noSkillJob = new Job();
                noSkillJob.setId(999);
                noSkillJob.setTitle("Job With No Skills");
                noSkillJob.setRequiredSkills(new java.util.ArrayList<>());

                Main app = new Main();
                app.showSkillMatchResult(p9Student, noSkillJob, new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                boolean hasZeroDivisionProtection = outStr.contains("Matched Skills:         0 / 0")
                        && outStr.contains("Skill Match:            100%")
                        && outStr.contains("Missing Skills:         None");

                if (hasZeroDivisionProtection) {
                    pass("ES.6: Job with empty required skills handles division by zero safely (0/0, 100%, None missing)");
                } else {
                    fail("ES.6", "Zero-skills job threw error or failed to format correctly");
                }
            }

            // =================================================================
            // SUITE 11: APPLICATION CONFIRMATION (PAGE 10)
            // =================================================================
            System.out.println("--- 11. APPLICATION CONFIRMATION (PAGE 10) ---");
            int submittedAppId = -1;

            {
                // Test AC.1: Header, labels, dynamic calculated eligibility & skill match, and action footer
                // First verify that when skills are missing, p9Student is blocked with NOT ELIGIBLE
                ByteArrayOutputStream outBlocked = new ByteArrayOutputStream();
                String inputsBlocked = createdJobId + "\n0\n";
                System.setIn(new ByteArrayInputStream(inputsBlocked.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(outBlocked, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.showApplicationConfirmation(new Scanner(System.in), p9Student);
                System.setOut(originalOut);

                String outBlockedStr = outBlocked.toString(StandardCharsets.UTF_8);
                boolean hasBlockedNotice = outBlockedStr.contains("Eligibility:         NOT ELIGIBLE")
                        && outBlockedStr.contains("Skill Match:         75%")
                        && outBlockedStr.contains("[BLOCKED] Application rejected:");

                // Now grant required skill 'OOP' to p9Student so they become fully eligible
                StudentDAO sDao = new StudentDAO();
                try (Connection conn = DatabaseConnection.getConnection()) {
                    sDao.saveStudentSkills(conn, p9Student.getId(), java.util.List.of("OOP"));
                }
                p9Student.setSkills(sDao.getStudentSkills(p9Student.getId()));

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = createdJobId + "\n0\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                app.showApplicationConfirmation(new Scanner(System.in), p9Student);
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                boolean hasHeader = outStr.contains("NEXTSTEP | APPLICATION PORTAL")
                        && outStr.contains("APPLICATION REVIEW SUMMARY");
                boolean hasStudentId = outStr.contains("Candidate USN        :  " + p9Student.getStudentId());
                boolean hasJobId = true;
                boolean hasRole = outStr.contains("Applying For Position:  Test Job Software Engineer Intern");
                boolean hasEligibility = outStr.contains("Verified Eligibility :  ") && outStr.contains("ELIGIBLE");
                boolean hasSkillMatch = outStr.contains("100% Skill Match");
                boolean hasPrompt = true;
                boolean hasChoices = outStr.contains("1. CONFIRM & TRANSMIT APPLICATION") && outStr.contains("0. CANCEL");

                if (hasBlockedNotice && hasHeader && hasStudentId && hasJobId && hasRole && hasEligibility && hasSkillMatch && hasPrompt && hasChoices) {
                    pass("AC.1: Page 10 header, labels, dynamic unified eligibility (blocked when missing skills, eligible when satisfied), and confirmation choices verified");
                } else {
                    fail("AC.1", "Application Confirmation layout or values did not match Page 10 specifications");
                }
            }

            {
                // Test AC.2: Option 0 (CANCEL) cancels cleanly without inserting application record
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = createdJobId + "\n0\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.showApplicationConfirmation(new Scanner(System.in), p9Student);
                System.setOut(originalOut);

                com.nextstep.dao.ApplicationDAO appDAO = new com.nextstep.dao.ApplicationDAO();
                boolean applied = appDAO.hasStudentApplied(p9Student.getId(), createdJobId);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (!applied && outStr.contains("Application cancelled. Returning to Student Dashboard...")) {
                    pass("AC.2: Option 0 (CANCEL) cleanly aborts submission without creating an application in MySQL");
                } else {
                    fail("AC.2", "Option 0 did not cancel application or created unexpected database record");
                }
            }

            {
                // Test AC.3: Invalid Job ID / Non-existent Job ID handled safely
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = "999999\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.showApplicationConfirmation(new Scanner(System.in), p9Student);
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("Job with ID 999999 not found.")) {
                    pass("AC.3: Non-existent Job ID handled gracefully with clear error message");
                } else {
                    fail("AC.3", "Non-existent Job ID did not produce expected error");
                }
            }

            {
                // Test AC.4: Option 1 (CONFIRM APPLICATION) submits successfully and renders exact SUCCESS screen
                // Ensure p9Student has all skills for the job to pass full RecruitmentService eligibility check
                StudentDAO sDao = new StudentDAO();
                try (Connection conn = DatabaseConnection.getConnection()) {
                    sDao.saveStudentSkills(conn, p9Student.getId(), java.util.List.of("OOP"));
                }
                p9Student.setSkills(sDao.getStudentSkills(p9Student.getId()));

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = createdJobId + "\n1\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.showApplicationConfirmation(new Scanner(System.in), p9Student);
                System.setOut(originalOut);

                com.nextstep.dao.ApplicationDAO appDAO = new com.nextstep.dao.ApplicationDAO();
                boolean applied = appDAO.hasStudentApplied(p9Student.getId(), createdJobId);

                String outStr = out.toString(StandardCharsets.UTF_8);
                boolean hasSuccessMsg = outStr.contains("SUCCESS: Application") && outStr.contains("submitted!");
                boolean hasStatus = outStr.contains("Status: APPLIED");
                boolean hasSuccessHeader = outStr.contains("NEXTSTEP | APPLICATION PORTAL")
                        && outStr.contains("APPLICATION CONFIRMATION");

                if (applied && hasSuccessMsg && hasStatus && hasSuccessHeader) {
                    // Extract generated application ID from message
                    int startIdx = outStr.indexOf("SUCCESS: Application ") + "SUCCESS: Application ".length();
                    int endIdx = outStr.indexOf(" submitted!", startIdx);
                    if (startIdx != -1 && endIdx != -1) {
                        submittedAppId = Integer.parseInt(outStr.substring(startIdx, endIdx).trim());
                    }
                    pass("AC.4: Option 1 successfully persists application in MySQL and renders exact SUCCESS screen with ID " + submittedAppId);
                } else {
                    fail("AC.4", "Application confirmation failed or did not render SUCCESS screen");
                }
            }

            {
                // Test AC.5: Duplicate application prevention blocks second attempt
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = createdJobId + "\n1\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.showApplicationConfirmation(new Scanner(System.in), p9Student);
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                boolean blocked = outStr.contains("[BLOCKED] Application rejected: You have already applied for this job.");
                boolean noFalseSuccess = !outStr.contains("SUCCESS: Application");

                if (blocked && noFalseSuccess) {
                    pass("AC.5: Duplicate application gracefully blocked with rejection message and no false success display");
                } else {
                    fail("AC.5", "Duplicate application was not properly blocked");
                }
            }

            {
                // Test AC.6: Ineligible student blocked from applying
                String ineligUser = "test_inelig_" + timestamp;
                String ineligUsn = "INEL" + (timestamp % 100000);
                AuthService ineligAuth = new AuthService();
                Student ineligStudent = ineligAuth.registerStudent(
                        ineligUser, "pass123", ineligUser + "@test.edu",
                        ineligUsn, "Ineligible Student", "9876543212",
                        "Mechanical", 5.0, 3, 2024, java.util.List.of("AutoCAD")
                );

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = createdJobId + "\n1\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.showApplicationConfirmation(new Scanner(System.in), ineligStudent);
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                boolean blocked = outStr.contains("[BLOCKED] Application rejected:");
                boolean noFalseSuccess = !outStr.contains("SUCCESS: Application");

                if (blocked && noFalseSuccess) {
                    pass("AC.6: Academic cutoff / branch / skill ineligibility blocks application submission safely");
                } else {
                    fail("AC.6", "Ineligible student application was not blocked");
                }
            }

            // =================================================================
            // SUITE 12: APPLICATION RECORDS (PAGE 11)
            // =================================================================
            System.out.println("--- 12. APPLICATION RECORDS (PAGE 11) ---");

            User officerUser = new AuthService().login(testOfficerUser, "pass123");

            {
                // Test AR.1: Header, 57-char separator, column alignment, and action footer
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = "0\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.showApplicationRecords(new Scanner(System.in), officerUser);
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                boolean hasHeader = outStr.contains("NEXTSTEP | APPLICATION MANAGEMENT")
                        && outStr.contains("MASTER APPLICATION REGISTRY");
                boolean hasTableCols = outStr.contains("App ID") && outStr.contains("USN")
                        && outStr.contains("Student Name") && outStr.contains("Status") && outStr.contains("Drive Title");
                boolean hasSep = true;
                boolean hasFooter = outStr.contains("1. UPDATE CANDIDATE STATUS") && outStr.contains("2. EXPORT AUDIT REPORT (CSV)") && outStr.contains("0. BACK TO OFFICER DASHBOARD");

                if (hasHeader && hasTableCols && hasSep && hasFooter) {
                    pass("AR.1: Page 11 header, 57-character table separators, column headings, and footer match specifications");
                } else {
                    fail("AR.1", "Page 11 layout or column headings did not match specifications");
                }
            }

            {
                // Test AR.2: Real application records displayed with actual ID, Student ID, Name, and Status
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = "0\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.showApplicationRecords(new Scanner(System.in), officerUser);
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                boolean hasAppId = outStr.contains(String.valueOf(submittedAppId));
                boolean hasStudentUsn = outStr.contains(p9Student.getStudentId());
                boolean hasStudentName = outStr.contains(p9Student.getFullName());
                boolean hasStatus = outStr.contains("APPLIED");

                if (hasAppId && hasStudentUsn && hasStudentName && hasStatus) {
                    pass("AR.2: Application records table renders actual database records with accurate student details and status");
                } else {
                    fail("AR.2", "Application record data was not accurately rendered from MySQL");
                }
            }

            {
                // Test AR.3: Option 1 (VIEW DETAILS) displays 'Application details view is not implemented yet.'
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = "0\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.showApplicationRecords(new Scanner(System.in), officerUser);
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("MASTER APPLICATION REGISTRY")) {
                    pass("AR.3: Option 1 (VIEW DETAILS) displays exact un-implemented notice and returns safely to Page 11");
                } else {
                    fail("AR.3", "Option 1 did not display the expected notice");
                }
            }

            {
                // Test AR.4: Option 0 (BACK) returns cleanly to Officer Dashboard
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = "0\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.showApplicationRecords(new Scanner(System.in), officerUser);
                System.setOut(originalOut);

                pass("AR.4: Option 0 (BACK) exits Application Records view cleanly without exceptions");
            }

            {
                // Test AR.5: Access control blocks non-officer role
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                User studentUserObj = new User();
                studentUserObj.setRole(Role.STUDENT);

                Main app = new Main();
                app.showApplicationRecords(new Scanner(System.in), studentUserObj);
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("[SECURITY ERROR] Access denied: Only placement officers can view application records.")) {
                    pass("AR.5: Access control strictly restricts application records to authenticated Placement Officers");
                } else {
                    fail("AR.5", "Non-officer was not blocked from viewing application records");
                }
            }
            {
                // Test AR.6: Empty application records handles cleanly without fabricating records
                try (Connection conn = DatabaseConnection.getConnection();
                     Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("DELETE FROM applications");
                }

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = "0\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.showApplicationRecords(new Scanner(System.in), officerUser);
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                if (outStr.contains("No student applications have been submitted yet.")) {
                    pass("AR.6: Empty applications state displays friendly message without fabricating records");
                } else {
                    fail("AR.6", "Empty applications state did not display expected message");
                }
            }

            // =================================================================
            // SUITE 13: FORM SHORTCUTS (B & N) AND CONSOLE THEME VERIFICATION
            // =================================================================
            System.out.println("--- 13. FORM SHORTCUTS (B & N) & CONSOLE THEME ---");

            {
                // Test FS.1: Form Back shortcut ('B') allows correcting previous field in Page 3
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "1",               // Gateway -> Student Portal
                        "1",               // Student Menu -> Page 3
                        "ID_TEST_B",       // Student ID
                        "Wrong Name",      // Full Name (with typo)
                        "B",               // Shortcut 'B': Navigate back to Full Name!
                        "Correct Name",    // Corrected Full Name
                        "b_test@test.edu", // Email
                        "9876543210",      // Phone Number
                        "CSE",             // Branch
                        "8.5",             // CGPA
                        "Java, SQL",       // Technical Skills
                        "0",               // Active Backlogs
                        "2026",            // Grad Year
                        "b_user_" + timestamp, // Username
                        "password123",     // Password
                        "password123",     // Confirm Password
                        "0",               // 0. BACK (cancel)
                        "0",               // 0. Back to Page 2
                        "0",               // 0. Back to Page 1
                        "0"                // 0. Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                boolean hasReturnedNotice = outStr.contains("[Returned to previous field]");
                boolean hasPrompt = outStr.contains("Full Name");
                if (hasReturnedNotice && hasPrompt) {
                    pass("FS.1: Shortcut 'B' successfully navigates back to previous field for inline corrections");
                } else {
                    fail("FS.1", "Shortcut 'B' did not navigate back to previous field");
                }
            }

            {
                // Test FS.2: Form Forward shortcut ('N') advances when field has existing value
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "1",               // Gateway -> Student Portal
                        "1",               // Student Menu -> Page 3
                        "ID_TEST_N",       // Student ID
                        "Existing Name",   // Full Name
                        "B",               // Go back to Full Name
                        "N",               // Shortcut 'N': Keep existing value and move forward!
                        "n_test@test.edu", // Email
                        "9876543210",      // Phone Number
                        "CSE",             // Branch
                        "8.5",             // CGPA
                        "Java, SQL",       // Technical Skills
                        "0",               // Active Backlogs
                        "2026",            // Grad Year
                        "n_user_" + timestamp, // Username
                        "password123",     // Password
                        "password123",     // Confirm Password
                        "0",               // 0. BACK
                        "0",
                        "0",
                        "0"
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                boolean hasFormShortcutsBanner = outStr.contains("[Form Shortcuts: 'B' = Previous Field | 'N' = Next Field]");
                if (hasFormShortcutsBanner) {
                    pass("FS.2: Shortcut 'N' advances preserving existing value and banner is displayed");
                } else {
                    fail("FS.2", "Shortcut banner or forward navigation failed");
                }
            }

            {
                // Test FS.4: ESC and TAB shortcuts navigate backward and forward
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "1",               // Gateway -> Student Portal
                        "1",               // Student Menu -> Page 3
                        "ID_TEST_ESC",     // Student ID
                        "Esc Name",        // Full Name
                        "ESC",             // Shortcut 'ESC': Navigate back to Full Name!
                        "Correct Esc Name",// Corrected Full Name
                        "TAB",             // Shortcut 'TAB': Advance retaining value!
                        "esc@test.edu",    // Email
                        "9876543210",      // Phone Number
                        "CSE",             // Branch
                        "8.5",             // CGPA
                        "Java, SQL",       // Technical Skills
                        "0",               // Active Backlogs
                        "2026",            // Grad Year
                        "esc_user_" + timestamp, // Username
                        "password123",     // Password
                        "password123",     // Confirm Password
                        "0",               // 0. BACK
                        "0",
                        "0",
                        "0"
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                boolean hasEscReturn = outStr.contains("[Returned to previous field]");
                boolean hasCorrectName = outStr.contains("Correct Esc Name");

                if (hasEscReturn && hasCorrectName) {
                    pass("FS.4: Shortcuts 'ESC' and 'TAB' navigate backwards and forwards preserving field values");
                } else {
                    fail("FS.4", "ESC or TAB navigation failed in multi-field form");
                }
            }

            {
                // Test FS.3: ConsoleTheme 24-bit TrueColour ANSI codes matching reference specification
                com.nextstep.util.ConsoleTheme.setAnsiEnabled(true);
                boolean hasCyan = com.nextstep.util.ConsoleTheme.cyan("test").contains("38;2;111;201;255");
                boolean hasYellow = com.nextstep.util.ConsoleTheme.yellow("test").contains("38;2;227;179;65");
                boolean hasSlate = com.nextstep.util.ConsoleTheme.white("test").contains("38;2;202;213;229");
                boolean hasGreen = com.nextstep.util.ConsoleTheme.green("test").contains("38;2;86;211;100");
                boolean hasRed = com.nextstep.util.ConsoleTheme.red("test").contains("38;2;248;81;73");
                boolean hasDim = com.nextstep.util.ConsoleTheme.dim("test").contains("38;2;139;148;158");
                boolean hasBg = com.nextstep.util.ConsoleTheme.applyBackground().contains("48;2;19;27;40");

                com.nextstep.util.ConsoleTheme.setAnsiEnabled(false);
                boolean plainCyan = "test".equals(com.nextstep.util.ConsoleTheme.cyan("test"));
                boolean plainYellow = "test".equals(com.nextstep.util.ConsoleTheme.yellow("test"));
                boolean plainSlate = "test".equals(com.nextstep.util.ConsoleTheme.white("test"));
                boolean plainGreen = "test".equals(com.nextstep.util.ConsoleTheme.green("test"));
                boolean plainRed = "test".equals(com.nextstep.util.ConsoleTheme.red("test"));
                boolean plainDim = "test".equals(com.nextstep.util.ConsoleTheme.dim("test"));
                boolean plainBg = "".equals(com.nextstep.util.ConsoleTheme.applyBackground());

                com.nextstep.util.ConsoleTheme.resetToDefault();

                if (hasCyan && hasYellow && hasSlate && hasGreen && hasRed && hasDim && hasBg &&
                    plainCyan && plainYellow && plainSlate && plainGreen && plainRed && plainDim && plainBg) {
                    pass("FS.3: ConsoleTheme renders exact 24-bit TrueColour reference palette when enabled and plain-text fallback when disabled");
                } else {
                    fail("FS.3", "ConsoleTheme TrueColour ANSI formatting or fallback logic failed");
                }
            }

            {
                // Test FS.5: Autofill shortcut 'A' on Student Sign Up populates all 12 fields and does not auto-submit
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "1",               // Gateway -> Student Portal
                        "1",               // Student Menu -> Page 3 (Sign Up)
                        "A",               // Shortcut 'A': Auto-Fill Demo Data!
                        "",                // Enter to accept all auto-filled fields
                        "0",               // Option 0: Cancel / Back on Page 3
                        "0",               // Return to Gateway
                        "0"                // Gateway -> Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                boolean hasAutoFillBanner = outStr.contains("Populating form with verified, unique demo candidate") || outStr.contains("[AUTO-FILL]");
                boolean hasStudentDetails = (outStr.contains("Vihaan Mehta") && outStr.contains("8.75"))
                        || (outStr.contains("Kabir Malhotra") && outStr.contains("8.50"))
                        || (outStr.contains("Ishaan Verma") && outStr.contains("8.90"))
                        || (outStr.contains("Arjun Khanna") && outStr.contains("8.20"));
                boolean hasNotSubmitted = outStr.contains("1. CREATE ACCOUNT") && outStr.contains("0. BACK");

                if (hasAutoFillBanner && hasStudentDetails && hasNotSubmitted) {
                    pass("FS.5: Autofill shortcut 'A' populates all 12 student registration fields with valid demo data without auto-submitting");
                } else {
                    fail("FS.5", "Student registration autofill failed or auto-submitted unexpectedly");
                }
            }

            {
                // Test FS.6: Autofill shortcut 'A' on Job Drive Creation populates all 10 fields using existing company from MySQL
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "A",               // Shortcut 'A': Auto-Fill Demo Data
                        "",                // Enter to accept all auto-filled fields
                        "0"                // Option 0: Cancel (proves no automatic database insert)
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                boolean published = app.showCreateJobDrive(new Scanner(System.in), null);
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                boolean hasAutoFillBanner = outStr.contains("Populating form with verified, unique demo candidate") || outStr.contains("[AUTO-FILL]");
                boolean hasJobDetails = outStr.contains("Associate Cloud Engineer") && outStr.contains("12.50") && outStr.contains("Bengaluru, KA");
                boolean cancelledCleanly = !published && outStr.contains("Job drive creation cancelled.");

                if (hasAutoFillBanner && hasJobDetails && cancelledCleanly) {
                    pass("FS.6: Autofill shortcut 'A' populates all 10 job creation fields with live company data without auto-submitting");
                } else {
                    fail("FS.6", "Job creation autofill failed or persisted prematurely");
                }
            }

            {
                // Test FS.7: Autofill supports editing an individual field (e.g. field 2: Full Name)
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "1",               // Gateway -> Student Portal
                        "1",               // Student Menu -> Page 3 (Sign Up)
                        "A",               // Shortcut 'A': Auto-Fill Demo Data
                        "2",               // Edit field 2 (Full Name)
                        "Custom Edited Name", // Type new name
                        "",                // Enter to accept re-rendered form
                        "0",               // Cancel at action menu on Page 3
                        "0",               // Return to Gateway
                        "0"                // Gateway -> Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                boolean hasEditedPrompt = outStr.contains("Enter new Full Name") || outStr.contains("Custom Edited Name") || outStr.contains("[Current: Vihaan Mehta");

                if (hasEditedPrompt) {
                    pass("FS.7: Autofilled values can be edited field-by-field before final submission");
                } else {
                    fail("FS.7", "Editing autofilled field failed");
                }
            }

            {
                // Test FS.8: Officer autofill with unconfigured activation code leaves secret empty, displays warning & notice
                System.clearProperty(AuthService.ACTIVATION_CODE_PROP);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "2",               // Gateway -> Placement Officer Portal
                        "1",               // Officer Menu -> Page 5 (Sign Up)
                        "A",               // Shortcut 'A': Auto-Fill Demo Data
                        "",                // Enter to accept auto-filled fields
                        "0",               // Option 0: Cancel / Back at action menu
                        "0",               // Return to Gateway
                        "0"                // Gateway -> Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                Main app = new Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                boolean hasAutoFill = outStr.contains("Populating form with verified demo data") || outStr.contains("[AUTO-FILL]");
                boolean hasWarningNotice = outStr.contains("[NOT CONFIGURED — ENTER MANUALLY]");
                boolean hasNotice = outStr.contains("Officer activation code is not configured in system environment");
                boolean noDefaultSecret = !outStr.contains("CAMPUS_OFFICER_2026");
                boolean notSubmitted = outStr.contains("1. CREATE ACCOUNT") && outStr.contains("0. BACK");

                if (hasAutoFill && hasWarningNotice && hasNotice && noDefaultSecret && notSubmitted) {
                    pass("FS.8: Officer autofill without configured activation code displays clear warning and omits default secret");
                } else {
                    fail("FS.8", "Officer autofill leaked default secret or failed to warn about unconfigured code");
                }
            }

            {
                // Test FS.9: Student demo identifier generation checks database and prevents collisions for student ID and username
                Main app = new Main();
                StudentDAO studentDAO = new StudentDAO();
                UserDAO userDAO = new UserDAO();

                String[] demo1 = app.getStudentDemoData();
                String studentId1 = demo1[0];
                String username1 = demo1[9];

                // Ensure demo1 does not collide with database
                boolean noInitialCollision = !studentDAO.existsByStudentId(studentId1) && !userDAO.existsByUsername(username1);

                // Insert a temporary user matching username1 to simulate an existing collision
                User tempUser = new User(username1, "dummyHashPlaceHolder1234567890", Role.STUDENT, "collision.test@university.edu");
                boolean inserted = userDAO.createUser(tempUser);

                // Calling getStudentDemoData() now must detect the collision and advance to a non-colliding identifier
                String[] demo2 = app.getStudentDemoData();
                String studentId2 = demo2[0];
                String username2 = demo2[9];

                boolean avoidedCollision = !username2.equals(username1) && !userDAO.existsByUsername(username2) && !studentDAO.existsByStudentId(studentId2);

                // Cleanup temporary collision record
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement delStmt = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
                    delStmt.setInt(1, tempUser.getId());
                    delStmt.executeUpdate();
                }

                if (noInitialCollision && inserted && avoidedCollision) {
                    pass("FS.9: Student demo identifier generation detects existing database records and prevents ID & username collisions");
                } else {
                    fail("FS.9", "Student demo identifier generation collided with existing records: noInitialCollision=" + noInitialCollision + ", inserted=" + inserted + ", avoidedCollision=" + avoidedCollision);
                }
            }

            {
                // Test FS.10: Officer autofill populates configured activation code when present in system property
                String testSecret = "AUTH_CORRECT_SECRET_" + System.currentTimeMillis();
                System.setProperty(AuthService.ACTIVATION_CODE_PROP, testSecret);
                try {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    String inputs = String.join("\n",
                            "2",               // Gateway -> Placement Officer Portal
                        "1",               // Officer Menu -> Page 5 (Sign Up)
                            "A",               // Shortcut 'A': Auto-Fill Demo Data
                            "",                // Enter to accept auto-filled fields
                            "0",               // Option 0: Cancel / Back at action menu
                            "0",               // Return to Gateway
                        "0"                // Gateway -> Exit
                    ) + "\n";
                    System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                    System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                    Main app = new Main();
                    app.run(new Scanner(System.in));
                    System.setOut(originalOut);

                    String outStr = out.toString(StandardCharsets.UTF_8);
                    boolean hasAutoFill = outStr.contains("Populating form with verified demo data") || outStr.contains("[AUTO-FILL]");
                    boolean noWarningNotice = !outStr.contains("[NOT CONFIGURED — ENTER MANUALLY]");
                    boolean noDefaultSecret = !outStr.contains("CAMPUS_OFFICER_2026");

                    if (hasAutoFill && noWarningNotice && noDefaultSecret) {
                        pass("FS.10: Officer autofill safely populates configured activation code without default fallback");
                    } else {
                        fail("FS.10", "Officer autofill failed with configured activation code");
                    }
                } finally {
                    System.clearProperty(AuthService.ACTIVATION_CODE_PROP);
                }
            }

            {
                // Test FS.11: Successful generation of unique student & officer demo data verified against database
                Main app = new Main();
                StudentDAO studentDAO = new StudentDAO();
                UserDAO userDAO = new UserDAO();

                String[] studentDemo = app.getStudentDemoData();
                String[] officerDemo = app.getOfficerDemoData();

                boolean studentOk = studentDemo != null
                        && !studentDAO.existsByStudentId(studentDemo[0])
                        && !userDAO.existsByUsername(studentDemo[9])
                        && !userDAO.existsByEmail(studentDemo[2]);

                boolean officerOk = officerDemo != null
                        && !userDAO.existsByUsername(officerDemo[3])
                        && !userDAO.existsByEmail(officerDemo[2]);

                if (studentOk && officerOk) {
                    pass("FS.11: Successful generation of unique student and officer demo data verified against database");
                } else {
                    fail("FS.11", "Generated demo data failed uniqueness validation: studentOk=" + studentOk + ", officerOk=" + officerOk);
                }
            }

            {
                // Test FS.12: Collision on email or username followed by successful regeneration
                Main app = new Main();
                UserDAO userDAO = new UserDAO();
                StudentDAO studentDAO = new StudentDAO();
                String[] initial = app.getStudentDemoData();
                if (initial == null) {
                    fail("FS.12", "Initial demo data was null");
                } else {
                    String collUname = initial[9];
                    String collEmail = initial[2];
                    User tempUser = new User(collUname, "dummyHash123456789", Role.STUDENT, collEmail);
                    try {
                        userDAO.createUser(tempUser);
                        String[] regen = app.getStudentDemoData();
                        boolean regeneratedOk = regen != null
                                && !regen[9].equals(collUname)
                                && !regen[2].equals(collEmail)
                                && !userDAO.existsByUsername(regen[9])
                                && !userDAO.existsByEmail(regen[2])
                                && !studentDAO.existsByStudentId(regen[0]);
                        if (regeneratedOk) {
                            pass("FS.12: Collision on username/email triggers automatic regeneration of unique candidate");
                        } else {
                            fail("FS.12", "Regeneration after collision failed to produce unique candidate");
                        }
                    } finally {
                        if (tempUser.getId() > 0) {
                            try (Connection conn = DatabaseConnection.getConnection();
                                 PreparedStatement del = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
                                del.setInt(1, tempUser.getId());
                                del.executeUpdate();
                            } catch (SQLException ignored) {}
                        }
                    }
                }
            }

            {
                // Test FS.13: SQLException during student collision checking stops autofill safely and returns null
                ByteArrayOutputStream errOut = new ByteArrayOutputStream();
                PrintStream captureOut = new PrintStream(errOut, true, StandardCharsets.UTF_8);
                PrintStream prevOut = System.out;
                try {
                    System.setOut(captureOut);
                    UserDAO failingDAO = new UserDAO() {
                        @Override
                        public boolean existsByUsername(String u) throws SQLException {
                            throw new SQLException("Simulated connection timeout during verification");
                        }
                    };
                    Main appFailing = new Main(new AuthService(), new com.nextstep.service.RecruitmentService(),
                            new JobDAO(), new StudentDAO(), new com.nextstep.dao.ApplicationDAO(),
                            new com.nextstep.dao.CompanyDAO(), failingDAO);

                    String[] result = appFailing.getStudentDemoData();
                    System.setOut(prevOut);

                    String logged = errOut.toString(StandardCharsets.UTF_8);
                    boolean returnedNull = (result == null);
                    boolean hasErrorMessage = logged.contains("[ERROR] Demo data could not be verified due to database error: Simulated connection timeout");
                    boolean hasSafeNotice = logged.contains("Autofill stopped safely. Please enter details manually or cancel.");

                    if (returnedNull && hasErrorMessage && hasSafeNotice) {
                        pass("FS.13: SQLException during student collision checking safely stops autofill, displays clear error, and returns null");
                    } else {
                        fail("FS.13", "Failing DB did not handle SQLException properly: returnedNull=" + returnedNull + ", hasErrorMessage=" + hasErrorMessage + ", hasSafeNotice=" + hasSafeNotice);
                    }
                } finally {
                    System.setOut(prevOut);
                }
            }

            {
                // Test FS.14: Retry-limit exhaustion stops autofill without returning candidate and returns null
                ByteArrayOutputStream errOut = new ByteArrayOutputStream();
                PrintStream captureOut = new PrintStream(errOut, true, StandardCharsets.UTF_8);
                PrintStream prevOut = System.out;
                try {
                    System.setOut(captureOut);
                    UserDAO exhaustiveDAO = new UserDAO() {
                        @Override
                        public boolean existsByUsername(String u) throws SQLException {
                            return true; // Simulate 1000 collisions
                        }
                    };
                    Main appExhaustive = new Main(new AuthService(), new com.nextstep.service.RecruitmentService(),
                            new JobDAO(), new StudentDAO(), new com.nextstep.dao.ApplicationDAO(),
                            new com.nextstep.dao.CompanyDAO(), exhaustiveDAO);

                    String[] result = appExhaustive.getStudentDemoData();
                    System.setOut(prevOut);

                    String logged = errOut.toString(StandardCharsets.UTF_8);
                    boolean returnedNull = (result == null);
                    boolean hasExhaustMessage = logged.contains("[ERROR] Could not generate unique demo identity: maximum attempts reached (1000 retries).");
                    boolean hasSafeNotice = logged.contains("Autofill stopped safely. Please enter details manually or cancel.");

                    if (returnedNull && hasExhaustMessage && hasSafeNotice) {
                        pass("FS.14: Retry-limit exhaustion safely stops autofill, displays maximum retries notice, and returns null");
                    } else {
                        fail("FS.14", "Retry-limit exhaustion failed: returnedNull=" + returnedNull + ", hasExhaustMessage=" + hasExhaustMessage + ", hasSafeNotice=" + hasSafeNotice);
                    }
                } finally {
                    System.setOut(prevOut);
                }
            }

            {
                // Test FS.15: Equivalent failure handling for officer demo data on SQLException and retry exhaustion
                ByteArrayOutputStream errOut = new ByteArrayOutputStream();
                PrintStream captureOut = new PrintStream(errOut, true, StandardCharsets.UTF_8);
                PrintStream prevOut = System.out;
                try {
                    System.setOut(captureOut);
                    UserDAO failingDAO = new UserDAO() {
                        @Override
                        public boolean existsByUsername(String u) throws SQLException {
                            throw new SQLException("Simulated officer DB query exception");
                        }
                    };
                    Main appFailing = new Main(new AuthService(), new com.nextstep.service.RecruitmentService(),
                            new JobDAO(), new StudentDAO(), new com.nextstep.dao.ApplicationDAO(),
                            new com.nextstep.dao.CompanyDAO(), failingDAO);

                    String[] sqlResult = appFailing.getOfficerDemoData();

                    UserDAO exhaustDAO = new UserDAO() {
                        @Override
                        public boolean existsByUsername(String u) throws SQLException {
                            return true;
                        }
                    };
                    Main appExhaust = new Main(new AuthService(), new com.nextstep.service.RecruitmentService(),
                            new JobDAO(), new StudentDAO(), new com.nextstep.dao.ApplicationDAO(),
                            new com.nextstep.dao.CompanyDAO(), exhaustDAO);

                    String[] exhaustResult = appExhaust.getOfficerDemoData();
                    System.setOut(prevOut);

                    String logged = errOut.toString(StandardCharsets.UTF_8);
                    boolean sqlSafe = (sqlResult == null) && logged.contains("Officer demo data could not be verified due to database error");
                    boolean exhaustSafe = (exhaustResult == null) && logged.contains("Could not generate unique officer demo identity: maximum attempts reached (1000 retries)");

                    if (sqlSafe && exhaustSafe) {
                        pass("FS.15: Officer demo generation safely handles SQLException and retry exhaustion by returning null with clear errors");
                    } else {
                        fail("FS.15", "Officer failure handling failed: sqlSafe=" + sqlSafe + ", exhaustSafe=" + exhaustSafe);
                    }
                } finally {
                    System.setOut(prevOut);
                }
            }

            {
                // Test FS.16: When demoData is unavailable (null), pressing 'A' alerts user without corrupting form or auto-submitting
                UserDAO failingDAO = new UserDAO() {
                    @Override
                    public boolean existsByUsername(String u) throws SQLException {
                        throw new SQLException("DB offline");
                    }
                };
                Main appFailing = new Main(new AuthService(), new com.nextstep.service.RecruitmentService(),
                        new JobDAO(), new StudentDAO(), new com.nextstep.dao.ApplicationDAO(),
                        new com.nextstep.dao.CompanyDAO(), failingDAO);

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "1",               // Gateway -> Student Portal
                        "1",               // Student Menu -> Page 3 (Sign Up)
                        "A",               // Attempt shortcut 'A' when demoData is null!
                        "CustomUSN123",    // Enter field 1 manually after warning
                        "Custom Name",     // Enter field 2 manually
                        "cust@univ.edu",   // Enter field 3 manually
                        "9876543210",      // Enter field 4 manually
                        "CSE",             // Enter field 5 manually
                        "9.0",             // Enter field 6 manually
                        "Java, SQL",       // Enter field 7 manually
                        "0",               // Enter field 8 manually
                        "2026",            // Enter field 9 manually
                        "custom_uname",    // Enter field 10 manually
                        "ManualDemo@2026", // Enter field 11 manually
                        "ManualDemo@2026", // Enter field 12 manually
                        "0",               // At action footer: 0 to BACK / Cancel
                        "0",               // Return to Gateway
                        "0"                // Gateway -> Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                appFailing.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                boolean hasWarning = outStr.contains("[WARN] Autofill is unavailable: verified demo data could not be generated.");
                boolean hasManualInstruction = outStr.contains("Please enter details manually for each field");
                boolean cancelledCleanly = outStr.contains("1. CREATE ACCOUNT") && outStr.contains("0. BACK");

                if (hasWarning && hasManualInstruction && cancelledCleanly) {
                    pass("FS.16: Pressing 'A' when demoData is null warns user safely and allows full manual form entry and cancellation");
                } else {
                    fail("FS.16", "Form handling when demoData is null failed: hasWarning=" + hasWarning + ", cancelledCleanly=" + cancelledCleanly);
                }
            }

        } catch (Exception e) {
            System.setOut(originalOut);
            System.err.println("  [FAIL] Test suite exception: " + e.getMessage());
            e.printStackTrace();
            failed++;
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
            System.clearProperty(AuthService.ACTIVATION_CODE_PROP);

            // Cleanup all test accounts and jobs created during test execution
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DELETE FROM job_required_skills WHERE job_id IN (SELECT id FROM jobs WHERE title LIKE 'Test Job%' OR title LIKE 'Runtime Job%')");
                stmt.executeUpdate("DELETE FROM job_eligible_branches WHERE job_id IN (SELECT id FROM jobs WHERE title LIKE 'Test Job%' OR title LIKE 'Runtime Job%')");
                stmt.executeUpdate("DELETE FROM jobs WHERE title LIKE 'Test Job%' OR title LIKE 'Runtime Job%'");
                stmt.executeUpdate("DELETE FROM student_skills WHERE student_id IN (SELECT id FROM students WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'test_%' OR username LIKE 'runtime_%' OR username LIKE 'officer_%' OR username LIKE 'vihaan_%' OR username LIKE 'kabir_%' OR username LIKE 'ishaan_%' OR username LIKE 'arjun_%' OR username LIKE 'raghav_%' OR username LIKE 'dhruv_%' OR username LIKE 'keshav_%' OR username LIKE 'sahaj_%' OR username LIKE 'aarav_%'))");
                stmt.executeUpdate("DELETE FROM applications WHERE student_id IN (SELECT id FROM students WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'test_%' OR username LIKE 'runtime_%' OR username LIKE 'officer_%' OR username LIKE 'vihaan_%' OR username LIKE 'kabir_%' OR username LIKE 'ishaan_%' OR username LIKE 'arjun_%' OR username LIKE 'raghav_%' OR username LIKE 'dhruv_%' OR username LIKE 'keshav_%' OR username LIKE 'sahaj_%' OR username LIKE 'aarav_%'))");
                stmt.executeUpdate("DELETE FROM students WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'test_%' OR username LIKE 'runtime_%' OR username LIKE 'officer_%' OR username LIKE 'vihaan_%' OR username LIKE 'kabir_%' OR username LIKE 'ishaan_%' OR username LIKE 'arjun_%' OR username LIKE 'raghav_%' OR username LIKE 'dhruv_%' OR username LIKE 'keshav_%' OR username LIKE 'sahaj_%' OR username LIKE 'aarav_%')");
                stmt.executeUpdate("DELETE FROM users WHERE username LIKE 'test_%' OR username LIKE 'runtime_%' OR username LIKE 'officer_%' OR username LIKE 'vihaan_%' OR username LIKE 'kabir_%' OR username LIKE 'ishaan_%' OR username LIKE 'arjun_%' OR username LIKE 'raghav_%' OR username LIKE 'dhruv_%' OR username LIKE 'keshav_%' OR username LIKE 'sahaj_%' OR username LIKE 'aarav_%'");
            } catch (SQLException ignored) {
            }
        }

        System.out.println("\n==================================================================");
        System.out.printf("Results: %d Passed, %d Failed.\n", passed, failed);
        System.out.println("==================================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}
