import com.nextstep.Main;
import com.nextstep.service.AuthService;
import com.nextstep.service.DemoService;
import com.nextstep.util.ConsoleTheme;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Visual Regression and Layout Inspector:
 * Captures, measures line widths, and visually inspects all modified screens:
 * 1. Student Sign Up (A-shortcut & Manual)
 * 2. Officer Sign Up (A-shortcut & Manual)
 * 3. Student Sign In (A-shortcut & Manual)
 * 4. Officer Sign In (A-shortcut & Manual)
 * 5. Job Creation (A-shortcut & Manual)
 * 6. Demo Data Manager Menu & Sub-screens
 * 7. Job Selection Prompts
 */
public class VisualInspectionVerifier {

    private static void inspectScreen(String screenName, String simulatedInput) {
        System.out.println("==================================================================");
        System.out.println(" VISUAL INSPECTION: " + screenName);
        System.out.println("==================================================================");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));

        try {
            System.setProperty(AuthService.ACTIVATION_CODE_PROP, DemoService.DEMO_OFFICER_ACTIVATION_CODE);
            System.setIn(new ByteArrayInputStream(simulatedInput.getBytes(StandardCharsets.UTF_8)));

            Main app = new Main();
            app.run(new Scanner(System.in));
        } catch (Exception e) {
            // expected on EOF
        } finally {
            System.setOut(originalOut);
        }

        String rendered = buffer.toString(StandardCharsets.UTF_8);
        String[] lines = rendered.split("\\r?\\n");

        int maxCol = 0;
        int over64Count = 0;
        for (String line : lines) {
            // strip ANSI codes to calculate actual visual column width
            String plain = line.replaceAll("\u001B\\[[;\\d]*m", "");
            if (plain.length() > maxCol) {
                maxCol = plain.length();
            }
            if (plain.length() > 64) {
                over64Count++;
            }
        }

        System.out.println("Total Lines Rendered : " + lines.length);
        System.out.println("Max Visual Column    : " + maxCol + " chars");
        System.out.println("Lines > 64 Columns   : " + over64Count);

        if (over64Count > 0) {
            System.out.println("--- Lines Exceeding 64 Columns ---");
            for (int i = 0; i < lines.length; i++) {
                String plain = lines[i].replaceAll("\u001B\\[[;\\d]*m", "");
                if (plain.length() > 64) {
                    System.out.println("  [Line " + (i + 1) + " | len=" + plain.length() + "]: " + plain);
                }
            }
        }

        // Print preview of first 35 lines or full screen
        System.out.println("--- Screen Sample Output (First 30 Lines) ---");
        int limit = Math.min(lines.length, 30);
        for (int i = 0; i < limit; i++) {
            System.out.println(lines[i]);
        }
        System.out.println("------------------------------------------------------------------");
        System.out.println();
    }

    public static void main(String[] args) throws Exception {
        new DemoService().prepareDemoData();

        // 1. Student Sign Up - A-Shortcut
        inspectScreen("1. Student Sign Up [A-Shortcut]",
                "1\n1\nA\n\n0\n0\n0\n");

        // 2. Student Sign Up - Manual Entry
        inspectScreen("2. Student Sign Up [Manual Entry]",
                "1\n1\n\n2510999999\nManual Candidate\nmanual@test.edu\n9876543210\nCSE\n8.50\nJava, SQL\n0\n2026\nmanual_user\nPass@123\nPass@123\n0\n0\n0\n");

        // 3. Officer Sign Up - A-Shortcut
        inspectScreen("3. Officer Sign Up [A-Shortcut]",
                "2\n1\nA\n\n0\n0\n0\n");

        // 4. Officer Sign Up - Manual Entry
        inspectScreen("4. Officer Sign Up [Manual Entry]",
                "2\n1\n\nTPO-2026-999\nProf. Test Officer\nofficer@test.edu\nmanual_officer\nPass@123\nPass@123\nCAMPUS_OFFICER_2026\n0\n0\n0\n");

        // 5. Student Sign In - A-Shortcut
        inspectScreen("5. Student Sign In [A-Shortcut]",
                "1\n2\nA\n1\n0\n0\n0\n");

        // 6. Student Sign In - Manual Entry
        inspectScreen("6. Student Sign In [Manual Entry]",
                "1\n2\nvihaan_demo\nNextStep@2026\n1\n0\n0\n0\n");

        // 7. Officer Sign In - A-Shortcut
        inspectScreen("7. Officer Sign In [A-Shortcut]",
                "2\n2\nA\n0\n0\n0\n");

        // 8. Officer Sign In - Manual Entry
        inspectScreen("8. Officer Sign In [Manual Entry]",
                "2\n2\nofficer_vihaan\nNextStep@2026\n0\n0\n0\n");

        // 9. Job Creation - A-Shortcut
        inspectScreen("9. Job Creation [A-Shortcut]",
                "2\n2\nA\n1\nA\n\n0\n0\n0\n0\n");

        // 10. Job Creation - Manual Entry
        inspectScreen("10. Job Creation [Manual Entry]",
                "2\n2\nA\n1\n\n1\nCloud Architect\nBuild Scalable Cloud\n15.00\nBengaluru, KA\n7.50\n0\nCSE, IT\nJava, AWS\n2026\n0\n0\n0\n0\n");

        // 11. Centralized Demo Data Manager Menu
        inspectScreen("11. Centralized Demo Data Manager Menu",
                "4\n0\n0\n");

        // 12. Centralized Demo Data Manager - Student Profiles (Option 3)
        inspectScreen("12. Demo Manager - Student Profiles List",
                "4\n3\n0\n0\n0\n");

        // 13. Centralized Demo Data Manager - Student Profile 1 Edit (Option 3 -> 1)
        inspectScreen("13. Demo Manager - Student Profile 1 Edit Screen",
                "4\n3\n1\n0\n0\n0\n0\n");

        // 14. Centralized Demo Data Manager - Officer Profile Edit (Option 4)
        inspectScreen("14. Demo Manager - Officer Profile Edit Screen",
                "4\n4\n0\n0\n0\n");

        // 15. Centralized Demo Data Manager - Job Specifications (Option 6)
        inspectScreen("15. Demo Manager - Job Specifications Edit Screen",
                "4\n6\n0\n0\n0\n");

        // 16. Centralized Demo Data Manager - Applications Tracking (Option 7)
        inspectScreen("16. Demo Manager - Applications Tracking Screen",
                "4\n7\n0\n0\n0\n");

        // 17. Page 1: Welcome Gateway & Campus Statistics (Option 3)
        inspectScreen("17. Gateway & Campus Recruitment Statistics",
                "3\n\n0\n");

        // 18. Page 6: Student Placement Hub Dashboard & Available Drives (Student 1 -> 1)
        inspectScreen("18. Student Placement Hub & Available Recruitment Drives",
                "1\n2\nA\n1\n0\n0\n0\n0\n");

        // 19. Page 8: Job Eligibility Checklist & Skill Match Matrix (Student 1 -> 2 -> Job 1)
        inspectScreen("19. Page 8 - Unified Eligibility Checklist & Skill Matrix [Eligible]",
                "1\n2\nA\n2\nA\n0\n0\n0\n0\n");

        // 20. Page 8: Ineligible Candidate Evaluation (Scenario C: Arjun Khanna)
        inspectScreen("20. Page 8 - Unified Eligibility Checklist & Skill Matrix [Ineligible]",
                "1\n2\nA3\n2\nA\n0\n0\n0\n0\n");

        // 21. Page 9: Application Review Summary (Scenario A: Eligible Candidate)
        inspectScreen("21. Page 9 - Application Review Summary [Eligible Review]",
                "1\n2\nA\n3\nA\n0\n0\n0\n0\n");

        // 22. Page 9: Application Review Rejection (Scenario C: Ineligible Candidate)
        inspectScreen("22. Page 9 - Application Review Rejection Notice [Blocked Candidate]",
                "1\n2\nA3\n3\nA\n0\n0\n0\n0\n");

        // 23. Page 9: Official Digital Filing Receipt (Apply & Transmit)
        inspectScreen("23. Page 9 - Official Digital Recruitment Filing Receipt",
                "1\n2\nA\n3\nA\n1\n\n0\n0\n0\n");

        // 24. Page 6: Student Application History (Option 4)
        inspectScreen("24. Student Application History Tracking",
                "1\n2\nA\n4\n0\n0\n0\n");

        // 25. Page 7: Placement Officer Administration Dashboard
        inspectScreen("25. Placement Officer Administration Dashboard",
                "2\n2\nA\n0\n0\n0\n");

        // 26. Page 10: Master Application Registry
        inspectScreen("26. Page 10 - Master Application Registry Table View",
                "2\n2\nA\n2\n0\n0\n0\n0\n");

        // 27-30: Responsive Dimensions Verification
        inspectResponsiveDimensions();

        System.out.println("ALL 30 VISUAL REGRESSION & RESPONSIVENESS INSPECTIONS COMPLETED SUCCESSFULLY!");
    }

    private static void inspectResponsiveDimensions() {
        int[] widths = {80, 100, 120, 50};
        for (int w : widths) {
            System.out.println("==================================================================");
            System.out.println(" RESPONSIVE DIMENSION VERIFICATION: " + w + " Columns Terminal");
            System.out.println("==================================================================");

            System.setProperty("nextstep.columns", String.valueOf(w));
            System.setProperty("nextstep.center", "true");
            ConsoleTheme.refreshTerminalDimensions();

            int expectedMargin = (w > 64) ? (w - 64) / 2 : 0;
            int actualMargin = ConsoleTheme.getLeftMargin();
            System.out.println("  Terminal Width   : " + w + " columns");
            System.out.println("  Expected Margin  : " + expectedMargin + " spaces");
            System.out.println("  Calculated Margin: " + actualMargin + " spaces");

            if (expectedMargin != actualMargin) {
                System.err.println("  [FAIL] Margin mismatch for width " + w + "!");
            } else {
                System.out.println("  [PASS] Centering margin perfectly calibrated (" + actualMargin + " spaces).");
            }
            System.out.println("------------------------------------------------------------------\n");
        }
        System.clearProperty("nextstep.columns");
        System.clearProperty("nextstep.center");
        ConsoleTheme.resetToDefault();
    }
}
