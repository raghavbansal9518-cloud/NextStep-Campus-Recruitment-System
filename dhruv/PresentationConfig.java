package com.nextstep.util;

/**
 * PresentationConfig
 * 
 * Centralized configuration to temporarily toggle menu options for presentation or viva demos.
 * Decoupled from core business logic so toggling an option does not affect underlying service methods.
 */
public class PresentationConfig {

    // Default: all menu options are visible
    private static boolean studentRegistrationVisible = true;
    private static boolean studentCheckEligibilityVisible = true;
    private static boolean officerRecruitmentSummaryVisible = true;
    private static boolean officerCsvExportVisible = true;

    public static boolean isStudentRegistrationVisible() {
        return studentRegistrationVisible;
    }

    public static void setStudentRegistrationVisible(boolean visible) {
        studentRegistrationVisible = visible;
    }

    public static boolean isStudentCheckEligibilityVisible() {
        return studentCheckEligibilityVisible;
    }

    public static void setStudentCheckEligibilityVisible(boolean visible) {
        studentCheckEligibilityVisible = visible;
    }

    public static boolean isOfficerRecruitmentSummaryVisible() {
        return officerRecruitmentSummaryVisible;
    }

    public static void setOfficerRecruitmentSummaryVisible(boolean visible) {
        officerRecruitmentSummaryVisible = visible;
    }

    public static boolean isOfficerCsvExportVisible() {
        return officerCsvExportVisible;
    }

    public static void setOfficerCsvExportVisible(boolean visible) {
        officerCsvExportVisible = visible;
    }

    /**
     * Resets all visibility options to fully visible (default state).
     */
    public static void resetDefaults() {
        studentRegistrationVisible = true;
        studentCheckEligibilityVisible = true;
        officerRecruitmentSummaryVisible = true;
        officerCsvExportVisible = true;
    }
}
