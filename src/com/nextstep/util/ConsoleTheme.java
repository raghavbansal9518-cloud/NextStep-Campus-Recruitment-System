package com.nextstep.util;

/**
 * ConsoleTheme
 * 
 * Centralized styling and ANSI colour theme for the NextStep console interface:
 * - Blue/Cyan: Main borders, horizontal dividers, and portal banners.
 * - Yellow: User input prompts and field labels during data entry.
 * - Green: Successful actions, confirmation messages, PASS results, and MATCH indicators.
 * - Red: Errors, blocked operations, FAIL results, and MISSING indicators.
 * - Dim / Gray: [Coming Soon] badges and secondary informational text.
 * 
 * Auto-detects terminal support: falls back cleanly to plain text when
 * running in non-interactive / automated testing environments, or when
 * explicitly disabled via -Dnextstep.ansi=false or NO_COLOR env var.
 */
public final class ConsoleTheme {

    private static boolean ansiEnabled = detectAnsiSupport();

    public static final String RESET  = "\u001B[0m";
    public static final String BLUE   = "\u001B[36m"; // Cyan/Blue for borders and separators
    public static final String YELLOW = "\u001B[33m"; // Yellow for user prompts
    public static final String GREEN  = "\u001B[32m"; // Green for success/PASS
    public static final String RED    = "\u001B[31m"; // Red for errors/FAIL
    public static final String DIM    = "\u001B[90m"; // Dim/gray for Coming Soon
    public static final String WHITE  = "\u001B[37m";

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
        return ansiEnabled ? BLUE + text + RESET : text;
    }

    public static String yellow(String text) {
        return ansiEnabled ? YELLOW + text + RESET : text;
    }

    public static String green(String text) {
        return ansiEnabled ? GREEN + text + RESET : text;
    }

    public static String red(String text) {
        return ansiEnabled ? RED + text + RESET : text;
    }

    public static String dim(String text) {
        return ansiEnabled ? DIM + text + RESET : text;
    }

    public static String white(String text) {
        return ansiEnabled ? WHITE + text + RESET : text;
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
}
