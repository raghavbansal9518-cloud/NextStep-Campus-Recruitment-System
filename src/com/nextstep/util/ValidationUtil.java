package com.nextstep.util;

/**
 * ValidationUtil
 * 
 * Lightweight, beginner-friendly validation methods for input verification.
 * Aligned with Semester 3 Java curriculum.
 */
public class ValidationUtil {

    /**
     * Checks if a string is null, empty, or contains only whitespace.
     */
    public static boolean isNullOrBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Validates that an email contains an '@' and a '.' after the '@'.
     * Simple, understandable validation without complex regular expressions.
     */
    public static boolean isValidEmail(String email) {
        if (isNullOrBlank(email)) {
            return false;
        }
        String trimmed = email.trim();
        int atIndex = trimmed.indexOf('@');
        int dotIndex = trimmed.lastIndexOf('.');
        return atIndex > 0 && dotIndex > atIndex + 1 && dotIndex < trimmed.length() - 1;
    }

    /**
     * Validates username length and characters (alphanumeric and underscores, 3-30 chars).
     */
    public static boolean isValidUsername(String username) {
        if (isNullOrBlank(username)) {
            return false;
        }
        String trimmed = username.trim();
        if (trimmed.length() < 3 || trimmed.length() > 30) {
            return false;
        }
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_') {
                return false;
            }
        }
        return true;
    }

    /**
     * Validates password minimum requirements (at least 6 characters).
     */
    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    /**
     * Validates that CGPA is within the academic range of 0.00 to 10.00.
     */
    public static boolean isValidCgpa(double cgpa) {
        return cgpa >= 0.0 && cgpa <= 10.0;
    }

    /**
     * Validates that active backlogs are non-negative.
     */
    public static boolean isValidBacklogs(int backlogs) {
        return backlogs >= 0;
    }

    /**
     * Validates that graduation year is within a realistic academic timeframe (e.g. 2020-2035).
     */
    public static boolean isValidGraduationYear(int year) {
        return year >= 2020 && year <= 2035;
    }
}
