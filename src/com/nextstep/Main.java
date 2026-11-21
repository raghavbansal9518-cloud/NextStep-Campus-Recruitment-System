package com.nextstep;

import com.nextstep.dao.ApplicationDAO;
import com.nextstep.dao.CompanyDAO;
import com.nextstep.dao.JobDAO;
import com.nextstep.dao.StudentDAO;
import com.nextstep.dao.UserDAO;
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
import com.nextstep.util.ConsoleTheme;
import com.nextstep.util.ValidationUtil;

import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Main
 * 
 * Interactive Console Application for the NextStep Campus Recruitment System.
 * 
 * Features:
 * - Student registration with multi-table transaction and PBKDF2 hashing.
 * - Secure role-based login and session navigation (Student vs. Placement Officer).
 * - Student job browsing, automated eligibility verification, and application submission.
 * - Student personal application history tracking.
 * - Placement officer application oversight, status updates, and recruitment summaries.
 * - CSV report generation, parsing into ArrayList, and audit file archiving.
 * - Java Streams (filter, map, distinct, sorted, count, anyMatch) & Lambdas.
 * - Collectors (groupingBy, partitioningBy, counting).
 * - Presentation visibility toggles for live academic demos.
 */
public class Main {

    private final AuthService authService;
    private final RecruitmentService recruitmentService;
    private final JobDAO jobDAO;
    private final StudentDAO studentDAO;
    private final ApplicationDAO applicationDAO;
    private final CompanyDAO companyDAO;
    private final UserDAO userDAO;

    public Main() {
        this(new AuthService(), new RecruitmentService(), new JobDAO(),
             new StudentDAO(), new ApplicationDAO(), new CompanyDAO(), new UserDAO());
    }

    public Main(AuthService authService, RecruitmentService recruitmentService,
                JobDAO jobDAO, StudentDAO studentDAO, ApplicationDAO applicationDAO,
                CompanyDAO companyDAO, UserDAO userDAO) {
        this.authService = authService;
        this.recruitmentService = recruitmentService;
        this.jobDAO = jobDAO;
        this.studentDAO = studentDAO;
        this.applicationDAO = applicationDAO;
        this.companyDAO = companyDAO;
        this.userDAO = userDAO;
    }

    public static void main(String[] args) {
        Main app = new Main();
        Scanner scanner = new Scanner(System.in);

        // Optional administrative CLI flag to seed initial placement officer
        if (args != null && args.length > 0 && "--init-officer".equalsIgnoreCase(args[0])) {
            app.handleAdminOfficerSetup(scanner);
        }

        app.run(scanner);
    }

    /**
     * Top-level application loop.
     */
    public void run(Scanner scanner) {
        boolean inApp = true;
        while (inApp) {
            // Welcome Page (Page 1)
            if (!showWelcomePage(scanner)) {
                inApp = false;
                break; // Option 0: Exit application cleanly
            }

            // Account Portal (Page 2)
            boolean inAccountPortal = true;
            while (inAccountPortal) {
                int roleChoice = showAccountPortal(scanner);
                switch (roleChoice) {
                    case 1 -> handleStudentWorkflow(scanner);
                    case 2 -> handlePlacementOfficerWorkflow(scanner);
                    case 0 -> inAccountPortal = false; // Option 0: Back to Welcome Page (Page 1)
                }
            }
        }
    }

    // =========================================================================
    // WELCOME PAGE (PAGE 1) & SCREEN HELPERS
    // =========================================================================

    /**
     * Displays the Welcome Page (Page 1) and handles the initial Continue / Exit choice.
     * 
     * @param scanner active Scanner
     * @return boolean true to Continue, false to Exit cleanly
     */
    public boolean showWelcomePage(Scanner scanner) {
        clearConsole();
        renderWelcomeScreen();

        while (true) {
            System.out.print(ConsoleTheme.prompt("Enter your choice: "));
            if (!scanner.hasNextLine()) {
                return false;
            }
            String input = scanner.nextLine().trim();
            if (System.console() == null) {
                System.out.println(input);
            }
            System.out.println(ConsoleTheme.border("================================================================"));

            if ("1".equals(input)) {
                return true;
            } else if ("0".equals(input)) {
                System.out.println("\nThank you for using NextStep. Goodbye!");
                return false;
            } else {
                System.out.println(ConsoleTheme.warn("[WARN] Invalid choice. Please enter 1 to Continue or 0 to Exit."));
                System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
            }
        }
    }

    /**
     * Renders the Welcome Page console layout matching exact design specifications.
     */
    public void renderWelcomeScreen() {
        System.out.println(ConsoleTheme.border("================================================================"));
        System.out.println(ConsoleTheme.header("           NEXTSTEP | CAMPUS RECRUITMENT SYSTEM (2026)"));
        System.out.println(ConsoleTheme.header("          Automated Eligibility, Skill Matching & Drives"));
        System.out.println(ConsoleTheme.border("================================================================"));
        System.out.println();
        System.out.println(ConsoleTheme.dim("  System Status:  ● ONLINE    |  Partner Drives: Active Open   "));
        System.out.println(ConsoleTheme.dim("  Academic Year:  2025–2026   |  Placement Hub:  Verified      "));
        System.out.println();
        System.out.println(ConsoleTheme.cyan("  ┌────────────────────────────────────────────────────────────┐"));
        System.out.println(ConsoleTheme.cyan("  │") + ConsoleTheme.text("                  YOUR CAREER STARTS HERE                   ") + ConsoleTheme.cyan("│"));
        System.out.println(ConsoleTheme.cyan("  └────────────────────────────────────────────────────────────┘"));
        System.out.println();
        System.out.println(ConsoleTheme.text("                  1. Continue"));
        System.out.println(ConsoleTheme.text("                  0. Exit"));
        System.out.println();
        System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
    }

