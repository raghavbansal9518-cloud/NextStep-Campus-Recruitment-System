import com.nextstep.dao.*;
import com.nextstep.database.DatabaseConnection;
import com.nextstep.model.*;
import com.nextstep.service.AuthService;
import com.nextstep.service.DemoService;
import com.nextstep.service.RecruitmentService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Verification Test Suite for Centralized Editable Demo Data Management System
 * and Application-Wide 'A' Shortcut Fetching across NextStep.
 */
public class CentralizedDemoManagerTestRunner {

    private static int passed = 0;
    private static int failed = 0;

    private static void pass(String msg) {
        System.out.println("  [PASS] " + msg);
        passed++;
    }

    private static void fail(String code, String reason) {
        System.out.println("  [FAIL] " + code + " -> " + reason);
        failed++;
    }

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("   NextStep — Centralized Demo Manager & Shortcut Test Suite      ");
        System.out.println("==================================================================");

        PrintStream originalOut = System.out;
        java.io.InputStream originalIn = System.in;

        // Ensure officer activation code is configured
        System.setProperty(AuthService.ACTIVATION_CODE_PROP, DemoService.DEMO_OFFICER_ACTIVATION_CODE);

        DemoService demoService = new DemoService();
        UserDAO userDAO = new UserDAO();
        StudentDAO studentDAO = new StudentDAO();
        JobDAO jobDAO = new JobDAO();
        ApplicationDAO appDAO = new ApplicationDAO();
        CompanyDAO companyDAO = new CompanyDAO();
        AuthService authService = new AuthService();
        RecruitmentService recruitmentService = new RecruitmentService();

