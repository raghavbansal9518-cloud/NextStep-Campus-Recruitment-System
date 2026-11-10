package com.nextstep;

import com.nextstep.dao.ApplicationDAO;
import com.nextstep.dao.JobDAO;
import com.nextstep.dao.StudentDAO;
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
import com.nextstep.util.ValidationUtil;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
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

    public Main() {
        this.authService = new AuthService();
        this.recruitmentService = new RecruitmentService();
        this.jobDAO = new JobDAO();
        this.studentDAO = new StudentDAO();
        this.applicationDAO = new ApplicationDAO();
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
            System.out.print("Enter your choice: ");
            if (!scanner.hasNextLine()) {
                return false;
            }
            String input = scanner.nextLine().trim();
            if (System.console() == null) {
                System.out.println(input);
            }
            System.out.println("================================================================");

            if ("1".equals(input)) {
                return true;
            } else if ("0".equals(input)) {
                System.out.println("\nThank you for using NextStep. Goodbye!");
                return false;
            } else {
                System.out.println("[WARN] Invalid choice. Please enter 1 to Continue or 0 to Exit.");
                System.out.println("----------------------------------------------------------------");
            }
        }
    }

    /**
     * Renders the Welcome Page console layout matching exact design specifications.
     */
    public void renderWelcomeScreen() {
        System.out.println("================================================================");
        System.out.println("                         NEXTSTEP");
        System.out.println("                  CAMPUS RECRUITMENT SYSTEM");
        System.out.println("================================================================");
        System.out.println();
        System.out.println("                     YOUR CAREER STARTS HERE");
        System.out.println();
        System.out.println("                  1. Continue");
        System.out.println("                  0. Exit");
        System.out.println();
        System.out.println("----------------------------------------------------------------");
    }

    /**
     * Clears the console screen if supported by the terminal environment.
     * Provides safe fallback for non-interactive or unsupported terminals.
     */
    public static void clearConsole() {
        try {
            if (System.console() != null) {
                if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                    new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                } else {
                    System.out.print("\033[H\033[2J");
                    System.out.flush();
                }
            } else {
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
            System.out.print("Enter your choice: ");
            if (!scanner.hasNextLine()) {
                return 0;
            }
            String input = scanner.nextLine().trim();
            if (System.console() == null) {
                System.out.println(input);
            }
            System.out.println("================================================================");

            if ("1".equals(input)) {
                return 1;
            } else if ("2".equals(input)) {
                return 2;
            } else if ("0".equals(input)) {
                return 0;
            } else {
                System.out.println("[WARN] Invalid choice. Please enter 0, 1, or 2.");
                System.out.println("----------------------------------------------------------------");
            }
        }
    }

    /**
     * Renders the Account Portal (Page 2) console layout matching exact design specifications.
     */
    public void renderAccountPortalScreen() {
        System.out.println("================================================================");
        System.out.println("                         NEXTSTEP");
        System.out.println("                     ACCOUNT PORTAL");
        System.out.println("================================================================");
        System.out.println();
        System.out.println("                     SELECT YOUR ROLE");
        System.out.println();
        System.out.println("                  1. Student");
        System.out.println("                  2. Placement Officer");
        System.out.println("                  0. Back");
        System.out.println();
        System.out.println("----------------------------------------------------------------");
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
                    case 2 -> showStudentSignIn(scanner);
                    case 0 -> inStudentMenu = false;
                    default -> System.out.println("[WARN] Invalid option. Please try again.");
                }
            } else {
                switch (choice) {
                    case 1 -> showStudentSignIn(scanner);
                    case 0 -> inStudentMenu = false;
                    default -> System.out.println("[WARN] Invalid option. Please try again.");
                }
            }
        }
    }

    /**
     * Handles the Placement Officer authentication workflow.
     * Reuses existing authentication, first-run provisioning, and role verification.
     * 
     * @param scanner active Scanner
     */
    private void handlePlacementOfficerWorkflow(Scanner scanner) {
        checkFirstRunOfficerSetup(scanner);
        handleLogin(scanner, Role.PLACEMENT_OFFICER);
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
        while (true) {
            clearConsole();
            renderStudentSignUpHeader();

            String studentId = readField(scanner, "Student ID:             ");
            String fullName = readField(scanner, "Full Name:              ");
            String email = readField(scanner, "Email:                  ");
            String branch = readField(scanner, "Branch:                 ");
            String cgpaStr = readField(scanner, "CGPA:                   ");
            String gradYearStr = readField(scanner, "Graduation Year:        ");
            System.out.println();
            String username = readField(scanner, "Username:               ");
            String password = readPasswordField(scanner, "Password:               ");
            String confirmPassword = readPasswordField(scanner, "Confirm Password:       ");
            System.out.println();

            renderStudentSignUpFooter();

            while (true) {
                System.out.print("Enter your choice: ");
                if (!scanner.hasNextLine()) {
                    return false;
                }
                String choice = scanner.nextLine().trim();
                if (System.console() == null) {
                    System.out.println(choice);
                }
                System.out.println("================================================================");

                if ("1".equals(choice)) {
                    boolean created = processStudentRegistration(
                            studentId, fullName, email, branch, cgpaStr, gradYearStr,
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
                        break; // re-run outer loop to show form again
                    }
                } else if ("0".equals(choice)) {
                    return false; // return to Student Menu
                } else {
                    System.out.println("[WARN] Invalid choice. Please enter 1 to Create Account or 0 to Back.");
                    System.out.println("----------------------------------------------------------------");
                }
            }
        }
    }

    /**
     * Renders header for Page 3 — Student Sign Up matching exact design specifications.
     */
    public void renderStudentSignUpHeader() {
        System.out.println("================================================================");
        System.out.println("                    NEXTSTEP | STUDENT PORTAL");
        System.out.println("                       STUDENT SIGN UP");
        System.out.println("================================================================");
        System.out.println();
    }

    /**
     * Renders footer and action options for Page 3 — Student Sign Up.
     */
    public void renderStudentSignUpFooter() {
        System.out.println("----------------------------------------------------------------");
        System.out.println("                  1. CREATE ACCOUNT");
        System.out.println("                  0. BACK");
        System.out.println("================================================================");
    }

    /**
     * Reads a field with prompt, echoing input in non-interactive / automated environments.
     */
    private String readField(Scanner scanner, String prompt) {
        System.out.print(prompt);
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
        System.out.print(prompt);
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
                                               String branch, String cgpaStr, String gradYearStr,
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
        if (ValidationUtil.isNullOrBlank(branch)) {
            System.out.println("\n[ERROR] Branch cannot be empty.");
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

        // 5. Transactional Persistence via AuthService
        try {
            Student registered = authService.registerStudent(
                    username, password, email, studentId, fullName, branch, cgpa, graduationYear
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
            System.out.println("================================================================");

            while (true) {
                System.out.print("Enter your choice: ");
                if (!scanner.hasNextLine()) {
                    return false;
                }
                String choice = scanner.nextLine().trim();
                if (System.console() == null) {
                    System.out.println(choice);
                }
                System.out.println("================================================================");

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
                    System.out.println("[WARN] Invalid choice. Please enter 1 to Sign In or 0 to Back.");
                    System.out.println("----------------------------------------------------------------");
                }
            }
        }
    }

    /**
     * Renders header for Page 4 — Student Sign In matching exact design specifications.
     */
    public void renderStudentSignInHeader() {
        System.out.println("================================================================");
        System.out.println("                    NEXTSTEP | STUDENT PORTAL");
        System.out.println("                        STUDENT SIGN IN");
        System.out.println("================================================================");
        System.out.println();
    }

    /**
     * Renders actions and separator for Page 4 — Student Sign In.
     */
    public void renderStudentSignInActions() {
        System.out.println("                  1. SIGN IN");
        System.out.println("                  0. BACK");
        System.out.println();
        System.out.println("----------------------------------------------------------------");
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

    public boolean handleStudentRegistration(Scanner scanner) {
        return showStudentSignUp(scanner);
    }

    public void handleLogin(Scanner scanner) {
        handleLogin(scanner, null);
    }

    public void handleLogin(Scanner scanner, Role expectedRole) {
        if (expectedRole == Role.STUDENT) {
            showStudentSignIn(scanner);
            return;
        }
        String roleHeader = expectedRole == null ? "USER LOGIN" : "PLACEMENT OFFICER LOGIN";
        System.out.println("\n--- " + roleHeader + " ---");
        String username = readNonEmptyString(scanner, "Username: ");
        String password = readNonEmptyString(scanner, "Password: ");

        try {
            User user = authService.login(username, password);
            if (user == null) {
                System.out.println("\n[ERROR] Invalid credentials. Please check your username and password.");
                return;
            }

            if (expectedRole != null && user.getRole() != expectedRole) {
                String roleName = expectedRole == Role.STUDENT ? "Student" : "Placement Officer";
                System.out.println("\n[ERROR] Access denied. This account does not have " + roleName + " privileges.");
                return;
            }

            if (user.getRole() == Role.STUDENT) {
                Student student = studentDAO.findByUserId(user.getId());
                if (student == null) {
                    System.out.println("\n[ERROR] Student profile not found for this account.");
                    return;
                }
                System.out.println("\n[SUCCESS] Login successful! Welcome, " + student.getFullName() + ".");
                studentPortal(scanner, student, user);
            } else if (user.getRole() == Role.PLACEMENT_OFFICER) {
                System.out.println("\n[SUCCESS] Login successful! Welcome, Placement Officer " + user.getUsername() + ".");
                placementOfficerPortal(scanner, user);
            } else {
                System.out.println("\n[ERROR] Unrecognized user role: " + user.getRole());
            }
        } catch (SQLException e) {
            System.out.println("\n[ERROR] Database error during login: " + e.getMessage());
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
    // STUDENT PORTAL
    // =========================================================================

    private void studentPortal(Scanner scanner, Student student, User user) {
        boolean inPortal = true;
        while (inPortal) {
            System.out.println("\n================ STUDENT PORTAL ================");
            System.out.println("Logged in as: " + student.getFullName() + " (" + student.getStudentId() + ")");
            System.out.println("Branch: " + student.getBranch() + " | CGPA: " + student.getCgpa() + " | Backlogs: " + student.getActiveBacklogs());
            System.out.println("------------------------------------------------");
            System.out.println("1. View Available Jobs");
            System.out.println("2. View Job Details");
            if (PresentationConfig.isStudentCheckEligibilityVisible()) {
                System.out.println("3. Check Eligibility for a Job");
                System.out.println("4. Apply for a Job");
                System.out.println("5. View My Applications");
                System.out.println("6. Logout");
            } else {
                System.out.println("3. Apply for a Job");
                System.out.println("4. View My Applications");
                System.out.println("5. Logout");
            }
            System.out.println("================================================");

            int choice = readInt(scanner, "Enter your choice: ");

            if (PresentationConfig.isStudentCheckEligibilityVisible()) {
                switch (choice) {
                    case 1 -> viewAvailableJobs();
                    case 2 -> viewJobDetails(scanner);
                    case 3 -> checkStudentJobEligibility(scanner, student);
                    case 4 -> applyForJobFlow(scanner, student);
                    case 5 -> viewStudentApplications(student);
                    case 6 -> {
                        System.out.println("\nLogging out from Student Portal...");
                        inPortal = false;
                    }
                    default -> System.out.println("[WARN] Invalid option. Please try again.");
                }
            } else {
                switch (choice) {
                    case 1 -> viewAvailableJobs();
                    case 2 -> viewJobDetails(scanner);
                    case 3 -> applyForJobFlow(scanner, student);
                    case 4 -> viewStudentApplications(student);
                    case 5 -> {
                        System.out.println("\nLogging out from Student Portal...");
                        inPortal = false;
                    }
                    default -> System.out.println("[WARN] Invalid option. Please try again.");
                }
            }
        }
    }

    private void viewAvailableJobs() {
        System.out.println("\n--- AVAILABLE RECRUITMENT DRIVES ---");
        try {
            // Demonstrates lambda sorting (highest CTC first)
            List<Job> jobs = recruitmentService.getJobsSortedByCtc();
            long totalDrives = recruitmentService.countOpenJobs();
            List<String> hiringCompanies = recruitmentService.getDistinctHiringCompanies();

            System.out.println("Active Drives: " + totalDrives + " | Hiring Companies: " + String.join(", ", hiringCompanies));
            System.out.println("--------------------------------------------------------------------------------------------------");
            if (jobs.isEmpty()) {
                System.out.println("No open job positions available at this moment.");
                return;
            }

            System.out.printf("%-5s | %-32s | %-28s | %-10s | %-15s%n",
                    "ID", "Job Title", "Company", "CTC (LPA)", "Location");
            System.out.println("--------------------------------------------------------------------------------------------------");
            for (Job j : jobs) {
                System.out.printf("%-5d | %-32s | %-28s | %-10.2f | %-15s%n",
                        j.getId(), j.getTitle(), j.getCompanyName(), j.getCtcLpa(), j.getLocation());
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Failed to load jobs: " + e.getMessage());
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
        System.out.println("\n--- MY APPLICATION HISTORY ---");
        try {
            List<Application> apps = recruitmentService.getStudentApplications(student.getId());
            if (apps.isEmpty()) {
                System.out.println("You have not applied for any campus jobs yet.");
                return;
            }

            System.out.printf("%-6s | %-30s | %-25s | %-14s | %-20s%n",
                    "App ID", "Job Title", "Company", "Status", "Applied At");
            System.out.println("--------------------------------------------------------------------------------------------------");
            for (Application a : apps) {
                String appliedAtStr = (a.getAppliedAt() != null) ? a.getAppliedAt().toString() : "N/A";
                System.out.printf("%-6d | %-30s | %-25s | %-14s | %-20s%n",
                        a.getId(), a.getJobTitle(), a.getCompanyName(), a.getStatus(), appliedAtStr);
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Failed to load application history: " + e.getMessage());
        }
    }

    // =========================================================================
    // PLACEMENT OFFICER PORTAL
    // =========================================================================

    private void placementOfficerPortal(Scanner scanner, User officer) {
        boolean inPortal = true;
        while (inPortal) {
            System.out.println("\n=========== PLACEMENT OFFICER PORTAL ===========");
            System.out.println("Logged in Officer: " + officer.getUsername() + " (" + officer.getEmail() + ")");
            System.out.println("------------------------------------------------");
            System.out.println("1. View Available Jobs");
            System.out.println("2. View All Student Applications");
            System.out.println("3. Update Application Status");

            int nextOption = 4;
            int summaryOption = -1;
            int csvExportOption = -1;
            int csvArchiveOption = -1;

            if (PresentationConfig.isOfficerRecruitmentSummaryVisible()) {
                summaryOption = nextOption++;
                System.out.println(summaryOption + ". View Recruitment Summary");
            }
            if (PresentationConfig.isOfficerCsvExportVisible()) {
                csvExportOption = nextOption++;
                System.out.println(csvExportOption + ". Export Applications Report to CSV");

                csvArchiveOption = nextOption++;
                System.out.println(csvArchiveOption + ". Process & Archive Applications CSV");
            }

            int logoutOption = nextOption;
            System.out.println(logoutOption + ". Logout");
            System.out.println("================================================");

            int choice = readInt(scanner, "Enter your choice: ");

            if (choice == 1) {
                viewAvailableJobs();
            } else if (choice == 2) {
                viewAllApplicationsFlow(officer);
            } else if (choice == 3) {
                updateApplicationStatusFlow(scanner, officer);
            } else if (choice == summaryOption && summaryOption != -1) {
                viewRecruitmentSummaryFlow(officer);
            } else if (choice == csvExportOption && csvExportOption != -1) {
                exportApplicationsToCsvFlow(officer);
            } else if (choice == csvArchiveOption && csvArchiveOption != -1) {
                processAndArchiveCsvFlow(scanner, officer);
            } else if (choice == logoutOption) {
                System.out.println("\nLogging out from Placement Officer Portal...");
                inPortal = false;
            } else {
                System.out.println("[WARN] Invalid option. Please try again.");
            }
        }
    }

    private void viewAllApplicationsFlow(User officer) {
        System.out.println("\n--- ALL CAMPUS APPLICATIONS ---");
        try {
            List<Application> apps = recruitmentService.getAllApplications(officer);
            if (apps.isEmpty()) {
                System.out.println("No student applications have been submitted yet.");
                return;
            }

            System.out.printf("%-6s | %-12s | %-20s | %-26s | %-22s | %-12s%n",
                    "App ID", "Roll No", "Student Name", "Job Title", "Company", "Status");
            System.out.println("---------------------------------------------------------------------------------------------------------------");
            for (Application a : apps) {
                String rollNo = (a.getStudentRollNumber() != null) ? a.getStudentRollNumber() : String.valueOf(a.getStudentId());
                String name = (a.getStudentName() != null) ? a.getStudentName() : "N/A";
                System.out.printf("%-6d | %-12s | %-20s | %-26s | %-22s | %-12s%n",
                        a.getId(), rollNo, name, a.getJobTitle(), a.getCompanyName(), a.getStatus());
            }
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
            System.out.println("----------------------------------------------");
            System.out.println("Lifecycle Breakdown (groupingBy):");
            for (ApplicationStatus status : ApplicationStatus.values()) {
                long count = summary.getOrDefault(status, 0L);
                System.out.printf("  %-15s : %d%n", status.name(), count);
            }
            System.out.println("----------------------------------------------");
            System.out.println("Placement Partition (partitioningBy):");
            long placed = placementPartition.getOrDefault(true, 0L);
            long inProcess = placementPartition.getOrDefault(false, 0L);
            System.out.printf("  Placed (SELECTED)      : %d%n", placed);
            System.out.printf("  In-Progress / Other    : %d%n", inProcess);
            System.out.println("----------------------------------------------");
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
    // CONSOLE INPUT HELPERS
    // =========================================================================

    private static String readString(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static String readNonEmptyString(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("[WARN] Input cannot be blank. Please try again.");
        }
    }

    private static int readInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("[WARN] Please enter a valid whole number.");
            }
        }
    }

    private static double readDouble(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                return Double.parseDouble(line);
            } catch (NumberFormatException e) {
                System.out.println("[WARN] Please enter a valid decimal number.");
            }
        }
    }
}