    /**
     * Clears the console screen if supported by the terminal environment.
     * Provides safe fallback for non-interactive or unsupported terminals.
     */
    public static void clearConsole() {
        try {
            if (ConsoleTheme.isAnsiEnabled()) {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            } else if (System.console() != null && System.getProperty("os.name", "").toLowerCase().contains("win")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else if (System.console() != null) {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception ignored) {
            // Safe fallback
        }
    }

    // =========================================================================
    // ACCOUNT PORTAL (PAGE 2) & ROLE NAVIGATION
    // =========================================================================

    /**
     * Displays Page 2 — Account Portal and prompts the user to select their role.
     * 
     * @param scanner active Scanner
     * @return int chosen option (1 for Student, 2 for Placement Officer, 0 for Back)
     */
    public int showAccountPortal(Scanner scanner) {
        clearConsole();
        renderAccountPortalScreen();

        while (true) {
            System.out.print(ConsoleTheme.prompt("Enter your choice: "));
            if (!scanner.hasNextLine()) {
                return 0;
            }
            String input = scanner.nextLine().trim();
            if (System.console() == null) {
                System.out.println(input);
            }
            System.out.println(ConsoleTheme.border("================================================================"));

            if ("1".equals(input)) {
                return 1;
            } else if ("2".equals(input)) {
                return 2;
            } else if ("0".equals(input)) {
                return 0;
            } else {
                System.out.println(ConsoleTheme.warn("[WARN] Invalid choice. Please enter 0, 1, or 2."));
                System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
            }
        }
    }

    /**
     * Renders the Account Portal (Page 2) console layout matching exact design specifications.
     */
    public void renderAccountPortalScreen() {
        System.out.println(ConsoleTheme.border("================================================================"));
        System.out.println(ConsoleTheme.header("                    NEXTSTEP | ACCOUNT PORTAL"));
        System.out.println(ConsoleTheme.header("          Automated Eligibility, Skill Matching & Drives"));
        System.out.println(ConsoleTheme.border("================================================================"));
        System.out.println();
        System.out.println(ConsoleTheme.dim("  System Status:  ● ONLINE    |  Partner Drives: 3 Open        "));
        System.out.println(ConsoleTheme.dim("  Academic Year:  2025–2026   |  Active Candidates: Verified   "));
        System.out.println();
        System.out.println(ConsoleTheme.cyan("  ┌────────────────────────────────────────────────────────────┐"));
        System.out.println(ConsoleTheme.cyan("  │") + ConsoleTheme.text("           SELECT YOUR PORTAL — SELECT YOUR ROLE            ") + ConsoleTheme.cyan("│"));
        System.out.println(ConsoleTheme.cyan("  └────────────────────────────────────────────────────────────┘"));
        System.out.println();
        System.out.println(ConsoleTheme.text("                  1. Student"));
        System.out.println(ConsoleTheme.text("                  2. Placement Officer"));
        System.out.println(ConsoleTheme.text("                  0. Back"));
        System.out.println();
        System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
    }

    /**
     * Handles the Student registration and authentication workflow.
     * Reuses existing student registration and student login functionality.
     * 
     * @param scanner active Scanner
     */
    private void handleStudentWorkflow(Scanner scanner) {
        boolean inStudentMenu = true;
        while (inStudentMenu) {
            System.out.println("\n---------------- STUDENT MENU ----------------");
            if (PresentationConfig.isStudentRegistrationVisible()) {
                System.out.println("1. Student Registration");
                System.out.println("2. Student Login");
                System.out.println("0. Back");
            } else {
                System.out.println("1. Student Login");
                System.out.println("0. Back");
            }
            System.out.println("----------------------------------------------");

            int choice = readInt(scanner, "Enter your choice: ");

            if (PresentationConfig.isStudentRegistrationVisible()) {
                switch (choice) {
                    case 1 -> handleStudentRegistration(scanner);
                    case 2 -> {
                        boolean signedIn = showStudentSignIn(scanner);
                        if (signedIn) {
                            inStudentMenu = false; // Sign out returns to Account Portal (Page 2)
                        }
                    }
                    case 0 -> inStudentMenu = false;
                    default -> System.out.println("[WARN] Invalid option. Please try again.");
                }
            } else {
                switch (choice) {
                    case 1 -> {
                        boolean signedIn = showStudentSignIn(scanner);
                        if (signedIn) {
                            inStudentMenu = false; // Sign out returns to Account Portal (Page 2)
                        }
                    }
                    case 0 -> inStudentMenu = false;
                    default -> System.out.println("[WARN] Invalid option. Please try again.");
                }
            }
        }
    }

    /**
     * Handles the Placement Officer authentication workflow.
     * Displays a menu for Officer Sign Up, Officer Sign In, or Back to Account Portal.
     * Reuses existing authentication, first-run provisioning, and role verification.
     * 
     * @param scanner active Scanner
     */
    private void handlePlacementOfficerWorkflow(Scanner scanner) {
        checkFirstRunOfficerSetup(scanner);

        boolean inOfficerMenu = true;
        while (inOfficerMenu) {
            System.out.println("\n----------- PLACEMENT OFFICER MENU -----------");
            System.out.println("1. Officer Sign Up");
            System.out.println("2. Officer Sign In");
            System.out.println("0. Back");
            System.out.println("----------------------------------------------");

            int choice = readInt(scanner, "Enter your choice: ");

            switch (choice) {
                case 1 -> showOfficerSignUp(scanner);
                case 2 -> {
                    boolean loggedIn = handleLogin(scanner, Role.PLACEMENT_OFFICER);
                    if (loggedIn) {
                        inOfficerMenu = false; // Sign out returns to Account Portal (Page 2)
                    }
                }
                case 0 -> inOfficerMenu = false;
                default -> System.out.println("[WARN] Invalid option. Please try again.");
            }
        }
    }

    // AUTHENTICATION & REGISTRATION FLOWS
    // =========================================================================

    // =========================================================================
    // STUDENT SIGN UP (PAGE 3)
    // =========================================================================

    /**
     * Displays Page 3 — Student Sign Up form and manages interactive registration.
     * 
     * @param scanner active Scanner
     * @return boolean true if account was created, false if cancelled/returned
     */
    public boolean showStudentSignUp(Scanner scanner) {
        String[] values = new String[12];
        while (true) {
            clearConsole();
            renderStudentSignUpHeader();
            System.out.println(ConsoleTheme.dim("[Form Shortcuts: 'B' = Previous Field | 'N' = Next Field] (Esc / Tab / Enter supported)"));
            System.out.println(ConsoleTheme.dim("[A] Auto-Fill Demo Data — Enter 'A' at any prompt to populate all fields with valid sample data"));
            System.out.println();

            String[] prompts = {
                    "Student ID:             ",
                    "Full Name:              ",
                    "Email:                  ",
                    "Phone Number:           ",
                    "Branch:                 ",
                    "CGPA:                   ",
                    "Technical Skills:       ",
                    "Active Backlogs:        ",
                    "Graduation Year:        ",
                    "Username:               ",
                    "Password:               ",
                    "Confirm Password:       "
            };
            boolean[] isPassword = {
                    false, false, false, false, false, false, false, false, false, false, true, true
            };
            int[] blankLines = { 8 };

            String[] demoData = getStudentDemoData();
            boolean completed = fillMultiFieldForm(scanner, prompts, isPassword, blankLines, values, demoData);
            if (!completed) {
                return false;
            }

            String studentId = values[0];
            String fullName = values[1];
            String email = values[2];
            String phone = values[3];
            String branch = values[4];
            String cgpaStr = values[5];
            String skillsStr = values[6];
            String backlogsStr = values[7];
            String gradYearStr = values[8];
            String username = values[9];
            String password = values[10];
            String confirmPassword = values[11];

            renderStudentSignUpFooter();

            while (true) {
                System.out.print(ConsoleTheme.prompt("Enter your choice: "));
                if (!scanner.hasNextLine()) {
                    return false;
                }
                String choice = scanner.nextLine().trim();
                if (System.console() == null) {
                    System.out.println(choice);
                }
                System.out.println(ConsoleTheme.border("================================================================"));

                if ("1".equals(choice)) {
                    boolean created = processStudentRegistration(
                            studentId, fullName, email, phone, branch, cgpaStr,
                            skillsStr, backlogsStr, gradYearStr,
                            username, password, confirmPassword
                    );
                    if (created) {
                        return true;
                    } else {
                        System.out.print("\nPress Enter to try again, or enter 0 to return to Student Menu: ");
                        if (!scanner.hasNextLine()) {
                            return false;
                        }
                        String retry = scanner.nextLine().trim();
                        if (System.console() == null) {
                            System.out.println(retry);
                        }
                        if ("0".equals(retry)) {
                            return false;
                        }
                        // Reset sensitive password fields on retry, keep filled demographic fields
                        values[10] = "";
                        values[11] = "";
                        break; // re-run outer loop to show form again
                    }
                } else if ("0".equals(choice)) {
                    return false; // return to Student Menu
                } else {
                    System.out.println(ConsoleTheme.warn("[WARN] Invalid choice. Please enter 1 to Create Account or 0 to Back."));
                    System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
                }
            }
        }
    }

    /**
     * Renders header for Page 3 — Student Sign Up matching exact design specifications.
     */
    public void renderStudentSignUpHeader() {
        System.out.println(ConsoleTheme.border("================================================================"));
        System.out.println(ConsoleTheme.header("                    NEXTSTEP | STUDENT PORTAL"));
        System.out.println(ConsoleTheme.header("                       STUDENT SIGN UP"));
        System.out.println(ConsoleTheme.border("================================================================"));
        System.out.println();
    }

    /**
     * Renders footer and action options for Page 3 — Student Sign Up.
     */
    public void renderStudentSignUpFooter() {
        System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
        System.out.println(ConsoleTheme.text("                  1. CREATE ACCOUNT"));
        System.out.println(ConsoleTheme.text("                  0. BACK"));
        System.out.println(ConsoleTheme.border("================================================================"));
    }

    /**
     * Interactively fills a multi-field form supporting Back ('B'), Forward ('N'), and Auto-Fill ('A') shortcuts.
     * 
     * @param scanner active Scanner
     * @param prompts array of field prompts (e.g. "Student ID:             ")
     * @param isPassword array indicating whether each field is masked
     * @param blankLines indices after which a blank line should be printed
     * @param values pre-existing or collected values for each field
     * @return true if all fields were collected successfully, false if input stream terminated
     */
    private boolean fillMultiFieldForm(Scanner scanner, String[] prompts, boolean[] isPassword, int[] blankLines, String[] values) {
        return fillMultiFieldForm(scanner, prompts, isPassword, blankLines, values, null);
    }

    private boolean fillMultiFieldForm(Scanner scanner, String[] prompts, boolean[] isPassword, int[] blankLines,
                                       String[] values, String[] demoData) {
        int i = 0;
        int n = prompts.length;
        while (i < n) {
            String prompt = prompts[i];
            boolean masked = isPassword != null && i < isPassword.length && isPassword[i];
            String currentVal = (values[i] != null) ? values[i] : "";

            if (!currentVal.isEmpty() && !masked) {
                System.out.println(ConsoleTheme.dim("  [Current: " + currentVal + " — Enter to keep, or type new value]"));
            }
            System.out.print(ConsoleTheme.prompt(prompt));

            String input;
            if (masked) {
                if (System.console() != null) {
                    char[] pass = System.console().readPassword();
                    input = (pass != null) ? new String(pass).trim() : "";
                } else {
                    if (!scanner.hasNextLine()) {
                        return false;
                    }
                    input = scanner.nextLine().trim();
                    System.out.println(); // newline only; never echo plaintext password
                }
            } else {
                if (!scanner.hasNextLine()) {
                    return false;
                }
                input = scanner.nextLine().trim();
                if (System.console() == null) {
                    System.out.println(input);
                }
            }

            // Shortcut 'A' / 'a' / 'AUTO': Auto-fill all fields with valid demo data
            if (input.equalsIgnoreCase("A") || input.equalsIgnoreCase("AUTO") || input.equalsIgnoreCase("AUTOFILL")) {
                if (demoData == null) {
                    System.out.println();
                    System.out.println(ConsoleTheme.warn("[WARN] Autofill is unavailable: verified demo data could not be generated."));
                    System.out.println(ConsoleTheme.dim("  Please enter details manually for each field, or enter 0 at action prompt to cancel."));
                    continue;
                }
                for (int k = 0; k < n && k < demoData.length; k++) {
                    if (demoData[k] != null) {
                        values[k] = demoData[k];
                    }
                }
                System.out.println();
                System.out.println(ConsoleTheme.success("[AUTO-FILL] Form populated with valid demo data:"));
                System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
                boolean hasUnsetActivation = false;
                for (int k = 0; k < n; k++) {
                    boolean maskedField = isPassword != null && k < isPassword.length && isPassword[k];
                    String rawVal;
                    String styledVal;
                    if (prompts[k].contains("Activation Code") && (values[k] == null || values[k].trim().isEmpty())) {
                        rawVal = "[NOT CONFIGURED — ENTER MANUALLY]";
                        styledVal = ConsoleTheme.warn(rawVal);
                        hasUnsetActivation = true;
                    } else {
                        rawVal = maskedField ? "********" : values[k];
                        styledVal = ConsoleTheme.cyan("[ " + rawVal + " ]");
                    }
                    String fieldLabel = prompts[k].trim().replaceAll(":$", "");
                    System.out.printf("  %s %s%n", ConsoleTheme.prompt(String.format("%-23s:", fieldLabel)), styledVal);
                }
                System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
                if (hasUnsetActivation) {
                    System.out.println();
                    System.out.println(ConsoleTheme.warn("  [NOTICE] Officer activation code is not configured in system environment."));
                    System.out.println(ConsoleTheme.dim("  (Set property '" + AuthService.ACTIVATION_CODE_PROP + "' or env '" + AuthService.ACTIVATION_CODE_ENV + "')."));
                    System.out.println(ConsoleTheme.dim("  Activation code was NOT auto-filled with any default secret; enter it manually to proceed."));
                }
                System.out.println();
                System.out.println(ConsoleTheme.dim("  All fields auto-filled! Press Enter to accept, or enter field number (1-" + n + ") to edit:"));

                if (!scanner.hasNextLine()) {
                    return false;
                }
                String postAuto = scanner.nextLine().trim();
                if (System.console() == null) {
                    System.out.println(postAuto);
                }
                if (postAuto.isEmpty() || postAuto.equalsIgnoreCase("Y") || postAuto.equalsIgnoreCase("YES")) {
                    break;
                } else if (postAuto.equalsIgnoreCase("E") || postAuto.equalsIgnoreCase("EDIT")) {
                    i = 0;
                    continue;
                } else {
                    try {
                        int fieldNum = Integer.parseInt(postAuto);
                        if (fieldNum >= 1 && fieldNum <= n) {
                            i = fieldNum - 1;
                            continue;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                    break;
                }
            }

            // Shortcut 'B' / 'b' / Esc / \u001B / \033: Return to previous field
            if (input.equalsIgnoreCase("B") || input.equalsIgnoreCase("ESC") || input.equals("\u001B") || input.equals("\033")) {
                if (i > 0) {
                    i--;
                    System.out.println(ConsoleTheme.dim("[Returned to previous field]"));
                } else {
                    System.out.println(ConsoleTheme.dim("[Already at first field]"));
                }
                continue;
            }

            // Shortcut 'N' / 'n' / Tab / \t: Move forward to next field
            if (input.equalsIgnoreCase("N") || input.equalsIgnoreCase("TAB") || input.equals("\t")) {
                if (!currentVal.isEmpty()) {
                    i++;
                } else {
                    System.out.println(ConsoleTheme.red("[WARN] Field is required. Cannot skip empty field."));
                }
                continue;
            }

            // Enter pressed (empty input)
            if (input.isEmpty()) {
                if (!currentVal.isEmpty()) {
                    // Keep existing value and advance
                    i++;
                } else {
                    // Field was empty and user entered empty string -> record empty string and advance
                    values[i] = "";
                    i++;
                }
                if (blankLines != null) {
                    for (int bl : blankLines) {
                        if (bl == i - 1) {
                            System.out.println();
                            break;
                        }
                    }
                }
                continue;
            }

            // Non-empty value entered
            values[i] = input;
            i++;
            if (blankLines != null) {
                for (int bl : blankLines) {
                    if (bl == i - 1) {
                        System.out.println();
                        break;
                    }
                }
            }
        }
        System.out.println(); // Trailing blank line before footer
        return true;
    }

    public String[] getStudentDemoData() {
        String[][] candidates = {
                { "Raghav Bansal", "2510990750", "raghav", "Computer Science", "8.75", "Java, Spring Boot, SQL, Git, Docker" },
                { "Dhruv Sharma",  "2510990751", "dhruv",  "Information Technology", "8.50", "Java, Python, SQL, Git, Linux" },
                { "Keshav Gupta",  "2510990752", "keshav", "Computer Science", "8.90", "Java, SQL, React, Node.js, Git" },
                { "Sahaj Patel",   "2510990753", "sahaj",  "Electronics & Communication", "8.20", "Python, C++, Embedded Systems, Linux" }
        };

        int candIndex = 0;
        String name = candidates[0][0];
        String studentId = candidates[0][1];
        String username = candidates[0][2] + "_2510";
        String email = candidates[0][2] + "." + studentId + "@university.edu";
        String branch = candidates[0][3];
        String cgpa = candidates[0][4];
        String skills = candidates[0][5];

        int attempts = 0;
        boolean verified = false;

        while (attempts < 1000) {
            try {
                boolean idExists = (studentDAO != null) && studentDAO.existsByStudentId(studentId);
                boolean userExists = (userDAO != null) && userDAO.existsByUsername(username);
                boolean emailExists = (userDAO != null) && userDAO.existsByEmail(email);
                if (!idExists && !userExists && !emailExists) {
                    verified = true;
                    break;
                }
            } catch (SQLException e) {
                System.out.println();
                System.out.println(ConsoleTheme.red("[ERROR] Demo data could not be verified due to database error: " + e.getMessage()));
                System.out.println(ConsoleTheme.dim("  Autofill stopped safely. Please enter details manually or cancel."));
                return null;
            }
            candIndex++;
            if (candIndex < candidates.length) {
                name = candidates[candIndex][0];
                studentId = candidates[candIndex][1];
                username = candidates[candIndex][2] + "_2510";
                email = candidates[candIndex][2] + "." + studentId + "@university.edu";
                branch = candidates[candIndex][3];
                cgpa = candidates[candIndex][4];
                skills = candidates[candIndex][5];
            } else {
                long nextId = 2510990750L + candIndex;
                studentId = String.valueOf(nextId);
                username = "raghav_" + (2510 + (candIndex % 9000));
                email = "raghav." + studentId + "@university.edu";
                name = "Raghav Bansal";
                branch = "Computer Science";
                cgpa = "8.75";
                skills = "Java, Spring Boot, SQL, Git, Docker";
            }
            attempts++;
        }

        if (!verified) {
            System.out.println();
            System.out.println(ConsoleTheme.red("[ERROR] Could not generate unique demo identity: maximum attempts reached (1000 retries)."));
            System.out.println(ConsoleTheme.dim("  Autofill stopped safely. Please enter details manually or cancel."));
            return null;
        }

        return new String[] {
                studentId,
                name,
                email,
                "9876543210",
                branch,
                cgpa,
                skills,
                "0",
                "2026",
                username,
                "Password@123",
                "Password@123"
        };
    }

    public String[] getOfficerDemoData() {
        int seed = (int) (System.currentTimeMillis() % 900) + 100;
        String officerId = "OFF-2026-" + seed;
        String username = "officer_raghav";
        String email = "tpo.raghav@university.edu";
        int attempts = 0;
        boolean verified = false;

        while (attempts < 1000) {
            try {
                boolean userExists = (userDAO != null) && userDAO.existsByUsername(username);
                boolean emailExists = (userDAO != null) && userDAO.existsByEmail(email);
                if (!userExists && !emailExists) {
                    verified = true;
                    break;
                }
            } catch (SQLException e) {
                System.out.println();
                System.out.println(ConsoleTheme.red("[ERROR] Officer demo data could not be verified due to database error: " + e.getMessage()));
                System.out.println(ConsoleTheme.dim("  Autofill stopped safely. Please enter details manually or cancel."));
                return null;
            }
            seed = (seed + 1 > 9999) ? 100 : seed + 1;
            officerId = "OFF-2026-" + seed;
            username = "officer_raghav_" + seed;
            email = "tpo.raghav." + seed + "@university.edu";
            attempts++;
        }

        if (!verified) {
            System.out.println();
            System.out.println(ConsoleTheme.red("[ERROR] Could not generate unique officer demo identity: maximum attempts reached (1000 retries)."));
            System.out.println(ConsoleTheme.dim("  Autofill stopped safely. Please enter details manually or cancel."));
            return null;
        }

        String actCode = System.getProperty(AuthService.ACTIVATION_CODE_PROP);
        if (actCode == null || actCode.trim().isEmpty()) {
            actCode = System.getenv(AuthService.ACTIVATION_CODE_ENV);
        }
        String resolvedCode = (actCode != null && !actCode.trim().isEmpty()) ? actCode.trim() : "";

        return new String[] {
                officerId,
                "Prof. Raghav Bansal",
                email,
                username,
                "Officer@123",
                "Officer@123",
                resolvedCode
        };
    }

    private String[] getJobDemoData(List<Company> companies) {
        String companyId = (companies != null && !companies.isEmpty())
                ? String.valueOf(companies.get(0).getId())
                : "1";
        String location = (companies != null && !companies.isEmpty() && companies.get(0).getLocation() != null)
                ? companies.get(0).getLocation()
                : "Bengaluru, KA";
        return new String[] {
                companyId,
                "Associate Cloud Engineer",
                "Design, build, and maintain cloud infrastructure on AWS and GCP.",
                "12.50",
                location,
                "7.20",
                "0",
                "Computer Science, Information Technology",
                "Java, SQL, Linux, Git, Docker",
                "2026"
        };
    }

    /**
     * Reads a field with prompt, echoing input in non-interactive / automated environments.
     */
    private String readField(Scanner scanner, String prompt) {
        System.out.print(ConsoleTheme.prompt(prompt));
        if (!scanner.hasNextLine()) {
            return "";
        }
        String input = scanner.nextLine().trim();
        if (System.console() == null) {
            System.out.println(input);
        }
        return input;
    }

    /**
     * Reads a password without displaying characters when console is present,
     * suppressing plaintext echo in non-interactive test environments.
     */
    private String readPasswordField(Scanner scanner, String prompt) {
        System.out.print(ConsoleTheme.prompt(prompt));
        if (System.console() != null) {
            char[] pass = System.console().readPassword();
            return (pass != null) ? new String(pass).trim() : "";
        } else {
            if (!scanner.hasNextLine()) {
                return "";
            }
            String input = scanner.nextLine().trim();
            System.out.println(); // newline only; never echo plaintext password
            return input;
        }
    }

    /**
     * Validates all form inputs and executes student account registration.
     */
    private boolean processStudentRegistration(String studentId, String fullName, String email,
                                               String phone, String branch, String cgpaStr,
                                               String skillsStr, String backlogsStr, String gradYearStr,
                                               String username, String password, String confirmPassword) {
        // 1. Password Confirmation Check
        if (!password.equals(confirmPassword)) {
            System.out.println("\n[ERROR] Password and Confirm Password do not match.");
            return false;
        }

        // 2. Non-empty field validations
        if (ValidationUtil.isNullOrBlank(studentId)) {
            System.out.println("\n[ERROR] Student ID / USN cannot be empty.");
            return false;
        }
        if (ValidationUtil.isNullOrBlank(fullName)) {
            System.out.println("\n[ERROR] Full name cannot be empty.");
            return false;
        }
        if (!ValidationUtil.isValidEmail(email)) {
            System.out.println("\n[ERROR] Invalid email format. (Must contain '@' and a domain).");
            return false;
        }
        if (ValidationUtil.isNullOrBlank(phone)) {
            System.out.println("\n[ERROR] Phone number cannot be empty.");
            return false;
        }
        if (!ValidationUtil.isValidPhone(phone)) {
            System.out.println("\n[ERROR] Invalid phone number. Must be exactly 10 digits.");
            return false;
        }
        if (ValidationUtil.isNullOrBlank(branch)) {
            System.out.println("\n[ERROR] Branch cannot be empty.");
            return false;
        }

        List<String> skills = ValidationUtil.parseCommaSeparatedList(skillsStr);
        if (skills.isEmpty()) {
            System.out.println("\n[ERROR] Technical skills cannot be empty. Please enter at least one valid skill (e.g. Java, SQL, Git).");
            return false;
        }

        // 3. Numeric range validations
        double cgpa;
        try {
            cgpa = Double.parseDouble(cgpaStr);
        } catch (Exception e) {
            System.out.println("\n[ERROR] Invalid CGPA: must be a valid numeric value between 0.00 and 10.00.");
            return false;
        }
        if (!ValidationUtil.isValidCgpa(cgpa)) {
            System.out.println("\n[ERROR] CGPA must be between 0.00 and 10.00.");
            return false;
        }

        int activeBacklogs;
        try {
            activeBacklogs = Integer.parseInt(backlogsStr);
        } catch (Exception e) {
            System.out.println("\n[ERROR] Invalid Active Backlogs: must be a valid integer greater than or equal to zero.");
            return false;
        }
        if (!ValidationUtil.isValidBacklogs(activeBacklogs)) {
            System.out.println("\n[ERROR] Active backlogs cannot be negative.");
            return false;
        }

        int graduationYear;
        try {
            graduationYear = Integer.parseInt(gradYearStr);
        } catch (Exception e) {
            System.out.println("\n[ERROR] Invalid Graduation Year: must be a valid 4-digit year.");
            return false;
        }
        if (!ValidationUtil.isValidGraduationYear(graduationYear)) {
            System.out.println("\n[ERROR] Graduation year must be between 2020 and 2035.");
            return false;
        }

        // 4. Credential format validations
        if (!ValidationUtil.isValidUsername(username)) {
            System.out.println("\n[ERROR] Username must be between 3 and 30 alphanumeric characters.");
            return false;
        }
        if (!ValidationUtil.isValidPassword(password)) {
            System.out.println("\n[ERROR] Password must be at least 6 characters.");
            return false;
        }

        // 5. Transactional Persistence via AuthService (Calling the 11-parameter overload)
        try {
            Student registered = authService.registerStudent(
                    username, password, email, studentId, fullName, phone,
                    branch, cgpa, activeBacklogs, graduationYear, skills
            );
            System.out.println("\n[SUCCESS] Account created successfully for " + registered.getFullName() + "!");
            System.out.println("You can now log in using your username and password.");
            return true;
        } catch (IllegalArgumentException e) {
            System.out.println("\n[ERROR] Registration validation failed: " + e.getMessage());
            return false;
        } catch (SQLException e) {
            System.out.println("\n[ERROR] Database error during registration: " + e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // STUDENT SIGN IN (PAGE 4)
    // =========================================================================

    /**
     * Displays Page 4 — Student Sign In and executes student authentication.
     * Supports both Username and Student ID (USN).
     * 
     * @param scanner active Scanner
     * @return boolean true if authenticated and portal session completed, false if cancelled/returned
     */
    public boolean showStudentSignIn(Scanner scanner) {
        String statusMessage = "Please enter your account credentials.";

        while (true) {
            clearConsole();
            renderStudentSignInHeader();

            String identifier = readField(scanner, "Student ID / Username:  ");
            String password = readPasswordField(scanner, "Password:               ");
            System.out.println();

            renderStudentSignInActions();
            System.out.println("Message: " + statusMessage);
            System.out.println(ConsoleTheme.border("================================================================"));

            while (true) {
                System.out.print(ConsoleTheme.prompt("Enter your choice: "));
                if (!scanner.hasNextLine()) {
                    return false;
                }
                String choice = scanner.nextLine().trim();
                if (System.console() == null) {
                    System.out.println(choice);
                }
                System.out.println(ConsoleTheme.border("================================================================"));

                if ("1".equals(choice)) {
                    boolean success = processStudentSignIn(scanner, identifier, password);
                    if (success) {
                        return true;
                    } else {
                        System.out.print("\nPress Enter to try again, or enter 0 to return to Student Menu: ");
                        if (!scanner.hasNextLine()) {
                            return false;
                        }
                        String retry = scanner.nextLine().trim();
                        if (System.console() == null) {
                            System.out.println(retry);
                        }
                        if ("0".equals(retry)) {
                            return false;
                        }
                        statusMessage = "Please check your credentials and try again.";
                        break; // re-display Page 4 form
                    }
                } else if ("0".equals(choice)) {
                    return false; // return to Student Menu
                } else {
                    System.out.println(ConsoleTheme.warn("[WARN] Invalid choice. Please enter 1 to Sign In or 0 to Back."));
                    System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
                }
            }
        }
    }

    /**
     * Renders header for Page 4 — Student Sign In matching exact design specifications.
     */
    public void renderStudentSignInHeader() {
        System.out.println(ConsoleTheme.border("================================================================"));
        System.out.println(ConsoleTheme.header("                    NEXTSTEP | STUDENT PORTAL"));
        System.out.println(ConsoleTheme.header("                        STUDENT SIGN IN"));
        System.out.println(ConsoleTheme.border("================================================================"));
        System.out.println();
    }

    /**
     * Renders actions and separator for Page 4 — Student Sign In.
     */
    public void renderStudentSignInActions() {
        System.out.println(ConsoleTheme.text("                  1. SIGN IN"));
        System.out.println(ConsoleTheme.text("                  0. BACK"));
        System.out.println();
        System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
    }

    /**
     * Authenticates student via Username or Student ID and dispatches to Student Portal.
     */
    private boolean processStudentSignIn(Scanner scanner, String identifier, String password) {
        if (ValidationUtil.isNullOrBlank(identifier) || ValidationUtil.isNullOrBlank(password)) {
            System.out.println("\n[ERROR] Invalid credentials. Student ID / Username and Password cannot be blank.");
            return false;
        }

        try {
            User user = authService.login(identifier, password);
            if (user == null) {
                System.out.println("\n[ERROR] Invalid credentials. Please check your Student ID / Username and password.");
                return false;
            }

            if (user.getRole() != Role.STUDENT) {
                System.out.println("\n[ERROR] Access denied. Placement officer accounts cannot sign in here.");
                return false;
            }

            Student student = studentDAO.findByUserId(user.getId());
            if (student == null) {
                System.out.println("\n[ERROR] Student profile not found for this account.");
                return false;
            }

            System.out.println("\n[SUCCESS] Login successful! Welcome, " + student.getFullName() + ".");
            studentPortal(scanner, student, user);
            return true;
        } catch (SQLException e) {
            System.out.println("\n[ERROR] Database error during sign in: " + e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // PLACEMENT OFFICER SIGN UP (PAGE 5)
    // =========================================================================

    /**
     * Displays Page 5 — Placement Officer Sign Up form and manages interactive registration.
     * 
     * @param scanner active Scanner
     * @return boolean true if account was created, false if cancelled/returned
     */
    public boolean showOfficerSignUp(Scanner scanner) {
        String[] values = new String[7];
        while (true) {
            clearConsole();
            renderOfficerSignUpHeader();
            System.out.println(ConsoleTheme.dim("[Form Shortcuts: 'B' = Previous Field | 'N' = Next Field] (Esc / Tab / Enter supported)"));
            System.out.println(ConsoleTheme.dim("[A] Auto-Fill Demo Data — Enter 'A' at any prompt to populate all fields with valid sample data"));
            System.out.println();

            String[] prompts = {
                    "Officer ID:             ",
                    "Full Name:              ",
                    "Official Email:         ",
                    "Username:               ",
                    "Password:               ",
                    "Confirm Password:       ",
                    "Activation Code:        "
            };
            boolean[] isPassword = {
                    false, false, false, false, true, true, true
            };
            int[] blankLines = { 2 };

            String[] demoData = getOfficerDemoData();
            boolean completed = fillMultiFieldForm(scanner, prompts, isPassword, blankLines, values, demoData);
            if (!completed) {
                return false;
            }

            String officerId = values[0];
            String fullName = values[1];
            String email = values[2];
            String username = values[3];
            String password = values[4];
            String confirmPassword = values[5];
            String activationCode = values[6];

            renderOfficerSignUpFooter();

            while (true) {
                System.out.print(ConsoleTheme.prompt("Enter your choice: "));
                if (!scanner.hasNextLine()) {
                    return false;
                }
                String choice = scanner.nextLine().trim();
                if (System.console() == null) {
                    System.out.println(choice);
                }
                System.out.println(ConsoleTheme.border("================================================================"));

                if ("1".equals(choice)) {
                    boolean created = processOfficerRegistration(
                            officerId, fullName, email,
                            username, password, confirmPassword, activationCode
                    );
                    if (created) {
                        return true;
                    } else {
                        System.out.print("\nPress Enter to try again, or enter 0 to return to Placement Officer Menu: ");
                        if (!scanner.hasNextLine()) {
                            return false;
                        }
                        String retry = scanner.nextLine().trim();
                        if (System.console() == null) {
                            System.out.println(retry);
                        }
                        if ("0".equals(retry)) {
                            return false;
                        }
                        // Reset credentials on retry for safety, retain demographic fields
                        values[4] = "";
                        values[5] = "";
                        values[6] = "";
                        break; // re-run outer loop to show form again
                    }
                } else if ("0".equals(choice)) {
                    return false; // return to Placement Officer Menu
                } else {
                    System.out.println(ConsoleTheme.warn("[WARN] Invalid choice. Please enter 1 to Create Account or 0 to Back."));
                    System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
                }
            }
        }
    }

    /**
     * Renders header for Page 5 — Placement Officer Sign Up matching exact design specifications.
     */
    public void renderOfficerSignUpHeader() {
        System.out.println(ConsoleTheme.border("================================================================"));
        System.out.println(ConsoleTheme.header("              NEXTSTEP | PLACEMENT OFFICER PORTAL"));
        System.out.println(ConsoleTheme.header("                     OFFICER SIGN UP"));
        System.out.println(ConsoleTheme.border("================================================================"));
        System.out.println();
    }

    /**
     * Renders footer and action options for Page 5 — Placement Officer Sign Up.
     */
    public void renderOfficerSignUpFooter() {
        System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
        System.out.println(ConsoleTheme.text("                  1. CREATE ACCOUNT"));
        System.out.println(ConsoleTheme.text("                  0. BACK"));
        System.out.println(ConsoleTheme.border("================================================================"));
    }

    /**
     * Validates all form inputs and executes placement officer account registration.
     */
    private boolean processOfficerRegistration(String officerId, String fullName, String email,
                                               String username, String password, String confirmPassword,
                                               String activationCode) {
        // 1. Password Confirmation Check
        if (!password.equals(confirmPassword)) {
            System.out.println("\n[ERROR] Password and Confirm Password do not match.");
            return false;
        }

        // 2. Non-empty field validations
        if (ValidationUtil.isNullOrBlank(officerId)) {
            System.out.println("\n[ERROR] Officer ID cannot be empty.");
            return false;
        }
        if (ValidationUtil.isNullOrBlank(fullName)) {
            System.out.println("\n[ERROR] Full name cannot be empty.");
            return false;
        }
        if (!ValidationUtil.isValidEmail(email)) {
            System.out.println("\n[ERROR] Invalid official email format. (Must contain '@' and a domain).");
            return false;
        }
        if (!ValidationUtil.isValidUsername(username)) {
            System.out.println("\n[ERROR] Username must be between 3 and 30 alphanumeric characters.");
            return false;
        }
        if (!ValidationUtil.isValidPassword(password)) {
            System.out.println("\n[ERROR] Password must be at least 6 characters.");
            return false;
        }

        // 3. Activation Code verification
        if (!authService.isOfficerActivationConfigured()) {
            System.out.println("\n[ERROR] Configuration error: Officer registration is disabled.");
            System.out.println("System administrator has not configured an activation code.");
            System.out.println("Please set '-Dnextstep.officer.activation.code=<CODE>' or environment variable 'NEXTSTEP_OFFICER_ACTIVATION_CODE'.");
            return false;
        }
        if (!authService.verifyOfficerActivationCode(activationCode)) {
            System.out.println("\n[ERROR] Invalid activation code. Placement officer registration unauthorized.");
            return false;
        }

        // 4. Attempt registration via AuthService
        try {
            User officer = authService.registerPlacementOfficer(
                    officerId, fullName, email,
                    username, password, activationCode
            );
            System.out.println("\n================================================================");
            System.out.println("[SUCCESS] Placement Officer account created successfully for '" + officer.getUsername() + "'!");
            System.out.println("Official Name: " + fullName);
            System.out.println("Official Email: " + email);
            System.out.println("You can now sign in using option 2 (Officer Sign In).");
            System.out.println("================================================================");
            return true;
        } catch (IllegalStateException e) {
            System.out.println("\n[ERROR] Configuration error: " + e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            System.out.println("\n[ERROR] Registration validation failed: " + e.getMessage());
            return false;
        } catch (SQLException e) {
            System.out.println("\n[ERROR] Database error during registration: " + e.getMessage());
            return false;
        }
    }

    public boolean handleOfficerRegistration(Scanner scanner) {
        return showOfficerSignUp(scanner);
    }

    public boolean handleStudentRegistration(Scanner scanner) {
        return showStudentSignUp(scanner);
    }

    public boolean handleLogin(Scanner scanner) {
        return handleLogin(scanner, null);
    }

    public boolean handleLogin(Scanner scanner, Role expectedRole) {
        if (expectedRole == Role.STUDENT) {
            return showStudentSignIn(scanner);
        }
        String roleHeader = expectedRole == null ? "USER LOGIN" : "PLACEMENT OFFICER LOGIN";
        System.out.println("\n--- " + roleHeader + " ---");
        String username = readField(scanner, "Username:               ");
        String password = readPasswordField(scanner, "Password:               ");

        try {
            User user = authService.login(username, password);
            if (user == null) {
                System.out.println("\n[ERROR] Invalid credentials. Please check your username and password.");
                return false;
            }

            if (expectedRole != null && user.getRole() != expectedRole) {
                String roleName = expectedRole == Role.STUDENT ? "Student" : "Placement Officer";
                System.out.println("\n[ERROR] Access denied. This account does not have " + roleName + " privileges.");
                return false;
            }

            if (user.getRole() == Role.STUDENT) {
                Student student = studentDAO.findByUserId(user.getId());
                if (student == null) {
                    System.out.println("\n[ERROR] Student profile not found for this account.");
                    return false;
                }
                System.out.println("\n[SUCCESS] Login successful! Welcome, " + student.getFullName() + ".");
                studentPortal(scanner, student, user);
                return true;
            } else if (user.getRole() == Role.PLACEMENT_OFFICER) {
                System.out.println("\n[SUCCESS] Login successful! Welcome, Placement Officer " + user.getUsername() + ".");
                placementOfficerPortal(scanner, user);
                return true;
            } else {
                System.out.println("\n[ERROR] Unrecognized user role: " + user.getRole());
                return false;
            }
        } catch (SQLException e) {
            System.out.println("\n[ERROR] Database error during login: " + e.getMessage());
            return false;
        }
    }

    private void checkFirstRunOfficerSetup(Scanner scanner) {
        try {
            if (!authService.hasAnyPlacementOfficer()) {
                System.out.println("\n[SYSTEM NOTICE] No Placement Officer account found in database.");
                System.out.print("Would you like to initialize an administrative Placement Officer account now? (y/n): ");
                String answer = scanner.nextLine().trim();
                if ("y".equalsIgnoreCase(answer) || "yes".equalsIgnoreCase(answer)) {
                    handleAdminOfficerSetup(scanner);
                }
            }
        } catch (SQLException e) {
            System.out.println("[WARN] Could not verify placement officer presence: " + e.getMessage());
        }
    }

    private void handleAdminOfficerSetup(Scanner scanner) {
        System.out.println("\n--- INITIAL PLACEMENT OFFICER PROVISIONING ---");
        try {
            String username = readNonEmptyString(scanner, "Enter Officer Username: ");
            String password = readNonEmptyString(scanner, "Enter Officer Password (min 6 chars): ");
            String email = readNonEmptyString(scanner, "Enter Officer Email: ");

            User officer = authService.registerPlacementOfficer(username, password, email);
            System.out.println("[SUCCESS] Placement Officer '" + officer.getUsername() + "' provisioned successfully!");
        } catch (IllegalArgumentException e) {
            System.out.println("[ERROR] Officer validation failed: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("[ERROR] Database error creating officer: " + e.getMessage());
        }
    }

    // =========================================================================
    // STUDENT DASHBOARD (PAGE 6)
    // =========================================================================

    /**
     * Renders header and options for Page 6 — Student Dashboard matching exact design specifications.
     */
    public void renderStudentDashboardHeader(Student student) {
        System.out.println(ConsoleTheme.border("================================================================"));
        System.out.println(ConsoleTheme.header("                    NEXTSTEP | STUDENT PORTAL"));
        System.out.println(ConsoleTheme.header("                       STUDENT DASHBOARD"));
        System.out.println(ConsoleTheme.border("================================================================"));
        System.out.println();
        String studentName = (student != null && student.getFullName() != null) ? student.getFullName() : "Student";
        String studentId = (student != null && student.getStudentId() != null) ? student.getStudentId() : "N/A";
        System.out.println(ConsoleTheme.text("Welcome, " + studentName));
        System.out.println(ConsoleTheme.text("Student ID: " + studentId));
        System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
        System.out.println(ConsoleTheme.cyan("  ┌────────────────────────────────────────────────────────────┐"));
        System.out.println(ConsoleTheme.cyan("  │") + ConsoleTheme.text("                     PLACEMENT WORKFLOW                     ") + ConsoleTheme.cyan("│"));
        System.out.println(ConsoleTheme.cyan("  └────────────────────────────────────────────────────────────┘"));
        System.out.println(ConsoleTheme.text("  1. View Available Job Drives"));
        System.out.println(ConsoleTheme.text("  2. Check Job Eligibility & Skill Match"));
        System.out.println(ConsoleTheme.text("  3. Apply for an Eligible Job"));
        System.out.println(ConsoleTheme.text("  4. View My Applications"));
        System.out.println(ConsoleTheme.text("  0. Sign Out"));
        System.out.println();
        System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
    }

    /**
     * Displays Page 6 — Student Dashboard and dispatches user actions.
     * 
     * @param scanner active Scanner
     * @param student authenticated Student profile
     * @param user authenticated User account
     */
    public void studentPortal(Scanner scanner, Student student, User user) {
        boolean inPortal = true;
        while (inPortal) {
            renderStudentDashboardHeader(student);
            System.out.print(ConsoleTheme.prompt("Enter your choice: "));
            if (!scanner.hasNextLine()) {
                break;
            }
            String choice = scanner.nextLine().trim();
            if (System.console() == null) {
                System.out.println(choice);
            }
            System.out.println(ConsoleTheme.border("================================================================"));

            switch (choice) {
                case "1" -> viewAvailableJobsFlow(scanner, student);
                case "2" -> showEligibilityAndSkillAnalysis(scanner, student);
                case "3" -> showApplicationConfirmation(scanner, student);
                case "4" -> viewStudentApplications(student);
                case "0" -> {
                    System.out.println("\nSigning out from Student Portal...");
                    inPortal = false;
                }
                default -> System.out.println("[WARN] Invalid option. Please try again.");
            }
        }
    }

    /**
     * Connected workflow for Option 1 on Student Dashboard:
     * Displays available job drives and allows immediate selection to inspect eligibility and apply.
     */
    public void viewAvailableJobsFlow(Scanner scanner, Student student) {
        viewAvailableJobs();
        System.out.println();
        System.out.print("Enter Job ID to select & check eligibility (or 0 to Back): ");
        if (!scanner.hasNextLine()) {
            return;
        }
        String input = scanner.nextLine().trim();
        if (System.console() == null) {
            System.out.println(input);
        }
        if ("0".equals(input) || input.isEmpty()) {
            return;
        }

        int jobId;
        try {
            jobId = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println(ConsoleTheme.red("[ERROR] Invalid Job ID. Must be numeric."));
            return;
        }

        try {
            Job job = jobDAO.findById(jobId);
            if (job == null) {
                System.out.println(ConsoleTheme.red("[ERROR] Job with ID " + jobId + " not found."));
                return;
            }
            showEligibilityAndSkillAnalysisForJob(scanner, student, job);
        } catch (SQLException e) {
            System.out.println(ConsoleTheme.red("[ERROR] Database error retrieving job: " + e.getMessage()));
        }
    }

    private void viewAvailableJobs() {
        System.out.println(ConsoleTheme.header("\n--- AVAILABLE RECRUITMENT DRIVES ---"));
        try {
            // Demonstrates lambda sorting (highest CTC first)
            List<Job> jobs = recruitmentService.getJobsSortedByCtc();
            long totalDrives = recruitmentService.countOpenJobs();
            List<String> hiringCompanies = recruitmentService.getDistinctHiringCompanies();

            System.out.println(ConsoleTheme.dim("Active Drives: " + totalDrives + " | Hiring Companies: " + String.join(", ", hiringCompanies)));
            System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
            if (jobs.isEmpty()) {
                System.out.println(ConsoleTheme.warn("No open job positions available at this moment."));
                return;
            }

            System.out.printf("%-4s | %-22s | %-26s | %-7s | %-6s%n",
                    "ID", "Job Title", "Company", "CTC", "Loc");
            System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
            for (Job j : jobs) {
                String title = j.getTitle() != null ? j.getTitle() : "N/A";
                if (title.length() > 22) title = title.substring(0, 19) + "...";
                String company = j.getCompanyName() != null ? j.getCompanyName() : "N/A";
                String ctc = j.getCtcLpa() != null ? String.format("%.2f", j.getCtcLpa()) : "0.00";
                String loc = j.getLocation() != null ? j.getLocation() : "Campus";
                if (loc.length() > 6) loc = loc.substring(0, 5) + ".";
                System.out.printf("%-4d | %-22s | %-26s | %-7s | %-6s%n",
                        j.getId(), title, company, ctc, loc);
            }
            System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
        } catch (SQLException e) {
            System.out.println(ConsoleTheme.error("[ERROR] Failed to load jobs: " + e.getMessage()));
        }
    }

    private void viewJobDetails(Scanner scanner) {
        int jobId = readInt(scanner, "\nEnter Job ID to inspect: ");
        try {
            Job job = jobDAO.findById(jobId);
            if (job == null) {
                System.out.println("[ERROR] Job with ID " + jobId + " not found.");
                return;
            }

            System.out.println("\n================ JOB DETAILS ================");
            System.out.println("Job ID:             " + job.getId());
            System.out.println("Position:           " + job.getTitle());
            System.out.println("Company:            " + job.getCompanyName());
            System.out.println("Package (CTC):      " + job.getCtcLpa() + " LPA");
            System.out.println("Location:           " + job.getLocation());
            System.out.println("Status:             " + job.getStatus());
            System.out.println("Minimum CGPA:       " + job.getMinCgpa());
            System.out.println("Max Backlogs:       " + job.getMaxBacklogs());
            System.out.println("Target Batch:       " + job.getGraduationYear());
            System.out.println("Eligible Branches:  " + String.join(", ", job.getEligibleBranches()));
            System.out.println("Required Skills:    " + String.join(", ", job.getRequiredSkills()));
            System.out.println("Description:        " + job.getDescription());
            System.out.println("=============================================");
        } catch (SQLException e) {
            System.out.println("[ERROR] Failed to retrieve job details: " + e.getMessage());
        }
    }

    private void checkStudentJobEligibility(Scanner scanner, Student student) {
        int jobId = readInt(scanner, "\nEnter Job ID to evaluate eligibility: ");
        try {
            Job job = jobDAO.findById(jobId);
            if (job == null) {
                System.out.println("[ERROR] Job with ID " + jobId + " not found.");
                return;
            }

            EligibilityResult result = recruitmentService.checkEligibility(student, job);
            System.out.println("\n--- ELIGIBILITY EVALUATION FOR: " + job.getTitle() + " ---");
            if (result.isEligible()) {
                System.out.println("[RESULT] ELIGIBLE! You satisfy all academic cutoffs and branch/skill criteria.");
                System.out.println("Matched Skills: " + result.getMatchedSkills());
            } else {
                System.out.println("[RESULT] NOT ELIGIBLE. The following criteria were not met:");
                for (String reason : result.getReasons()) {
                    System.out.println("  • " + reason);
                }
                if (!result.getMissingSkills().isEmpty()) {
                    System.out.println("Missing Required Skills: " + result.getMissingSkills());
                }
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Could not check eligibility: " + e.getMessage());
        }
    }

    private void applyForJobFlow(Scanner scanner, Student student) {
        int jobId = readInt(scanner, "\nEnter Job ID you want to apply for: ");
        try {
            Application app = recruitmentService.applyForJob(student, jobId);
            System.out.println("\n[SUCCESS] Application successfully submitted!");
            System.out.println("Application ID: " + app.getId());
            System.out.println("Role:           " + app.getJobTitle());
            System.out.println("Company:        " + app.getCompanyName());
            System.out.println("Status:         " + app.getStatus());
        } catch (IllegalStateException e) {
            System.out.println("\n[BLOCKED] Application rejected: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("\n[ERROR] Invalid application request: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("\n[ERROR] Database error processing application: " + e.getMessage());
        }
    }

    private void viewStudentApplications(Student student) {
        System.out.println(ConsoleTheme.header("\n--- MY APPLICATION HISTORY ---"));
        try {
            List<Application> apps = recruitmentService.getStudentApplications(student.getId());
            if (apps.isEmpty()) {
                System.out.println(ConsoleTheme.warn("You have not applied for any campus jobs yet."));
                return;
            }

            System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
            System.out.printf("%-6s | %-22s | %-16s | %-11s%n",
                    "App ID", "Job Title", "Company", "Status");
            System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
            for (Application a : apps) {
                String title = a.getJobTitle() != null ? a.getJobTitle() : "N/A";
                if (title.length() > 22) title = title.substring(0, 19) + "...";
                String comp = a.getCompanyName() != null ? a.getCompanyName() : "N/A";
                if (comp.length() > 16) comp = comp.substring(0, 13) + "...";
                String st = a.getStatus() != null ? a.getStatus().name() : "N/A";
                System.out.printf("%-6d | %-22s | %-16s | %-11s%n",
                        a.getId(), title, comp, st);
            }
            System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
        } catch (SQLException e) {
            System.out.println(ConsoleTheme.error("[ERROR] Failed to load application history: " + e.getMessage()));
        }
    }

    // =========================================================================
    // PLACEMENT OFFICER DASHBOARD (PAGE 7)
    // =========================================================================

    /**
     * Renders header and options for Page 7 — Placement Officer Dashboard matching exact design specifications.
     */
    public void renderOfficerDashboardHeader(User officer) {
        System.out.println(ConsoleTheme.border("================================================================"));
        System.out.println(ConsoleTheme.header("              NEXTSTEP | PLACEMENT OFFICER PORTAL"));
        System.out.println(ConsoleTheme.header("                    OFFICER DASHBOARD"));
        System.out.println(ConsoleTheme.border("================================================================"));
        System.out.println();
        System.out.println(ConsoleTheme.text("Welcome, Placement Officer"));
        System.out.println(ConsoleTheme.text("Officer ID: Not available"));
        System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
        System.out.println(ConsoleTheme.cyan("  ┌────────────────────────────────────────────────────────────┐"));
        System.out.println(ConsoleTheme.cyan("  │") + ConsoleTheme.text("                    ADMINISTRATIVE TOOLS                    ") + ConsoleTheme.cyan("│"));
        System.out.println(ConsoleTheme.cyan("  └────────────────────────────────────────────────────────────┘"));
        System.out.println(ConsoleTheme.text("  1. Manage Student Records ") + ConsoleTheme.comingSoon());
        System.out.println(ConsoleTheme.text("  2. Manage Companies ") + ConsoleTheme.comingSoon());
        System.out.println(ConsoleTheme.text("  3. Create Job Drive"));
        System.out.println(ConsoleTheme.text("  4. View Applications"));
        System.out.println(ConsoleTheme.text("  5. Update Application Status"));
        System.out.println(ConsoleTheme.text("  0. Sign Out"));
        System.out.println();
        System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
    }

    /**
     * Displays Page 7 — Placement Officer Dashboard and dispatches user actions.
     * 
     * @param scanner active Scanner
     * @param officer authenticated Placement Officer User account
     */
    public void placementOfficerPortal(Scanner scanner, User officer) {
        boolean inPortal = true;
        while (inPortal) {
            renderOfficerDashboardHeader(officer);
            System.out.print(ConsoleTheme.prompt("Enter your choice: "));
            if (!scanner.hasNextLine()) {
                break;
            }
            String choice = scanner.nextLine().trim();
            if (System.console() == null) {
                System.out.println(choice);
            }
            System.out.println(ConsoleTheme.border("================================================================"));

            switch (choice) {
                case "1" -> System.out.println("\nThis feature is not implemented yet.");
                case "2" -> System.out.println("\nThis feature is not implemented yet.");
                case "3" -> showCreateJobDrive(scanner, officer);
                case "4" -> showApplicationRecords(scanner, officer);
                case "5" -> updateApplicationStatusFlow(scanner, officer);
                case "0" -> {
                    System.out.println("\nSigning out from Placement Officer Portal...");
                    inPortal = false;
                }
                default -> System.out.println("[WARN] Invalid option. Please try again.");
            }
        }
    }

    private void viewAllApplicationsFlow(User officer) {
        System.out.println(ConsoleTheme.header("\n--- ALL CAMPUS APPLICATIONS ---"));
        try {
            List<Application> apps = recruitmentService.getAllApplications(officer);
            if (apps.isEmpty()) {
                System.out.println(ConsoleTheme.warn("No student applications have been submitted yet."));
                return;
            }

            System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
            System.out.printf("%-6s | %-12s | %-16s | %-11s | %-10s%n",
                    "App ID", "Roll No", "Student Name", "Status", "Company");
            System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
            for (Application a : apps) {
                String rollNo = (a.getStudentRollNumber() != null) ? a.getStudentRollNumber() : String.valueOf(a.getStudentId());
                if (rollNo.length() > 12) rollNo = rollNo.substring(0, 9) + "...";
                String name = (a.getStudentName() != null) ? a.getStudentName() : "N/A";
                if (name.length() > 16) name = name.substring(0, 13) + "...";
                String st = (a.getStatus() != null) ? a.getStatus().name() : "N/A";
                String comp = (a.getCompanyName() != null) ? a.getCompanyName() : "N/A";
                if (comp.length() > 10) comp = comp.substring(0, 8) + "..";
                System.out.printf("%-6d | %-12s | %-16s | %-11s | %-10s%n",
                        a.getId(), rollNo, name, st, comp);
            }
            System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
        } catch (SecurityException e) {
            System.out.println("[SECURITY ERROR] " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("[ERROR] Failed to load applications: " + e.getMessage());
        }
    }

    private void updateApplicationStatusFlow(Scanner scanner, User officer) {
        System.out.println("\n--- UPDATE APPLICATION STATUS ---");
        int appId = readInt(scanner, "Enter Application ID: ");

        try {
            Application app = applicationDAO.findById(appId);
            if (app == null) {
                System.out.println("[ERROR] Application with ID " + appId + " not found.");
                return;
            }

            System.out.println("Found Application: ID " + app.getId() + " | Job: " + app.getJobTitle() + " | Current Status: " + app.getStatus());
            System.out.println("\nSelect New Status:");
            ApplicationStatus[] statuses = ApplicationStatus.values();
            for (int i = 0; i < statuses.length; i++) {
                System.out.println((i + 1) + ". " + statuses[i]);
            }

            int statusChoice = readInt(scanner, "Choose status (1-" + statuses.length + "): ");
            if (statusChoice < 1 || statusChoice > statuses.length) {
                System.out.println("[ERROR] Invalid status selection.");
                return;
            }

            ApplicationStatus newStatus = statuses[statusChoice - 1];
            boolean updated = recruitmentService.updateApplicationStatus(officer, appId, newStatus);
            if (updated) {
                System.out.println("[SUCCESS] Application #" + appId + " status successfully updated to " + newStatus + "!");
            } else {
                System.out.println("[ERROR] Update failed.");
            }
        } catch (SecurityException e) {
            System.out.println("[SECURITY ERROR] " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("[ERROR] Database error during status update: " + e.getMessage());
        }
    }

    private void viewRecruitmentSummaryFlow(User officer) {
        System.out.println("\n--- RECRUITMENT SUMMARY & METRICS ---");
        try {
            // Demonstrates groupingBy and counting
            Map<ApplicationStatus, Long> summary = recruitmentService.getRecruitmentSummary(officer);
            // Demonstrates partitioningBy and counting
            Map<Boolean, Long> placementPartition = recruitmentService.getPlacementPartitionCount(officer);
            List<Application> allApps = recruitmentService.getAllApplications(officer);

            System.out.println("Total Applications Received: " + allApps.size());
            System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
            System.out.println("Lifecycle Breakdown (groupingBy):");
            for (ApplicationStatus status : ApplicationStatus.values()) {
                long count = summary.getOrDefault(status, 0L);
                System.out.printf("  %-15s : %d%n", status.name(), count);
            }
            System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
            System.out.println("Placement Partition (partitioningBy):");
            long placed = placementPartition.getOrDefault(true, 0L);
            long inProcess = placementPartition.getOrDefault(false, 0L);
            System.out.printf("  Placed (SELECTED)      : %d%n", placed);
            System.out.printf("  In-Progress / Other    : %d%n", inProcess);
            System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
        } catch (SecurityException e) {
            System.out.println("[SECURITY ERROR] " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("[ERROR] Failed to compile summary: " + e.getMessage());
        }
    }

    private void exportApplicationsToCsvFlow(User officer) {
        System.out.println("\n--- EXPORT APPLICATION REPORT TO CSV ---");
        try {
            File reportFile = recruitmentService.exportApplicationsToCsv(officer, "reports/applications_report.csv");
            System.out.println("[SUCCESS] Report exported successfully!");
            System.out.println("File Location: " + reportFile.getAbsolutePath());
            System.out.println("File Size:     " + reportFile.length() + " bytes");
        } catch (SecurityException e) {
            System.out.println("[SECURITY ERROR] " + e.getMessage());
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to export CSV report: " + e.getMessage());
        }
    }

    private void processAndArchiveCsvFlow(Scanner scanner, User officer) {
        System.out.println("\n--- PROCESS & ARCHIVE CSV REPORT ---");
        String sourcePath = readString(scanner, "Enter Source CSV path (default: reports/applications_report.csv): ");
        if (sourcePath == null || sourcePath.isEmpty()) {
            sourcePath = "reports/applications_report.csv";
        }
        String targetPath = readString(scanner, "Enter Destination CSV path (default: reports/applications_audited.csv): ");
        if (targetPath == null || targetPath.isEmpty()) {
            targetPath = "reports/applications_audited.csv";
        }

        System.out.print("Do you want to update an application status before archiving? (y/n): ");
        String ans = scanner.nextLine().trim();
        int updateId = -1;
        ApplicationStatus newStatus = null;
        if ("y".equalsIgnoreCase(ans) || "yes".equalsIgnoreCase(ans)) {
            updateId = readInt(scanner, "Enter Application ID to update: ");
            System.out.println("Select New Status:");
            ApplicationStatus[] statuses = ApplicationStatus.values();
            for (int i = 0; i < statuses.length; i++) {
                System.out.println((i + 1) + ". " + statuses[i]);
            }
            int choice = readInt(scanner, "Choose status (1-" + statuses.length + "): ");
            if (choice >= 1 && choice <= statuses.length) {
                newStatus = statuses[choice - 1];
            }
        }

        try {
            List<Application> processed = recruitmentService.processAndArchiveCsvReport(
                    officer, sourcePath, targetPath, updateId, newStatus);
            System.out.println("[SUCCESS] CSV processed and verified successfully!");
            System.out.println("Records Processed : " + processed.size());
            System.out.println("Audited Output    : " + targetPath);
        } catch (SecurityException e) {
            System.out.println("[SECURITY ERROR] " + e.getMessage());
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to process CSV: " + e.getMessage());
        }
    }

    // =========================================================================
    // CREATE JOB DRIVE (PAGE 8)
    // =========================================================================

    /**
     * Renders header for Page 8 — Create Job Drive matching exact design specifications.
     */
    public void renderCreateJobDriveHeader() {
        System.out.println(ConsoleTheme.border("================================================================"));
        System.out.println(ConsoleTheme.header("              NEXTSTEP | PLACEMENT OFFICER PORTAL"));
        System.out.println(ConsoleTheme.header("                       CREATE JOB DRIVE"));
        System.out.println(ConsoleTheme.border("================================================================"));
        System.out.println();
    }

    /**
     * Renders footer and action options for Page 8 — Create Job Drive.
     */
    public void renderCreateJobDriveFooter() {
        System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
        System.out.println(ConsoleTheme.text("                  1. PUBLISH JOB"));
        System.out.println(ConsoleTheme.text("                  0. CANCEL"));
        System.out.println(ConsoleTheme.border("================================================================"));
    }

    /**
     * Displays Page 8 — Create Job Drive form and manages interactive drive publishing.
     * 
     * @param scanner active Scanner
     * @param officer authenticated Placement Officer User account
     * @return boolean true if job drive was published, false if cancelled/returned
     */
    public boolean showCreateJobDrive(Scanner scanner, User officer) {
        String[] values = new String[10];
        while (true) {
            clearConsole();
            renderCreateJobDriveHeader();
            System.out.println(ConsoleTheme.dim("[Form Shortcuts: 'B' = Previous Field | 'N' = Next Field] (Esc / Tab / Enter supported)"));
            System.out.println(ConsoleTheme.dim("[A] Auto-Fill Demo Data — Enter 'A' at any prompt to populate all fields with valid sample data"));
            System.out.println();

            int nextJobId = 1;
            try {
                nextJobId = jobDAO.getNextJobId();
            } catch (SQLException e) {
                System.out.println(ConsoleTheme.warn("[WARN] Could not compute next Job ID: " + e.getMessage()));
            }

            // Retrieve and display available partner companies from MySQL
            List<Company> companies;
            try {
                companies = companyDAO.findAllCompanies();
            } catch (SQLException e) {
                System.out.println(ConsoleTheme.error("[ERROR] Failed to load hiring companies: " + e.getMessage()));
                companies = Collections.emptyList();
            }

            if (companies.isEmpty()) {
                System.out.println(ConsoleTheme.warn("[NOTICE] No partner companies registered in the database."));
                System.out.println(ConsoleTheme.dim("Partner companies must be registered before publishing job drives."));
                System.out.println();
            } else {
                System.out.println(ConsoleTheme.header("Available Partner Companies (Select by ID):"));
                for (Company c : companies) {
                    System.out.printf("  [ID %d] %-30s | %-22s | %s%n",
                            c.getId(), c.getName(), c.getIndustry(), c.getLocation());
                }
                System.out.println();
            }

            System.out.println("Job ID:                 " + nextJobId);

            String[] prompts = {
                    "Company ID:             ",
                    "Job Title:              ",
                    "Job Description:        ",
                    "CTC (LPA):              ",
                    "Job Location:           ",
                    "Minimum CGPA:           ",
                    "Max Active Backlogs:    ",
                    "Eligible Branches:      ",
                    "Required Skills:        ",
                    "Graduation Year:        "
            };
            boolean[] isPassword = new boolean[10];
            int[] blankLines = { 4 };

            String[] demoData = getJobDemoData(companies);
            boolean completed = fillMultiFieldForm(scanner, prompts, isPassword, blankLines, values, demoData);
            if (!completed) {
                return false;
            }

            String companyIdStr = values[0];
            String jobTitle = values[1];
            String description = values[2];
            String ctcStr = values[3];
            String location = values[4];
            String minCgpaStr = values[5];
            String maxBacklogsStr = values[6];
            String branchesStr = values[7];
            String skillsStr = values[8];
            String gradYearStr = values[9];

            renderCreateJobDriveFooter();

            while (true) {
                System.out.print(ConsoleTheme.prompt("Enter your choice: "));
                if (!scanner.hasNextLine()) {
                    return false;
                }
                String choice = scanner.nextLine().trim();
                if (System.console() == null) {
                    System.out.println(choice);
                }
                System.out.println(ConsoleTheme.border("================================================================"));

                if ("1".equals(choice)) {
                    boolean created = processJobDriveCreation(companyIdStr, jobTitle, description, ctcStr,
                            location, minCgpaStr, maxBacklogsStr, branchesStr, skillsStr, gradYearStr);
                    if (created) {
                        return true;
                    } else {
                        System.out.print("\nPress Enter to try again, or enter 0 to return to Officer Dashboard: ");
                        if (!scanner.hasNextLine()) {
                            return false;
                        }
                        String retry = scanner.nextLine().trim();
                        if (System.console() == null) {
                            System.out.println(retry);
                        }
                        if ("0".equals(retry)) {
                            return false;
                        }
                        break; // re-run outer loop to show form again
                    }
                } else if ("0".equals(choice)) {
                    System.out.println("\nJob drive creation cancelled. Returning to Placement Officer Dashboard...");
                    return false;
                } else {
                    System.out.println(ConsoleTheme.warn("[WARN] Invalid choice. Please enter 1 to Publish Job or 0 to Cancel."));
                    System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
                }
            }
        }
    }

    public boolean showCreateJobDrive(Scanner scanner) {
        return showCreateJobDrive(scanner, null);
    }

    /**
     * Validates all form inputs and executes job drive creation within a database transaction.
     * Full 10-parameter signature persisting real CTC, Location, Backlogs, Description, and Company FK.
     */
    public boolean processJobDriveCreation(String companyIdStr, String jobTitle, String description,
                                           String ctcStr, String location, String minCgpaStr,
                                           String maxBacklogsStr, String branchesStr, String skillsStr,
                                           String gradYearStr) {
        // 1. Validate Company ID & verify existence in database
        if (ValidationUtil.isNullOrBlank(companyIdStr)) {
            System.out.println("\n[ERROR] Company ID cannot be empty.");
            return false;
        }
        int companyId;
        try {
            companyId = Integer.parseInt(companyIdStr.trim());
        } catch (NumberFormatException e) {
            System.out.println("\n[ERROR] Invalid Company ID: must be a valid numeric ID.");
            return false;
        }

        Company company;
        try {
            company = companyDAO.findById(companyId);
            if (company == null) {
                System.out.println("\n[ERROR] Company with ID " + companyId + " does not exist in the database.");
                return false;
            }
        } catch (SQLException e) {
            System.out.println("\n[ERROR] Database error verifying company: " + e.getMessage());
            return false;
        }

        // 2. Validate Job Title
        if (ValidationUtil.isNullOrBlank(jobTitle)) {
            System.out.println("\n[ERROR] Job Title cannot be empty.");
            return false;
        }

        // 3. Clean Description
        String cleanDescription = (description != null && !description.trim().isEmpty())
                ? description.trim()
                : "Campus Recruitment Drive for " + jobTitle.trim();

        // 4. Validate CTC (LPA)
        if (ValidationUtil.isNullOrBlank(ctcStr)) {
            System.out.println("\n[ERROR] CTC cannot be empty.");
            return false;
        }
        BigDecimal ctc;
        try {
            ctc = new BigDecimal(ctcStr.trim());
            if (ctc.compareTo(BigDecimal.ZERO) < 0) {
                System.out.println("\n[ERROR] Invalid CTC: compensation package cannot be negative.");
                return false;
            }
        } catch (Exception e) {
            System.out.println("\n[ERROR] Invalid CTC: must be a valid numeric value in LPA (e.g. 12.50).");
            return false;
        }

        // 5. Validate Location
        if (ValidationUtil.isNullOrBlank(location)) {
            System.out.println("\n[ERROR] Job Location cannot be empty.");
            return false;
        }
        String cleanLocation = location.trim();

        // 6. Validate Minimum CGPA
        if (ValidationUtil.isNullOrBlank(minCgpaStr)) {
            System.out.println("\n[ERROR] Minimum CGPA cannot be empty.");
            return false;
        }
        double minCgpa;
        try {
            minCgpa = Double.parseDouble(minCgpaStr.trim());
        } catch (NumberFormatException e) {
            System.out.println("\n[ERROR] Invalid Minimum CGPA: must be a numeric value between 0.00 and 10.00.");
            return false;
        }
        if (!ValidationUtil.isValidCgpa(minCgpa)) {
            System.out.println("\n[ERROR] Minimum CGPA must be between 0.00 and 10.00.");
            return false;
        }

        // 7. Validate Maximum Active Backlogs
        if (ValidationUtil.isNullOrBlank(maxBacklogsStr)) {
            System.out.println("\n[ERROR] Maximum Active Backlogs cannot be empty.");
            return false;
        }
        int maxBacklogs;
        try {
            maxBacklogs = Integer.parseInt(maxBacklogsStr.trim());
        } catch (NumberFormatException e) {
            System.out.println("\n[ERROR] Invalid Maximum Active Backlogs: must be an integer greater than or equal to zero.");
            return false;
        }
        if (!ValidationUtil.isValidBacklogs(maxBacklogs)) {
            System.out.println("\n[ERROR] Maximum active backlogs cannot be negative.");
            return false;
        }

        // 8. Validate Eligible Branches
        if (ValidationUtil.isNullOrBlank(branchesStr)) {
            System.out.println("\n[ERROR] Eligible Branches cannot be empty.");
            return false;
        }
        List<String> branches = ValidationUtil.parseCommaSeparatedList(branchesStr);
        if (branches.isEmpty()) {
            System.out.println("\n[ERROR] At least one eligible branch must be specified.");
            return false;
        }

        // 9. Validate Required Skills
        if (ValidationUtil.isNullOrBlank(skillsStr)) {
            System.out.println("\n[ERROR] Required Skills cannot be empty.");
            return false;
        }
        List<String> skills = ValidationUtil.parseCommaSeparatedList(skillsStr);
        if (skills.isEmpty()) {
            System.out.println("\n[ERROR] At least one required technical skill must be specified.");
            return false;
        }

        // 10. Validate Graduation Year
        if (ValidationUtil.isNullOrBlank(gradYearStr)) {
            System.out.println("\n[ERROR] Graduation Year cannot be empty.");
            return false;
        }
        int gradYear;
        try {
            gradYear = Integer.parseInt(gradYearStr.trim());
        } catch (NumberFormatException e) {
            System.out.println("\n[ERROR] Invalid Graduation Year: must be a valid 4-digit year.");
            return false;
        }
        if (!ValidationUtil.isValidGraduationYear(gradYear)) {
            System.out.println("\n[ERROR] Graduation year must be between 2020 and 2035.");
            return false;
        }

        // 11. Construct Job and persist via JobDAO in transaction
        try {
            Job job = new Job();
            job.setCompanyId(companyId);
            job.setTitle(jobTitle.trim());
            job.setDescription(cleanDescription);
            job.setCtcLpa(ctc);
            job.setLocation(cleanLocation);
            job.setMinCgpa(minCgpa);
            job.setMaxBacklogs(maxBacklogs);
            job.setGraduationYear(gradYear);
            job.setStatus("OPEN");

            Job createdJob = jobDAO.createJob(job, branches, skills);
            System.out.println("\n[SUCCESS] Job drive created and published successfully!");
            System.out.println("Job ID:     " + createdJob.getId());
            System.out.println("Role:       " + createdJob.getTitle());
            System.out.println("Company:    " + company.getName());
            System.out.println("Package:    " + createdJob.getCtcLpa() + " LPA");
            System.out.println("Location:   " + createdJob.getLocation());
            System.out.println("Target:     Batch of " + createdJob.getGraduationYear());
            return true;
        } catch (SQLException e) {
            System.out.println("\n[ERROR] Database error publishing job drive: " + e.getMessage());
            return false;
        }
    }

    /**
     * Backward-compatible 7-parameter overload for existing tests.
     */
    public boolean processJobDriveCreation(String companyIdStr, String jobRole, String minCgpaStr,
                                           String branchesStr, String gradYearStr, String skillsStr,
                                           String deadlineStr) {
        String description = (deadlineStr != null && !deadlineStr.trim().isEmpty())
                ? "Role: " + jobRole + " | Deadline: " + deadlineStr.trim()
                : "Campus Recruitment Drive for " + jobRole;
        return processJobDriveCreation(companyIdStr, jobRole, description,
                "0.00", "Campus", minCgpaStr, "0", branchesStr, skillsStr, gradYearStr);
    }

    // =========================================================================
    // ELIGIBILITY RESULT & SKILL MATCH RESULT (PAGE 9)
    // =========================================================================

    /**
     * Renders header for Page 9 Part A — Eligibility Result matching exact design specifications.
     */
    public void renderEligibilityResultHeader() {
        System.out.println(ConsoleTheme.border("================================================================"));
        System.out.println(ConsoleTheme.header("                    NEXTSTEP | STUDENT PORTAL"));
        System.out.println(ConsoleTheme.header("                       ELIGIBILITY RESULT"));
        System.out.println(ConsoleTheme.border("================================================================"));
        System.out.println();
    }

    /**
     * Renders header for Page 9 Part B — Skill Match Result matching exact design specifications.
     */
    public void renderSkillMatchResultHeader() {
        System.out.println(ConsoleTheme.border("================================================================"));
        System.out.println(ConsoleTheme.header("                    NEXTSTEP | STUDENT PORTAL"));
        System.out.println(ConsoleTheme.header("                       SKILL MATCH RESULT"));
        System.out.println(ConsoleTheme.border("================================================================"));
        System.out.println();
    }

    /**
     * Displays Page 9 Part A — Eligibility Result and navigates to Part B.
     */
    public void showEligibilityAndSkillAnalysis(Scanner scanner, Student student) {
        if (student == null) {
            System.out.println("[ERROR] Student profile is null.");
            return;
        }

        System.out.print("Enter Job ID: ");
        if (!scanner.hasNextLine()) {
            return;
        }
        String input = scanner.nextLine().trim();
        if (System.console() == null) {
            System.out.println(input);
        }
        if (input.isEmpty()) {
            System.out.println("[ERROR] Job ID cannot be empty.");
            return;
        }
        if ("0".equals(input)) {
            return;
        }

        int jobId;
        try {
            jobId = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("[ERROR] Invalid Job ID. Must be numeric.");
            return;
        }

        Job job;
        try {
            job = jobDAO.findById(jobId);
            if (job == null) {
                System.out.println("[ERROR] Job with ID " + jobId + " not found.");
                return;
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Database error fetching job: " + e.getMessage());
            return;
        }

        showEligibilityAndSkillAnalysisForJob(scanner, student, job);
    }

    /**
     * Displays Page 9 Part A — Unified Eligibility Result and Criteria Checklist for a selected job.
     */
    public void showEligibilityAndSkillAnalysisForJob(Scanner scanner, Student student, Job job) {
        try {
            if (student.getSkills() == null || student.getSkills().isEmpty()) {
                student.setSkills(studentDAO.getStudentSkills(student.getId()));
            }
        } catch (SQLException e) {
            System.out.println("[WARN] Could not load student skills: " + e.getMessage());
        }

        while (true) {
            clearConsole();
            renderEligibilityResultHeader();

            System.out.println("Student Name:           " + (student.getFullName() != null ? student.getFullName() : "N/A"));
            System.out.println("Job ID:                 " + job.getId());
            System.out.println();
            System.out.println("Criteria Checklist:");
            System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
            System.out.printf("%-24s%-19s%-16s%s%n", "Criteria", "Requirement", "Student Status", "Result");
            System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));

            // Calculate unified eligibility using RecruitmentService as the single source of truth
            EligibilityResult result = recruitmentService.checkEligibility(student, job);

            // 1. CGPA
            String cgpaReq = ">= " + job.getMinCgpa();
            String cgpaStatus = String.valueOf(student.getCgpa());
            String cgpaResult = result.isCgpaEligible() ? ConsoleTheme.green("PASS") : ConsoleTheme.red("FAIL");
            System.out.printf("%-24s%-19s%-16s%s%n", "CGPA", cgpaReq, cgpaStatus, cgpaResult);

            // 2. Active Backlogs
            String backlogsReq = "<= " + job.getMaxBacklogs();
            String backlogsStatus = String.valueOf(student.getActiveBacklogs());
            String backlogsResult = result.isBacklogsEligible() ? ConsoleTheme.green("PASS") : ConsoleTheme.red("FAIL");
            System.out.printf("%-24s%-19s%-16s%s%n", "Active Backlogs", backlogsReq, backlogsStatus, backlogsResult);

            // 3. Branch
            List<String> branches = job.getEligibleBranches();
            String rawBranchReq = (branches != null && !branches.isEmpty()) ? String.join(", ", branches) : "All";
            String branchReq = rawBranchReq.length() > 18 ? rawBranchReq.substring(0, 15) + "..." : rawBranchReq;
            String stdBranch = student.getBranch() != null ? student.getBranch() : "N/A";
            if (stdBranch.length() > 15) stdBranch = stdBranch.substring(0, 12) + "...";
            String branchResult = result.isBranchEligible() ? ConsoleTheme.green("PASS") : ConsoleTheme.red("FAIL");
            System.out.printf("%-24s%-19s%-16s%s%n", "Branch", branchReq, stdBranch, branchResult);

            // 4. Graduation Year
            String gradReq = (job.getGraduationYear() > 0) ? String.valueOf(job.getGraduationYear()) : "Any";
            String gradResult = result.isGraduationYearEligible() ? ConsoleTheme.green("PASS") : ConsoleTheme.red("FAIL");
            System.out.printf("%-24s%-19s%-16s%s%n", "Graduation Year", gradReq, String.valueOf(student.getGraduationYear()), gradResult);

            // 5. Technical Skills
            int totalReq = job.getRequiredSkills() != null ? job.getRequiredSkills().size() : 0;
            int matchedReq = result.getMatchedSkills().size();
            String skillsReq = totalReq > 0 ? totalReq + " Required" : "None";
            String skillsStatus;
            if (student.getSkills() == null || student.getSkills().isEmpty()) {
                skillsStatus = "None Added";
            } else {
                skillsStatus = matchedReq + " / " + totalReq;
            }
            String skillsResult = result.isSkillsEligible() ? ConsoleTheme.green("PASS") : ConsoleTheme.red("FAIL");
            System.out.printf("%-24s%-19s%-16s%s%n", "Technical Skills", skillsReq, skillsStatus, skillsResult);

            System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
            System.out.println();
            boolean overallEligible = result.isEligible();
            String statusWord = overallEligible ? ConsoleTheme.green("ELIGIBLE") : ConsoleTheme.red("NOT ELIGIBLE");
            System.out.println("OVERALL RESULT: " + statusWord);

            if (!result.getMissingSkills().isEmpty()) {
                System.out.println("Missing Skills:         " + ConsoleTheme.red(String.join(", ", result.getMissingSkills())));
            }
            if (student.getSkills() == null || student.getSkills().isEmpty()) {
                System.out.println(ConsoleTheme.warn("[NOTE] You have no technical skills registered. Please register skills to qualify."));
            }

            System.out.println();
            System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
            System.out.println(ConsoleTheme.text("                  1. CHECK SKILL MATCH"));
            if (overallEligible) {
                System.out.println(ConsoleTheme.text("                  2. APPLY FOR THIS JOB"));
            }
            System.out.println(ConsoleTheme.text("                  0. BACK"));
            System.out.println(ConsoleTheme.border("================================================================"));

            System.out.print(ConsoleTheme.prompt("Enter your choice: "));
            if (!scanner.hasNextLine()) {
                return;
            }
            String choice = scanner.nextLine().trim();
            if (System.console() == null) {
                System.out.println(choice);
            }
            System.out.println(ConsoleTheme.border("================================================================"));

            if ("1".equals(choice)) {
                showSkillMatchResult(student, job, scanner);
            } else if ("2".equals(choice) && overallEligible) {
                showApplicationConfirmationForJob(scanner, student, job);
                return;
            } else if ("0".equals(choice)) {
                return;
            } else {
                if (overallEligible) {
                    System.out.println(ConsoleTheme.warn("[WARN] Invalid option. Please enter 1 to Check Skill Match, 2 to Apply, or 0 to Back."));
                } else {
                    System.out.println(ConsoleTheme.warn("[WARN] Invalid option. Please enter 1 to Check Skill Match or 0 to Back."));
                }
                System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
            }
        }
    }

    /**
     * Displays Page 9 Part B — Skill Match Result for a student and job opening.
     */
    public void showSkillMatchResult(Student student, Job job, Scanner scanner) {
        clearConsole();
        renderSkillMatchResultHeader();

        System.out.println("Student Name:           " + (student.getFullName() != null ? student.getFullName() : "N/A"));
        System.out.println("Job ID:                 " + job.getId());
        System.out.println();
        System.out.println("Skill Analysis:");
        System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
        System.out.printf("%-24s%-19s%s%n", "Required Skill", "Student Skill", "Result");
        System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));

        List<String> requiredSkills = job.getRequiredSkills() != null ? job.getRequiredSkills() : new ArrayList<>();
        List<String> studentSkills = student.getSkills() != null ? student.getSkills() : new ArrayList<>();

        int matchedCount = 0;
        List<String> missingSkills = new ArrayList<>();

        if (requiredSkills.isEmpty()) {
            System.out.printf("%-24s%-19s%s%n", "None", "-", ConsoleTheme.green("MATCH"));
        } else {
            for (String req : requiredSkills) {
                String reqTrim = req.trim();
                String foundSkill = null;
                for (String s : studentSkills) {
                    if (s.trim().equalsIgnoreCase(reqTrim)) {
                        foundSkill = s.trim();
                        break;
                    }
                }
                if (foundSkill != null) {
                    matchedCount++;
                    System.out.printf("%-24s%-19s%s%n", reqTrim, foundSkill, ConsoleTheme.green("MATCH"));
                } else {
                    missingSkills.add(reqTrim);
                    System.out.printf("%-24s%-19s%s%n", reqTrim, "-", ConsoleTheme.red("MISSING"));
                }
            }
        }
        System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
        System.out.println();
        int totalSkills = requiredSkills.size();
        int matchPct = totalSkills > 0 ? (int) Math.round(((double) matchedCount / totalSkills) * 100.0) : 100;
        System.out.println("Matched Skills:         " + matchedCount + " / " + totalSkills);
        System.out.println("Skill Match:            " + ConsoleTheme.green(matchPct + "%"));
        System.out.println("Missing Skills:         " + (missingSkills.isEmpty() ? "None" : ConsoleTheme.red(String.join(", ", missingSkills))));
        if (studentSkills.isEmpty() && totalSkills > 0) {
            System.out.println(ConsoleTheme.warn("[NOTE] You have no technical skills registered on your student profile."));
        }
        System.out.println();
        System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));

        EligibilityResult result = recruitmentService.checkEligibility(student, job);
        if (result.isEligible()) {
            System.out.println(ConsoleTheme.text("                  1. APPLY FOR THIS JOB"));
        }
        System.out.println(ConsoleTheme.text("                  0. BACK"));
        System.out.println(ConsoleTheme.border("================================================================"));

        while (true) {
            System.out.print(ConsoleTheme.prompt("Enter your choice: "));
            if (!scanner.hasNextLine()) {
                return;
            }
            String choice = scanner.nextLine().trim();
            if (System.console() == null) {
                System.out.println(choice);
            }
            System.out.println(ConsoleTheme.border("================================================================"));

            if ("0".equals(choice)) {
                return;
            } else if ("1".equals(choice) && result.isEligible()) {
                showApplicationConfirmationForJob(scanner, student, job);
                return;
            } else {
                System.out.println(ConsoleTheme.warn("[WARN] Invalid option. Please enter 0 to Back."));
                System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
            }
        }
    }

    public void showSkillMatchResult(Student student, Job job) {
        showSkillMatchResult(student, job, new Scanner(System.in));
    }

    /**
     * Direct flow for Option 5 on Student Dashboard (Analyse Skill Match).
     */
    private void showSkillAnalysisFlow(Scanner scanner, Student student) {
        System.out.print("Enter Job ID: ");
        if (!scanner.hasNextLine()) {
            return;
        }
        String input = scanner.nextLine().trim();
        if (System.console() == null) {
            System.out.println(input);
        }
        if (input.isEmpty()) {
            System.out.println("[ERROR] Job ID cannot be empty.");
            return;
        }

        int jobId;
        try {
            jobId = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("[ERROR] Invalid Job ID. Must be numeric.");
            return;
        }

        try {
            Job job = jobDAO.findById(jobId);
            if (job == null) {
                System.out.println("[ERROR] Job with ID " + jobId + " not found.");
                return;
            }
            if (student.getSkills() == null || student.getSkills().isEmpty()) {
                student.setSkills(studentDAO.getStudentSkills(student.getId()));
            }
            showSkillMatchResult(student, job, scanner);
        } catch (SQLException e) {
            System.out.println("[ERROR] Database error fetching job: " + e.getMessage());
        }
    }

    // =========================================================================
    // APPLICATION CONFIRMATION (PAGE 10)
    // =========================================================================

    /**
     * Renders header for Page 10 — Application Confirmation matching exact design specifications.
     */
    public void renderApplicationConfirmationHeader() {
        System.out.println(ConsoleTheme.border("================================================================"));
        System.out.println(ConsoleTheme.header("                   NEXTSTEP | APPLICATION PORTAL"));
        System.out.println(ConsoleTheme.header("                    APPLICATION CONFIRMATION"));
        System.out.println(ConsoleTheme.border("================================================================"));
        System.out.println();
    }

    /**
     * Displays Page 10 — Application Confirmation and processes interactive application submission.
     * 
     * @param scanner active Scanner
     * @param student authenticated Student profile
     */
    public void showApplicationConfirmation(Scanner scanner, Student student) {
        if (student == null) {
            System.out.println("[ERROR] Student session not found.");
            return;
        }

        System.out.print("Enter Job ID you want to apply for: ");
        if (!scanner.hasNextLine()) {
            return;
        }
        String input = scanner.nextLine().trim();
        if (System.console() == null) {
            System.out.println(input);
        }
        if (input.isEmpty()) {
            System.out.println("[ERROR] Job ID cannot be empty.");
            return;
        }
        if ("0".equals(input)) {
            return;
        }

        int jobId;
        try {
            jobId = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("[ERROR] Invalid Job ID. Must be numeric.");
            return;
        }

        Job job;
        try {
            job = jobDAO.findById(jobId);
            if (job == null) {
                System.out.println("[ERROR] Job with ID " + jobId + " not found.");
                return;
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Database error fetching job: " + e.getMessage());
            return;
        }

        showApplicationConfirmationForJob(scanner, student, job);
    }

    /**
     * Connected workflow for Page 10 — Application Confirmation for a selected job opening.
     */
    public void showApplicationConfirmationForJob(Scanner scanner, Student student, Job job) {
        if (student == null || job == null) {
            return;
        }

        // Ensure student skills are loaded
        if (student.getSkills() == null || student.getSkills().isEmpty()) {
            try {
                student.setSkills(studentDAO.getStudentSkills(student.getId()));
            } catch (SQLException ignored) {
            }
        }

        // Calculate unified eligibility dynamically using RecruitmentService as the single source of truth
        EligibilityResult eligResult = recruitmentService.checkEligibility(student, job);
        boolean isEligible = eligResult.isEligible();
        String eligibilityStr = isEligible ? ConsoleTheme.green("ELIGIBLE") : ConsoleTheme.red("NOT ELIGIBLE");

        int totalSkills = job.getRequiredSkills() != null ? job.getRequiredSkills().size() : 0;
        int matchedCount = eligResult.getMatchedSkills().size();
        int matchPct = totalSkills > 0 ? (int) Math.round(((double) matchedCount / totalSkills) * 100.0) : 100;
        String skillMatchStr = matchPct + "%";

        // If not eligible, explain why and block application
        if (!isEligible) {
            clearConsole();
            renderApplicationConfirmationHeader();

            System.out.println("Student ID:          " + (student.getStudentId() != null ? student.getStudentId() : "N/A"));
            System.out.println("Job ID:              " + job.getId());
            System.out.println("Job Role:            " + job.getTitle());
            System.out.println("Eligibility:         " + eligibilityStr);
            System.out.println("Skill Match:         " + skillMatchStr);
            System.out.println();
            System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
            System.out.println(ConsoleTheme.red("[BLOCKED] Application rejected: You are not eligible to apply for this job."));
            for (String reason : eligResult.getReasons()) {
                System.out.println(ConsoleTheme.red("  - " + reason));
            }
            System.out.println();
            System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
            System.out.println(ConsoleTheme.text("                 0. BACK"));
            System.out.println(ConsoleTheme.border("================================================================"));

            while (true) {
                System.out.print(ConsoleTheme.prompt("Enter your choice: "));
                if (!scanner.hasNextLine()) {
                    return;
                }
                String choice = scanner.nextLine().trim();
                if (System.console() == null) {
                    System.out.println(choice);
                }
                System.out.println(ConsoleTheme.border("================================================================"));

                if ("0".equals(choice)) {
                    System.out.println("\nApplication cancelled. Returning to Student Dashboard...");
                    return;
                } else {
                    System.out.println(ConsoleTheme.red("[BLOCKED] Application rejected: Ineligible student cannot apply."));
                    return;
                }
            }
        }

        while (true) {
            clearConsole();
            renderApplicationConfirmationHeader();

            System.out.println("Student ID:          " + (student.getStudentId() != null ? student.getStudentId() : "N/A"));
            System.out.println("Job ID:              " + job.getId());
            System.out.println("Job Role:            " + job.getTitle());
            System.out.println("Eligibility:         " + eligibilityStr);
            System.out.println("Skill Match:         " + skillMatchStr);
            System.out.println();
            System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
            System.out.println(ConsoleTheme.text("Are you sure you want to apply?"));
            System.out.println();
            System.out.println(ConsoleTheme.text("                 1. CONFIRM APPLICATION"));
            System.out.println(ConsoleTheme.text("                 0. CANCEL"));
            System.out.println();
            System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));

            System.out.print(ConsoleTheme.prompt("Enter your choice: "));
            if (!scanner.hasNextLine()) {
                return;
            }
            String choice = scanner.nextLine().trim();
            if (System.console() == null) {
                System.out.println(choice);
            }
            System.out.println(ConsoleTheme.border("================================================================"));

            if ("1".equals(choice)) {
                try {
                    Application app = recruitmentService.applyForJob(student, job.getId());
                    renderApplicationConfirmationHeader();
                    System.out.println(ConsoleTheme.success("SUCCESS: Application " + app.getId() + " submitted!"));
                    System.out.println(ConsoleTheme.header("           ✔ APPLICATION TRANSMITTED SUCCESSFULLY!"));
                    System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
                    System.out.println("  Official Application ID : " + app.getId());
                    System.out.println("  Status: " + app.getStatus());
                    System.out.println("  Company: " + (app.getCompanyName() != null ? app.getCompanyName() : job.getCompanyName()) +
                            " | Role: " + (app.getJobTitle() != null ? app.getJobTitle() : job.getTitle()));
                    System.out.println("  Next Step: The Placement Cell will review your application.");
                    System.out.println("  Track real-time status in Option 4: 'My Applications'.");
                    System.out.println(ConsoleTheme.border("================================================================"));
                    return;
                } catch (IllegalStateException e) {
                    System.out.println(ConsoleTheme.red("\n[BLOCKED] Application rejected: " + e.getMessage()));
                    return;
                } catch (IllegalArgumentException e) {
                    System.out.println(ConsoleTheme.red("\n[ERROR] Invalid application request: " + e.getMessage()));
                    return;
                } catch (SQLException e) {
                    System.out.println(ConsoleTheme.red("\n[ERROR] Database error processing application: " + e.getMessage()));
                    return;
                }
            } else if ("0".equals(choice)) {
                System.out.println("\nApplication cancelled. Returning to Student Dashboard...");
                return;
            } else {
                System.out.println(ConsoleTheme.warn("[WARN] Invalid option. Please enter 1 to Confirm Application or 0 to Cancel."));
                System.out.println(ConsoleTheme.divider("----------------------------------------------------------------"));
            }
        }
    }

    public void showApplicationConfirmation(Scanner scanner) {
        showApplicationConfirmation(scanner, null);
    }

    // =========================================================================
    // APPLICATION RECORDS (PAGE 11)
    // =========================================================================

    /**
     * Renders header for Page 11 — Application Records matching exact design specifications.
     */
    public void renderApplicationRecordsHeader() {
        System.out.println(ConsoleTheme.border("================================================================"));
        System.out.println(ConsoleTheme.header("                NEXTSTEP | APPLICATION MANAGEMENT"));
        System.out.println(ConsoleTheme.header("                     APPLICATION RECORDS"));
        System.out.println(ConsoleTheme.border("================================================================"));
        System.out.println();
    }

    /**
     * Renders footer and action options for Page 11 — Application Records.
     */
    public void renderApplicationRecordsFooter() {
        System.out.println(ConsoleTheme.divider("---------------------------------------------------------"));
        System.out.println(ConsoleTheme.text("                 1. VIEW DETAILS ") + ConsoleTheme.comingSoon());
        System.out.println(ConsoleTheme.text("                 0. BACK"));
        System.out.println(ConsoleTheme.border("================================================================"));
    }

    /**
     * Displays Page 11 — Application Records for authenticated placement officers.
     * 
     * @param scanner active Scanner
     * @param officer authenticated Placement Officer User account
     */
    public void showApplicationRecords(Scanner scanner, User officer) {
        if (officer == null || officer.getRole() != Role.PLACEMENT_OFFICER) {
            System.out.println("[SECURITY ERROR] Access denied: Only placement officers can view application records.");
            return;
        }

        while (true) {
            clearConsole();
            renderApplicationRecordsHeader();

            List<Application> apps;
            try {
                apps = recruitmentService.getAllApplications(officer);
            } catch (SQLException e) {
                System.out.println("[ERROR] Failed to load application records: " + e.getMessage());
                return;
            }

            System.out.println("Application ID   Student ID    Student Name     Status");
            System.out.println(ConsoleTheme.divider("---------------------------------------------------------"));

            if (apps.isEmpty()) {
                System.out.println("No student applications have been submitted yet.");
            } else {
                for (Application a : apps) {
                    String appIdStr = String.valueOf(a.getId());
                    String rollNo = (a.getStudentRollNumber() != null) ? a.getStudentRollNumber() : String.valueOf(a.getStudentId());
                    String name = (a.getStudentName() != null) ? a.getStudentName() : "N/A";
                    if (name.length() > 16) {
                        name = name.substring(0, 13) + "...";
                    }
                    String statusStr = (a.getStatus() != null) ? a.getStatus().name() : "N/A";
                    System.out.printf("%-17s%-14s%-17s%s%n", appIdStr, rollNo, name, statusStr);
                }
            }
            System.out.println();
            renderApplicationRecordsFooter();

            while (true) {
                System.out.print(ConsoleTheme.prompt("Enter your choice: "));
                if (!scanner.hasNextLine()) {
                    return;
                }
                String choice = scanner.nextLine().trim();
                if (System.console() == null) {
                    System.out.println(choice);
                }
                System.out.println(ConsoleTheme.border("================================================================"));

                if ("1".equals(choice)) {
                    System.out.println(ConsoleTheme.dim("\nApplication details view is not implemented yet."));
                    break; // Re-render Page 11
                } else if ("0".equals(choice)) {
                    return; // Return to Placement Officer Dashboard
                } else {
                    System.out.println(ConsoleTheme.warn("[WARN] Invalid option. Please enter 1 to View Details or 0 to Back."));
                    System.out.println(ConsoleTheme.divider("---------------------------------------------------------"));
                }
            }
        }
    }

    public void showApplicationRecords(Scanner scanner) {
        showApplicationRecords(scanner, null);
    }

    // =========================================================================
    // CONSOLE INPUT HELPERS
    // =========================================================================

    private static String readString(Scanner scanner, String prompt) {
        System.out.print(ConsoleTheme.prompt(prompt));
        return scanner.nextLine().trim();
    }

    private static String readNonEmptyString(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(ConsoleTheme.prompt(prompt));
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println(ConsoleTheme.warn("[WARN] Input cannot be blank. Please try again."));
        }
    }

    private static int readInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(ConsoleTheme.prompt(prompt));
            String line = scanner.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println(ConsoleTheme.warn("[WARN] Please enter a valid whole number."));
            }
        }
    }

    private static double readDouble(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(ConsoleTheme.prompt(prompt));
            String line = scanner.nextLine().trim();
            try {
                return Double.parseDouble(line);
            } catch (NumberFormatException e) {
                System.out.println(ConsoleTheme.warn("[WARN] Please enter a valid decimal number."));
            }
        }
    }
}
