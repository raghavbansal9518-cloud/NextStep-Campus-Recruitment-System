package com.nextstep.service;

import com.nextstep.dao.ApplicationDAO;
import com.nextstep.dao.JobDAO;
import com.nextstep.model.Application;
import com.nextstep.model.ApplicationStatus;
import com.nextstep.model.Job;
import com.nextstep.model.Student;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RecruitmentService
 * 
 * Handles recruitment operations, eligibility verification, and application lifecycle.
 * 
 * College Viva Concepts:
 * - Business Layer: Decouples eligibility logic from data access and UI.
 * - Encapsulation: EligibilityResult encapsulates the decision and diagnostic reasons.
 * - Defensive Programming: Validates job status and eligibility prior to database write.
 */
public class RecruitmentService {

    private final JobDAO jobDAO;
    private final ApplicationDAO applicationDAO;

    public RecruitmentService() {
        this.jobDAO = new JobDAO();
        this.applicationDAO = new ApplicationDAO();
    }

    public RecruitmentService(JobDAO jobDAO, ApplicationDAO applicationDAO) {
        this.jobDAO = jobDAO;
        this.applicationDAO = applicationDAO;
    }

    /**
     * Inner helper class encapsulating an eligibility evaluation.
     */
    public static class EligibilityResult {
        private final boolean eligible;
        private final List<String> reasons;
        private final List<String> matchedSkills;
        private final List<String> missingSkills;

        public EligibilityResult(boolean eligible, List<String> reasons,
                                 List<String> matchedSkills, List<String> missingSkills) {
            this.eligible = eligible;
            this.reasons = reasons != null ? reasons : new ArrayList<>();
            this.matchedSkills = matchedSkills != null ? matchedSkills : new ArrayList<>();
            this.missingSkills = missingSkills != null ? missingSkills : new ArrayList<>();
        }

        public boolean isEligible() {
            return eligible;
        }

        public List<String> getReasons() {
            return Collections.unmodifiableList(reasons);
        }

        public List<String> getMatchedSkills() {
            return Collections.unmodifiableList(matchedSkills);
        }

        public List<String> getMissingSkills() {
            return Collections.unmodifiableList(missingSkills);
        }

        @Override
        public String toString() {
            if (eligible) {
                return "[ELIGIBLE] Student meets all criteria. Matched Skills: " + matchedSkills;
            } else {
                return "[INELIGIBLE] Reasons: " + String.join("; ", reasons);
            }
        }
    }

    /**
     * Evaluates a student's eligibility for a given job opening.
     * Checks:
     * 1. Minimum CGPA
     * 2. Maximum Allowed Active Backlogs
     * 3. Academic Branch
     * 4. Required Technical Skills match
     */
    public EligibilityResult checkEligibility(Student student, Job job) {
        if (student == null || job == null) {
            throw new IllegalArgumentException("Student and Job cannot be null");
        }

        List<String> reasons = new ArrayList<>();
        boolean isEligible = true;

        // 1. CGPA Criterion
        if (student.getCgpa() < job.getMinCgpa()) {
            isEligible = false;
            reasons.add(String.format("CGPA (%.2f) is below minimum cutoff (%.2f)",
                    student.getCgpa(), job.getMinCgpa()));
        }

        // 2. Backlogs Criterion
        if (student.getActiveBacklogs() > job.getMaxBacklogs()) {
            isEligible = false;
            reasons.add(String.format("Active backlogs (%d) exceed maximum allowed (%d)",
                    student.getActiveBacklogs(), job.getMaxBacklogs()));
        }

        // 3. Branch Criterion (Case-insensitive matching)
        boolean branchEligible = false;
        List<String> eligibleBranches = job.getEligibleBranches();
        if (eligibleBranches == null || eligibleBranches.isEmpty()) {
            branchEligible = true; // Open to all branches if none specified
        } else {
            for (String allowed : eligibleBranches) {
                if (allowed.equalsIgnoreCase(student.getBranch()) ||
                    allowed.toLowerCase().contains(student.getBranch().toLowerCase()) ||
                    student.getBranch().toLowerCase().contains(allowed.toLowerCase())) {
                    branchEligible = true;
                    break;
                }
            }
        }

        if (!branchEligible) {
            isEligible = false;
            reasons.add(String.format("Branch '%s' is not in eligible branches: %s",
                    student.getBranch(), eligibleBranches));
        }

        // 4. Skills Match Comparison
        List<String> matchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();
        List<String> studentSkills = student.getSkills();
        List<String> requiredSkills = job.getRequiredSkills();

        if (requiredSkills != null) {
            for (String required : requiredSkills) {
                boolean found = false;
                if (studentSkills != null) {
                    for (String sSkill : studentSkills) {
                        if (sSkill.equalsIgnoreCase(required.trim())) {
                            found = true;
                            break;
                        }
                    }
                }
                if (found) {
                    matchedSkills.add(required);
                } else {
                    missingSkills.add(required);
                }
            }
        }

        return new EligibilityResult(isEligible, reasons, matchedSkills, missingSkills);
    }

    /**
     * Submits an application for an eligible student to an open job.
     * Prevents duplicate applications and blocks ineligible students.
     * 
     * @param student The applying student
     * @param jobId Target job ID
     * @return Application created application record
     * @throws IllegalStateException on eligibility or duplicate application failures
     * @throws SQLException on database errors
     */
    public Application applyForJob(Student student, int jobId) throws SQLException {
        if (student == null) {
            throw new IllegalArgumentException("Student cannot be null");
        }

        // 1. Verify Job exists and is open
        Job job = jobDAO.findById(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Job with ID " + jobId + " does not exist.");
        }
        if (!"OPEN".equalsIgnoreCase(job.getStatus())) {
            throw new IllegalStateException("Job '" + job.getTitle() + "' is currently closed for applications.");
        }

        // 2. Check for duplicate application
        if (applicationDAO.hasStudentApplied(student.getId(), jobId)) {
            throw new IllegalStateException("You have already applied for this job.");
        }

        // 3. Verify student eligibility
        EligibilityResult eligibility = checkEligibility(student, job);
        if (!eligibility.isEligible()) {
            throw new IllegalStateException("Application blocked: " + String.join(", ", eligibility.getReasons()));
        }

        // 4. Submit application
        Application app = new Application(student.getId(), jobId);
        app.setStatus(ApplicationStatus.APPLIED);
        boolean created = applicationDAO.createApplication(app);
        if (!created) {
            throw new SQLException("Failed to record application in database.");
        }

        app.setJobTitle(job.getTitle());
        app.setCompanyName(job.getCompanyName());
        return app;
    }

    /**
     * Retrieves application history strictly scoped to a single student.
     */
    public List<Application> getStudentApplications(int studentId) throws SQLException {
        return applicationDAO.findByStudentId(studentId);
    }

    /**
     * Updates an application status (used by placement officers/admins).
     */
    public boolean updateApplicationStatus(int applicationId, ApplicationStatus newStatus) throws SQLException {
        return applicationDAO.updateStatus(applicationId, newStatus);
    }
}
