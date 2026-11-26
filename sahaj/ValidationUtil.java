package com.nextstep.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValidationUtil {

    public static boolean isNullOrBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isValidEmail(String email) {
        if (isNullOrBlank(email)) {
            return false;
        }
        String trimmed = email.trim();
        int atIndex = trimmed.indexOf('@');
        int dotIndex = trimmed.lastIndexOf('.');
        return atIndex > 0 && dotIndex > atIndex + 1 && dotIndex < trimmed.length() - 1;
    }

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

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    public static boolean isValidPhone(String phone) {
        if (isNullOrBlank(phone)) {
            return false;
        }
        String trimmed = phone.trim();
        if (trimmed.length() != 10) {
            return false;
        }
        for (int i = 0; i < 10; i++) {
            if (!Character.isDigit(trimmed.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isValidCgpa(double cgpa) {
        return cgpa >= 0.0 && cgpa <= 10.0;
    }

    public static boolean isValidBacklogs(int backlogs) {
        return backlogs >= 0;
    }

    public static boolean isValidGraduationYear(int year) {
        return year >= 2020 && year <= 2035;
    }

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
