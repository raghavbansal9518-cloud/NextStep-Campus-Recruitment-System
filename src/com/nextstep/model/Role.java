package com.nextstep.model;

/**
 * Role
 * 
 * Defines the user roles within the NextStep recruitment system.
 * Prevents invalid role assignments using type-safe enums.
 */
public enum Role {
    STUDENT,
    PLACEMENT_OFFICER;

    /**
     * Converts a database or string value to its corresponding Role enum.
     * Case-insensitive matching.
     * 
     * @param roleStr String value of role
     * @return Role enum constant
     * @throws IllegalArgumentException if role name is invalid
     */
    public static Role fromString(String roleStr) {
        if (roleStr == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
        return Role.valueOf(roleStr.trim().toUpperCase());
    }
}
