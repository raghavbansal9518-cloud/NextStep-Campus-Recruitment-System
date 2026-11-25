package com.nextstep.model;

/**
 * ApplicationStatus
 * 
 * Represents the recruitment progression stages of an application.
 * Follows: APPLIED -> SHORTLISTED -> SELECTED / REJECTED.
 */
public enum ApplicationStatus {
    APPLIED,
    SHORTLISTED,
    SELECTED,
    REJECTED;

    /**
     * Converts a string to ApplicationStatus enum.
     * 
     * @param statusStr String representation of status
     * @return ApplicationStatus enum constant
     */
    public static ApplicationStatus fromString(String statusStr) {
        if (statusStr == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        return ApplicationStatus.valueOf(statusStr.trim().toUpperCase());
    }
}