        try {
            // Setup demo baseline in database
            demoService.prepareDemoData();

            // =================================================================
            // SUITE 1: CENTRALIZED PROFILE INITIALIZATION & RETRIEVAL
            // =================================================================
            System.out.println("\n--- 1. CENTRALIZED DEMO PROFILES INITIALIZATION ---");
            {
                // Test CP.1: 3 Pre-configured Student Demonstration Profiles
                List<DemoService.DemoStudentProfile> profiles = demoService.getDemoStudentProfiles();
                if (profiles != null && profiles.size() == 3) {
                    DemoService.DemoStudentProfile pA = profiles.get(0);
                    DemoService.DemoStudentProfile pB = profiles.get(1);
                    DemoService.DemoStudentProfile pC = profiles.get(2);

                    boolean pAOk = "Vihaan Mehta".equals(pA.getFullName()) && "2510990750".equals(pA.getStudentId()) && pA.getCgpa() == 8.75;
                    boolean pBOk = "Kabir Malhotra".equals(pB.getFullName()) && "2510990751".equals(pB.getStudentId()) && pB.getActiveBacklogs() == 0;
                    boolean pCOk = "Arjun Khanna".equals(pC.getFullName()) && "2510990752".equals(pC.getStudentId()) && pC.getActiveBacklogs() == 3;

                    if (pAOk && pBOk && pCOk) {
                        pass("CP.1: DemoService maintains 3 distinct candidate profiles (Scenario A, B, C)");
                    } else {
                        fail("CP.1", "Candidate profiles have incorrect baseline attributes");
                    }
                } else {
                    fail("CP.1", "Failed to retrieve 3 demo student profiles");
                }
            }

            {
                // Test CP.2: Placement Officer Demonstration Profile
                DemoService.DemoOfficerProfile officer = demoService.getDemoOfficerProfile();
                if (officer != null &&
                    "officer_vihaan".equals(officer.getUsername()) &&
                    "Prof. Vihaan Mehta".equals(officer.getFullName()) &&
                    "TPO-2026-001".equals(officer.getOfficerId())) {
                    pass("CP.2: Placement Officer demo profile accurately configured with activation code");
                } else {
                    fail("CP.2", "Placement Officer demo profile mismatch");
                }
            }

            {
                // Test CP.3: Demo Job Drive Specifications Profile
                DemoService.DemoJobProfile job = demoService.getDemoJobProfile();
                if (job != null &&
                    job.getTitle() != null &&
                    job.getCtcLpa() != null &&
                    job.getLocation() != null &&
                    !job.getRequiredSkills().isEmpty()) {
                    pass("CP.3: Demo Job Drive profile accurately configured with corporate criteria");
                } else {
                    fail("CP.3", "Demo Job Drive profile mismatch");
                }
            }

            // =================================================================
            // SUITE 2: CENTRALIZED PROFILE EDITING & CANONICAL MYSQL PERSISTENCE
            // =================================================================
            System.out.println("\n--- 2. CANONICAL MYSQL PERSISTENCE & EDITING ---");
            {
                // Test PE.1: Edit Student Profile A and persist changes to MySQL
                DemoService.DemoStudentProfile pA = demoService.getDemoStudentProfile(0);
                String originalPhone = pA.getPhone();
                String updatedPhone = "9988776655";
                pA.setPhone(updatedPhone);

                boolean saved = demoService.saveStudentProfile(pA);
                Student inDb = studentDAO.findByStudentId(pA.getStudentId());

                boolean persisted = saved && inDb != null && updatedPhone.equals(inDb.getPhone());
                // Revert
                pA.setPhone(originalPhone);
                demoService.saveStudentProfile(pA);

                if (persisted) {
                    pass("PE.1: Student profile edit saved canonically to MySQL and verified via StudentDAO");
                } else {
                    fail("PE.1", "Student profile edit failed to persist in MySQL");
                }
            }

            {
                // Test PE.2: Edit Officer Profile and persist to MySQL
                DemoService.DemoOfficerProfile officer = demoService.getDemoOfficerProfile();
                String originalEmail = officer.getEmail();
                String updatedEmail = "updated.tpo@university.edu";
                officer.setEmail(updatedEmail);

                boolean saved = demoService.saveOfficerProfile(officer);
                User inDb = userDAO.findByUsername(officer.getUsername());

                boolean persisted = saved && inDb != null && updatedEmail.equals(inDb.getEmail());
                // Revert
                officer.setEmail(originalEmail);
                demoService.saveOfficerProfile(officer);

                if (persisted) {
                    pass("PE.2: Officer profile edit saved canonically to MySQL and verified via UserDAO");
                } else {
                    fail("PE.2", "Officer profile edit failed to persist in MySQL");
                }
            }

            // =================================================================
            // SUITE 3: APPLICATION-WIDE 'A' SHORTCUT ON STUDENT SIGN IN (PAGE 3)
            // =================================================================
            System.out.println("\n--- 3. APPLICATION-WIDE 'A' SHORTCUT ON STUDENT SIGN IN ---");
            {
                // Test AS.1: 'A' on Student Sign In populates Scenario A (vihaan_demo) and signs in with real PBKDF2
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "1",               // Gateway -> Student Portal
                        "2",               // Student Menu -> Page 3 (Sign In)
                        "A",               // Shortcut 'A': Auto-fetch Scenario A credentials
                        "1",               // 1. SIGN IN (Submit form explicitly)
                        "0",               // Hub -> Sign Out
                        "0",               // Student Menu -> Back
                        "0"                // Gateway -> Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                com.nextstep.Main app = new com.nextstep.Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                boolean hasFetched = outStr.contains("vihaan_demo");
                boolean loggedIn = outStr.contains("PLACEMENT HUB") && outStr.contains("Vihaan Mehta");

                if (hasFetched && loggedIn) {
                    pass("AS.1: Shortcut 'A' on Student Sign In populates Scenario A and authenticates via PBKDF2 into Student Hub");
                } else {
                    fail("AS.1", "Shortcut 'A' failed to populate or authenticate student: hasFetched=" + hasFetched + ", loggedIn=" + loggedIn);
                }
            }

            {
                // Test AS.2: 'A2' on Student Sign In populates Scenario B (kabir_demo)
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "1",               // Gateway -> Student Portal
                        "2",               // Student Menu -> Page 3 (Sign In)
                        "A2",              // Shortcut 'A2': Auto-fetch Scenario B credentials
                        "1",               // 1. SIGN IN
                        "0",               // Hub -> Sign Out
                        "0",               // Student Menu -> Back
                        "0"                // Gateway -> Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                com.nextstep.Main app = new com.nextstep.Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                boolean hasFetchedB = outStr.contains("kabir_demo");
                boolean loggedInB = outStr.contains("PLACEMENT HUB") && outStr.contains("Kabir Malhotra");

                if (hasFetchedB && loggedInB) {
                    pass("AS.2: Shortcut 'A2' on Student Sign In populates Scenario B (Kabir Malhotra) and authenticates into Hub");
                } else {
                    fail("AS.2", "Shortcut 'A2' failed to populate or authenticate Scenario B: hasFetchedB=" + hasFetchedB + ", loggedInB=" + loggedInB);
                }
            }

            {
                // Test AS.3: 'A3' on Student Sign In populates Scenario C (arjun_demo)
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "1",               // Gateway -> Student Portal
                        "2",               // Student Menu -> Page 3 (Sign In)
                        "A3",              // Shortcut 'A3': Auto-fetch Scenario C credentials
                        "1",               // 1. SIGN IN
                        "0",               // Hub -> Sign Out
                        "0",               // Student Menu -> Back
                        "0"                // Gateway -> Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                com.nextstep.Main app = new com.nextstep.Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                boolean hasFetchedC = outStr.contains("arjun_demo");
                boolean loggedInC = outStr.contains("PLACEMENT HUB") && outStr.contains("Arjun Khanna");

                if (hasFetchedC && loggedInC) {
                    pass("AS.3: Shortcut 'A3' on Student Sign In populates Scenario C (Arjun Khanna) and authenticates into Hub");
                } else {
                    fail("AS.3", "Shortcut 'A3' failed to populate or authenticate Scenario C: hasFetchedC=" + hasFetchedC + ", loggedInC=" + loggedInC);
                }
            }

            // =================================================================
            // SUITE 4: APPLICATION-WIDE 'A' SHORTCUT ON OFFICER SIGN IN
            // =================================================================
            System.out.println("\n--- 4. APPLICATION-WIDE 'A' SHORTCUT ON OFFICER SIGN IN ---");
            {
                // Test AO.1: 'A' on Officer Sign In populates officer_vihaan and logs in
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "2",               // Gateway -> Placement Officer Portal
                        "2",               // Officer Menu -> Option 2 (Sign In)
                        "A",               // Shortcut 'A': Auto-fetch Placement Officer credentials
                        "1",               // 1. SIGN IN
                        "0",               // Officer Dashboard -> Sign Out
                        "0",               // Officer Menu -> Back
                        "0"                // Gateway -> Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                com.nextstep.Main app = new com.nextstep.Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                boolean hasOfficerFetched = outStr.contains("Populating credentials for Placement Officer (Prof. Vihaan Mehta)") || outStr.contains("officer_vihaan");
                boolean officerIn = outStr.contains("ADMINISTRATION DASHBOARD") || outStr.contains("OFFICER DASHBOARD");

                if (hasOfficerFetched && officerIn) {
                    pass("AO.1: Shortcut 'A' on Officer Sign In populates officer_vihaan and authenticates into Administration Dashboard");
                } else {
                    fail("AO.1", "Shortcut 'A' failed to authenticate placement officer");
                }
            }

            // =================================================================
            // SUITE 5: APPLICATION-WIDE 'A' SHORTCUT ON JOB SELECTION
            // =================================================================
            System.out.println("\n--- 5. APPLICATION-WIDE 'A' SHORTCUT ON JOB SELECTION ---");
            {
                // Test AJ.1: Entering 'A' at Job ID selection in Eligibility Check selects Demo Job 1
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                String inputs = String.join("\n",
                        "1",               // Gateway -> Student Portal
                        "2",               // Student Menu -> Sign In
                        "A",               // Populate Scenario A
                        "1",               // Sign In
                        "2",               // Hub -> 2. Check Job Eligibility & Skill Match Matrix
                        "A",               // Enter 'A' to select Demo Job #1 automatically!
                        "0",               // Part A -> Return to Hub
                        "0",               // Hub -> Sign Out
                        "0",               // Student Menu -> Back
                        "0"                // Gateway -> Exit
                ) + "\n";
                System.setIn(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

                com.nextstep.Main app = new com.nextstep.Main();
                app.run(new Scanner(System.in));
                System.setOut(originalOut);

                String outStr = out.toString(StandardCharsets.UTF_8);
                boolean evaluatedJob1 = outStr.contains("OVERALL RESULT: ELIGIBLE") &&
                                        outStr.contains("Junior Backend Engineer") &&
                                        outStr.contains("100%");

                if (evaluatedJob1) {
                    pass("AJ.1: Shortcut 'A' for Job ID selection selects Demo Job 1 and evaluates eligibility matrix");
                } else {
                    fail("AJ.1", "Shortcut 'A' failed to select Demo Job 1 during eligibility check");
                }
            }

            // =================================================================
            // SUITE 6: APPLICATION REVIEW & STATUS TRACKING IN DEMO MANAGER
            // =================================================================
            System.out.println("\n--- 6. DEMO MANAGER APPLICATION TRACKING ---");
            {
                // Test MT.1: Retrieve demo applications and update status in MySQL
                // First ensure student 1 has an application
                Student vihaan = studentDAO.findByStudentId(DemoService.DEMO_STUDENT_USN);
                if (vihaan != null && !appDAO.hasStudentApplied(vihaan.getId(), 1)) {
                    appDAO.createApplication(new Application(vihaan.getId(), 1));
                }

                List<DemoService.DemoApplicationView> demoApps = demoService.getDemoApplications();
                boolean retrieved = demoApps != null && !demoApps.isEmpty();

                boolean updated = false;
                if (retrieved) {
                    DemoService.DemoApplicationView first = demoApps.get(0);
                    int appId = first.getId();
                    updated = demoService.updateDemoApplicationStatus(appId, ApplicationStatus.SHORTLISTED);
                    Application fromDb = appDAO.findById(appId);
                    updated = updated && fromDb != null && fromDb.getStatus() == ApplicationStatus.SHORTLISTED;
                }

                if (retrieved && updated) {
                    pass("MT.1: Demo Manager Option 7 retrieves demo candidate applications and updates status in MySQL");
                } else {
                    fail("MT.1", "Demo application tracking failed: retrieved=" + retrieved + ", updated=" + updated);
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

            // Re-initialize demo data cleanly
            try {
                new DemoService().prepareDemoData();
            } catch (Exception ignored) {}
        }

        System.out.println("\n==================================================================");
        System.out.printf("Results: %d Passed, %d Failed.\n", passed, failed);
        System.out.println("==================================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}
