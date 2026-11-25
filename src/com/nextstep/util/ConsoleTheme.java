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

    public static final int FRAME_WIDTH = 64;

    private static int terminalColumns = detectTerminalColumns();
    private static boolean centeringEnabled = detectCenteringSupport();
    private static AutoMarginOutputStream installedMarginStream = null;
    private static java.io.PrintStream originalSystemOut = null;

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

    private static int detectTerminalColumns() {
        String prop = System.getProperty("nextstep.columns");
        if (prop != null) {
            try {
                return Integer.parseInt(prop.trim());
            } catch (NumberFormatException ignored) {}
        }
        String env = System.getenv("COLUMNS");
        if (env != null) {
            try {
                return Integer.parseInt(env.trim());
            } catch (NumberFormatException ignored) {}
        }
        if (System.console() != null) {
            if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                int winCols = detectWindowsColumns();
                if (winCols > 0) return winCols;
            }
            return 80;
        }
        return FRAME_WIDTH;
    }

    private static int detectWindowsColumns() {
        try {
            Process p = new ProcessBuilder("cmd", "/c", "mode con").start();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("Columns:")) {
                        String[] parts = line.split(":");
                        if (parts.length > 1) {
                            return Integer.parseInt(parts[1].trim());
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return 80;
    }

    private static boolean detectCenteringSupport() {
        String prop = System.getProperty("nextstep.center");
        if (prop != null) {
            return Boolean.parseBoolean(prop);
        }
        String env = System.getenv("NEXTSTEP_CENTER");
        if (env != null) {
            return Boolean.parseBoolean(env);
        }
        return System.console() != null && terminalColumns > FRAME_WIDTH;
    }

    public static synchronized int getTerminalColumns() {
        return terminalColumns;
    }

    public static synchronized void setTerminalColumns(int columns) {
        terminalColumns = Math.max(FRAME_WIDTH, columns);
        if (installedMarginStream != null) {
            installedMarginStream.setMargin(getLeftMargin());
        }
    }

    public static synchronized boolean isCenteringEnabled() {
        return centeringEnabled;
    }

    public static synchronized void setCenteringEnabled(boolean enabled) {
        centeringEnabled = enabled;
        if (installedMarginStream != null) {
            installedMarginStream.setMargin(getLeftMargin());
        }
    }

    public static synchronized int getLeftMargin() {
        if (!centeringEnabled || terminalColumns <= FRAME_WIDTH) {
            return 0;
        }
        return Math.max(0, (terminalColumns - FRAME_WIDTH) / 2);
    }

    public static synchronized String margin() {
        int m = getLeftMargin();
        if (m <= 0) return "";
        return " ".repeat(m);
    }

    public static synchronized void refreshTerminalDimensions() {
        terminalColumns = detectTerminalColumns();
        centeringEnabled = detectCenteringSupport();
        if (installedMarginStream != null) {
            installedMarginStream.setMargin(getLeftMargin());
            installedMarginStream.resetLineStart();
        }
    }

    public static synchronized void resetLineStart() {
        if (installedMarginStream != null) {
            installedMarginStream.resetLineStart();
        }
    }

    /**
     * Clears the console screen reliably across platforms and ANSI modes,
     * writing directly to the underlying raw output stream so that no extraneous
     * margin bytes precede the screen clear, and resets line start tracking
     * so that the very next line rendered begins with the proper left margin.
     */
    public static synchronized void clearScreen() {
        refreshTerminalDimensions();
        java.io.PrintStream raw = (originalSystemOut != null) ? originalSystemOut : System.out;
        if (ansiEnabled) {
            raw.print("\033[H\033[2J\033[3J");
            raw.flush();
        } else if (System.console() != null && System.getProperty("os.name", "").toLowerCase().contains("win")) {
            try {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } catch (Exception ignored) {}
        } else if (System.console() != null) {
            raw.print("\033[H\033[2J\033[3J");
            raw.flush();
        }
        if (installedMarginStream != null) {
            installedMarginStream.resetLineStart();
        }
    }

    public static synchronized void installAutoMargin() {
        installAutoMargin(System.out);
    }

    public static synchronized void installAutoMargin(java.io.PrintStream targetOut) {
        if (installedMarginStream != null) {
            return;
        }
        originalSystemOut = targetOut;
        int margin = getLeftMargin();
        installedMarginStream = new AutoMarginOutputStream(targetOut, margin);
        System.setOut(new java.io.PrintStream(installedMarginStream, true, java.nio.charset.StandardCharsets.UTF_8));
    }

    public static synchronized void uninstallAutoMargin() {
        if (originalSystemOut != null) {
            System.setOut(originalSystemOut);
            originalSystemOut = null;
            installedMarginStream = null;
        }
    }

    public static class AutoMarginOutputStream extends java.io.FilterOutputStream {
        private volatile int margin;
        private boolean atLineStart = true;
        private volatile byte[] marginBytes;

        public AutoMarginOutputStream(java.io.OutputStream out, int margin) {
            super(out);
            setMargin(margin);
        }

        public synchronized void setMargin(int newMargin) {
            this.margin = Math.max(0, newMargin);
            this.marginBytes = " ".repeat(this.margin).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }

        public synchronized void resetLineStart() {
            this.atLineStart = true;
        }

        @Override
        public synchronized void write(int b) throws java.io.IOException {
            if (margin > 0 && atLineStart && b != '\n' && b != '\r') {
                out.write(marginBytes);
                atLineStart = false;
            }
            out.write(b);
            if (b == '\n' || b == '\r') {
                atLineStart = true;
            }
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) throws java.io.IOException {
            for (int i = 0; i < len; i++) {
                write(b[off + i]);
            }
        }
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
        uninstallAutoMargin();
        ansiEnabled = detectAnsiSupport();
        terminalColumns = detectTerminalColumns();
        centeringEnabled = detectCenteringSupport();
    }

    public static String cyan(String text) {
        return ansiEnabled ? CYAN_RGB + text + RESET : text;
    }

    public static String blue(String text) {
        return cyan(text);
    }

    public static String yellow(String text) {
        return ansiEnabled ? YELLOW_RGB + text + RESET : text;
    }

    public static String gold(String text) {
        return yellow(text);
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
