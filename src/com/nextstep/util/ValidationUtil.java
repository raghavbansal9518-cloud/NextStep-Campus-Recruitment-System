package com.nextstep.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ValidationUtil
 * 
 * Lightweight, beginner-friendly validation and generic utility methods.
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

    /**
     * Parses a comma-separated string of items (e.g. technical skills) into a clean List<String>.
     * Removes leading and trailing spaces, ignores empty tokens, and removes duplicates
     * without regard to case while preserving original input case.
     */
    public static List<String> parseCommaSeparatedList(String input) {
        if (isNullOrBlank(input)) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>();
        String[] tokens = input.split(",");
        for (String token : tokens) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                boolean duplicate = false;
                for (String existing : list) {
                    if (existing.equalsIgnoreCase(trimmed)) {
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) {
                    list.add(trimmed);
                }
            }
        }
        return list;
    }

    /**
     * Generic pagination utility method to safely slice any collection of items.
     * Demonstrates Java Generic Methods with type parameter <T> (Course Handout: Lectures 47–48).
     * 
     * @param <T> Element type
     * @param list Source list
     * @param page 1-based page number
     * @param pageSize Number of items per page
     * @return Sublist containing paginated items, or empty list if out of bounds
     */
    public static <T> List<T> paginate(List<T> list, int page, int pageSize) {
        if (list == null || list.isEmpty() || page < 1 || pageSize < 1) {
            return Collections.emptyList();
        }
        int fromIndex = (page - 1) * pageSize;
        if (fromIndex >= list.size()) {
            return Collections.emptyList();
        }
        int toIndex = Math.min(fromIndex + pageSize, list.size());
        return list.subList(fromIndex, toIndex);
    }
}
