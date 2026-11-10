package com.nextstep.util;

/**
 * ConsoleTheme
 * 
 * Centralized styling and 24-bit TrueColour ANSI theme for the NextStep console interface,
 * matched exactly against the reference Vantique specification:
 * - Borders and dividers: #6FC9FF (RGB 111, 201, 255) — Cyan / Sky Blue
 * - Prompts and input labels: #E3B341 (RGB 227, 179, 65) — Yellow / Gold
 * - Headings, menu options, and content text: #CAD5E5 (RGB 202, 213, 229) — Light Slate
 * - Terminal background: #131B28 (RGB 19, 27, 40) — Midnight Navy
 * - Success, confirmations, PASS, and MATCH: #56D364 (RGB 86, 211, 100) — Green
 * - Errors, blocked actions, FAIL, and MISSING: #F85149 (RGB 248, 81, 73) — Red
 * - Subdued notices and [Coming Soon] badges: #8B949E (RGB 139, 148, 158) — Subdued Slate
 * 
 * Auto-detects terminal support: falls back cleanly to plain text when
 * running in non-interactive / automated testing environments (pipes), or when
 * explicitly disabled via -Dnextstep.ansi=false or NO_COLOR env var.
 */
public final class ConsoleTheme {

    private static boolean ansiEnabled = detectAnsiSupport();

    public static final String RESET = "\u001B[0m";

    // 24-bit TrueColour Foreground Codes (38;2;R;G;B)
    public static final String CYAN_RGB   = "\u001B[38;2;111;201;255m"; // #6FC9FF Sky Blue / Cyan
    public static final String YELLOW_RGB = "\u001B[38;2;227;179;65m";  // #E3B341 Yellow / Gold
    public static final String SLATE_RGB  = "\u001B[38;2;202;213;229m"; // #CAD5E5 Light Slate
    public static final String GREEN_RGB  = "\u001B[38;2;86;211;100m";  // #56D364 Green (Success/PASS/MATCH)
    public static final String RED_RGB    = "\u001B[38;2;248;81;73m";   // #F85149 Red (Error/FAIL/MISSING)
    public static final String DIM_RGB    = "\u001B[38;2;139;148;158m"; // #8B949E Subdued Gray

    // 24-bit TrueColour Background Code (48;2;R;G;B)
    public static final String BG_NAVY    = "\u001B[48;2;19;27;40m";    // #131B28 Midnight Navy

    // Color aliases for backward compatibility and semantic clarity
    public static final String BLUE   = CYAN_RGB;
    public static final String CYAN   = CYAN_RGB;
    public static final String YELLOW = YELLOW_RGB;
    public static final String GREEN  = GREEN_RGB;
    public static final String RED    = RED_RGB;
    public static final String DIM    = DIM_RGB;
    public static final String WHITE  = SLATE_RGB;
    public static final String SLATE  = SLATE_RGB;

    private ConsoleTheme() {
        // Utility class
    }

    private static boolean detectAnsiSupport() {
        String prop = System.getProperty("nextstep.ansi");
        if (prop != null) {
            return Boolean.parseBoolean(prop);
        }
        String env = System.getenv("NEXTSTEP_ANSI");
        if (env != null) {
            return Boolean.parseBoolean(env);
        }
        if (System.getenv("NO_COLOR") != null) {
            return false;
        }
        return System.console() != null;
    }

    public static synchronized boolean isAnsiEnabled() {
        return ansiEnabled;
    }

    public static synchronized void setAnsiEnabled(boolean enabled) {
        ansiEnabled = enabled;
    }

    /**
     * Resets ANSI detection to default environment evaluation.
     */
    public static synchronized void resetToDefault() {
        ansiEnabled = detectAnsiSupport();
    }

    public static String cyan(String text) {
        return ansiEnabled ? CYAN_RGB + text + RESET : text;
    }

    public static String yellow(String text) {
        return ansiEnabled ? YELLOW_RGB + text + RESET : text;
    }

    public static String text(String text) {
        return ansiEnabled ? SLATE_RGB + text + RESET : text;
    }

    public static String slate(String text) {
        return text(text);
    }

    public static String white(String text) {
        return text(text);
    }

    public static String green(String text) {
        return ansiEnabled ? GREEN_RGB + text + RESET : text;
    }

    public static String red(String text) {
        return ansiEnabled ? RED_RGB + text + RESET : text;
    }

    public static String dim(String text) {
        return ansiEnabled ? DIM_RGB + text + RESET : text;
    }

    public static String border(String border) {
        return cyan(border);
    }

    public static String divider(String divider) {
        return cyan(divider);
    }

    public static String header(String text) {
        return cyan(text);
    }

    public static String prompt(String prompt) {
        return yellow(prompt);
    }

    public static String label(String text) {
        return yellow(text);
    }

    public static String success(String text) {
        return green(text);
    }

    public static String error(String text) {
        return red(text);
    }

    public static String warn(String text) {
        return yellow(text);
    }

    public static String comingSoon() {
        return dim("[Coming Soon]");
    }

    /**
     * Applies the midnight navy background color sequence if ANSI is supported.
     * Note: This affects terminal output stream during runtime and does not alter
     * the user's permanent terminal profile settings.
     */
    public static String applyBackground() {
        return ansiEnabled ? BG_NAVY : "";
    }
}
