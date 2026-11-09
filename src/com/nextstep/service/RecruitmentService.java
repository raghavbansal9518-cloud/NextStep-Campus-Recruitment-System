package com.nextstep.service;

import com.nextstep.dao.ApplicationDAO;
import com.nextstep.dao.JobDAO;
import com.nextstep.model.Application;
import com.nextstep.model.ApplicationStatus;
import com.nextstep.model.Job;
import com.nextstep.model.Role;
import com.nextstep.model.Student;
import com.nextstep.model.User;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RecruitmentService
 * 
 * Handles recruitment operations, eligibility verification, application lifecycle,
 * and reporting.
 * 
 * College Viva Concepts:
 * - Business Layer: Decouples eligibility logic from data access and UI.
 * - Encapsulation: EligibilityResult encapsulates the decision and diagnostic reasons.
 * - Defensive Programming: Validates job status and eligibility prior to database write.
 * - Lambda Expressions: Sorts collections using concise comparator lambdas.
 * - Java Streams: Demonstrates filter, map, distinct, sorted, count, and anyMatch.
 * - Collectors: Demonstrates groupingBy, partitioningBy, and counting.
 * - File I/O: CSV export, parsing into ArrayList, validation, and updating records using try-with-resources.
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

        // Flag as ineligible if student is missing required technical skills
        if (!missingSkills.isEmpty()) {
            isEligible = false;
            reasons.add(String.format("Missing required skills: %s", missingSkills));
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
     * Updates an application status (e.g. SHORTLISTED, SELECTED, REJECTED).
     * Restricted to PLACEMENT_OFFICER role. Normal students are blocked from modifying statuses.
     * 
     * @param actor The logged-in user attempting the status change
     * @param applicationId ID of the target application
     * @param newStatus New recruitment lifecycle status
     * @return boolean true if update succeeded, false if application was not found
     * @throws SecurityException if actor is null or does not have PLACEMENT_OFFICER role
     * @throws IllegalArgumentException if newStatus is null
     * @throws SQLException on database errors
     */
    public boolean updateApplicationStatus(User actor, int applicationId, ApplicationStatus newStatus) throws SQLException {
        if (actor == null || actor.getRole() != Role.PLACEMENT_OFFICER) {
            throw new SecurityException("Access denied: Only placement officers can update application statuses.");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        return applicationDAO.updateStatus(applicationId, newStatus);
    }

    /**
     * Retrieves all applications submitted in the system.
     * Restricted to PLACEMENT_OFFICER role.
     * 
     * @param actor The logged-in placement officer
     * @return List of all applications with student, job, and company details
     * @throws SecurityException if actor is not a PLACEMENT_OFFICER
     * @throws SQLException on database errors
     */
    public List<Application> getAllApplications(User actor) throws SQLException {
        if (actor == null || actor.getRole() != Role.PLACEMENT_OFFICER) {
            throw new SecurityException("Access denied: Only placement officers can view all applications.");
        }
        return applicationDAO.findAllApplications();
    }

    // =========================================================================
    // SEMESTER 3 JAVA CONCEPTS: STREAMS, LAMBDAS & COLLECTORS
    // =========================================================================

    /**
     * Filters and returns only currently OPEN jobs.
     * Demonstrates Java Streams (filter, collect).
     * 
     * @return List of open Job entities
     * @throws SQLException on database errors
     */
    public List<Job> getOpenJobs() throws SQLException {
        return jobDAO.findAllJobs().stream()
                     .filter(j -> "OPEN".equalsIgnoreCase(j.getStatus()))
                     .collect(Collectors.toList());
    }

    /**
     * Returns open jobs sorted by CTC in descending order (highest package first).
     * Demonstrates Lambda Expression in Collections.sort().
     * 
     * @return List of jobs sorted by compensation
     * @throws SQLException on database errors
     */
    public List<Job> getJobsSortedByCtc() throws SQLException {
        List<Job> jobs = new ArrayList<>(getOpenJobs());
        // Lambda expression comparing package CTC (BigDecimal)
        jobs.sort((j1, j2) -> {
            if (j1.getCtcLpa() == null) return 1;
            if (j2.getCtcLpa() == null) return -1;
            return j2.getCtcLpa().compareTo(j1.getCtcLpa());
        });
        return jobs;
    }

    /**
     * Retrieves unique hiring company names with open positions, sorted alphabetically.
     * Demonstrates Stream pipeline: filter -> map -> distinct -> sorted -> collect.
     * 
     * @return Sorted list of distinct company names
     * @throws SQLException on database errors
     */
    public List<String> getDistinctHiringCompanies() throws SQLException {
        return jobDAO.findAllJobs().stream()
                     .filter(j -> "OPEN".equalsIgnoreCase(j.getStatus()))
                     .map(Job::getCompanyName)
                     .distinct()
                     .sorted()
                     .collect(Collectors.toList());
    }

    /**
     * Counts the total number of currently active recruitment drives.
     * Demonstrates Stream terminal operation count().
     */
    public long countOpenJobs() throws SQLException {
        return jobDAO.findAllJobs().stream()
                     .filter(j -> "OPEN".equalsIgnoreCase(j.getStatus()))
                     .count();
    }

    /**
     * Checks if any active drive is available in a given city/location.
     * Demonstrates Stream terminal operation anyMatch().
     */
    public boolean hasOpeningInLocation(String location) throws SQLException {
        if (location == null || location.trim().isEmpty()) {
            return false;
        }
        return jobDAO.findAllJobs().stream()
                     .filter(j -> "OPEN".equalsIgnoreCase(j.getStatus()))
                     .anyMatch(j -> j.getLocation() != null &&
                                    j.getLocation().toLowerCase().contains(location.toLowerCase()));
    }

    /**
     * Generates a recruitment summary mapping each ApplicationStatus to its count.
     * Demonstrates Collectors.groupingBy() and Collectors.counting().
     * 
     * @param actor The logged-in placement officer
     * @return Map of ApplicationStatus to count of applications
     * @throws SecurityException if actor is not a PLACEMENT_OFFICER
     * @throws SQLException on database errors
     */
    public Map<ApplicationStatus, Long> getRecruitmentSummary(User actor) throws SQLException {
        List<Application> apps = getAllApplications(actor);
        return apps.stream()
                   .collect(Collectors.groupingBy(Application::getStatus, Collectors.counting()));
    }

    /**
     * Partitions applications into Placed (SELECTED) vs Other (in-progress/pending).
     * Demonstrates Collectors.partitioningBy() and Collectors.counting().
     * 
     * @param actor The logged-in placement officer
     * @return Map where Boolean.TRUE = placed applications count, FALSE = remaining count
     * @throws SecurityException if actor is not a PLACEMENT_OFFICER
     * @throws SQLException on database errors
     */
    public Map<Boolean, Long> getPlacementPartitionCount(User actor) throws SQLException {
        List<Application> apps = getAllApplications(actor);
        return apps.stream()
                   .collect(Collectors.partitioningBy(
                           app -> app.getStatus() == ApplicationStatus.SELECTED,
                           Collectors.counting()
                   ));
    }

    // =========================================================================
    // SEMESTER 3 JAVA CONCEPTS: FILE I/O & CSV PROCESSING
    // =========================================================================

    /**
     * Exports all recruitment applications to a CSV report file.
     * Restricted to PLACEMENT_OFFICER role.
     * Demonstrates Java File I/O (BufferedWriter, FileWriter, try-with-resources).
     * 
     * @param actor The logged-in placement officer
     * @param destinationPath Path to destination CSV file (defaults to reports/applications_report.csv if null)
     * @return File object pointing to the created CSV report
     * @throws SecurityException if actor is not a PLACEMENT_OFFICER
     * @throws IOException on file write errors
     * @throws SQLException on database errors
     */
    public File exportApplicationsToCsv(User actor, String destinationPath) throws IOException, SQLException {
        if (actor == null || actor.getRole() != Role.PLACEMENT_OFFICER) {
            throw new SecurityException("Access denied: Only placement officers can export reports.");
        }

        List<Application> applications = getAllApplications(actor);

        File outputFile = (destinationPath != null && !destinationPath.trim().isEmpty())
                ? new File(destinationPath)
                : new File("reports/applications_report.csv");

        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            // Write CSV Header
            writer.write("Application ID,Student Roll No,Student Name,Job Title,Company,Status,Applied At");
            writer.newLine();

            // Write Data Rows
            for (Application app : applications) {
                String rollNo = (app.getStudentRollNumber() != null) ? app.getStudentRollNumber() : String.valueOf(app.getStudentId());
                String name = (app.getStudentName() != null) ? app.getStudentName().replace(",", " ") : "N/A";
                String title = (app.getJobTitle() != null) ? app.getJobTitle().replace(",", " ") : "N/A";
                String company = (app.getCompanyName() != null) ? app.getCompanyName().replace(",", " ") : "N/A";
                String status = (app.getStatus() != null) ? app.getStatus().name() : "N/A";
                String appliedAt = (app.getAppliedAt() != null) ? app.getAppliedAt().toString() : "N/A";

                writer.write(String.format("%d,%s,%s,%s,%s,%s,%s",
                        app.getId(), rollNo, name, title, company, status, appliedAt));
                writer.newLine();
            }
        }

        return outputFile;
    }

    /**
     * Reads an exported CSV report, performs basic format validation, parses valid rows
     * into an ArrayList of Application objects, and writes updated/audited records to another file.
     * 
     * Demonstrates:
     * - File I/O: BufferedReader and BufferedWriter with try-with-resources.
     * - String parsing and basic validation.
     * - Collections: Populating an ArrayList<Application>.
     * - Writing modified records to a second file.
     * 
     * @param actor The logged-in placement officer
     * @param sourceCsvPath Path to source CSV file
     * @param targetCsvPath Path to destination updated CSV file
     * @return List<Application> Parsed and validated applications
     * @throws SecurityException if actor is not a PLACEMENT_OFFICER
     * @throws IOException on file I/O errors
     * @throws IllegalArgumentException on CSV validation failures
     */
    public List<Application> processAndArchiveCsvReport(User actor, String sourceCsvPath, String targetCsvPath)
            throws IOException {
        if (actor == null || actor.getRole() != Role.PLACEMENT_OFFICER) {
            throw new SecurityException("Access denied: Only placement officers can process CSV reports.");
        }

        File inputFile = new File(sourceCsvPath);
        if (!inputFile.exists()) {
            throw new IllegalArgumentException("Source CSV file does not exist: " + sourceCsvPath);
        }

        List<Application> parsedList = new ArrayList<>();

        // 1. Read and validate CSV using try-with-resources
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String header = reader.readLine();
            if (header == null || !header.contains("Application ID")) {
                throw new IllegalArgumentException("Invalid CSV header. Expected 'Application ID' column.");
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue; // Skip blank lines
                }

                String[] tokens = trimmed.split(",", -1);
                if (tokens.length < 6) {
                    System.err.println("[WARN] Skipping malformed line " + lineNumber + ": " + line);
                    continue;
                }

                try {
                    int appId = Integer.parseInt(tokens[0].trim());
                    String rollNo = tokens[1].trim();
                    String studentName = tokens[2].trim();
                    String jobTitle = tokens[3].trim();
                    String companyName = tokens[4].trim();
                    ApplicationStatus status = ApplicationStatus.fromString(tokens[5].trim());

                    Application app = new Application();
                    app.setId(appId);
                    app.setStudentRollNumber(rollNo);
                    app.setStudentName(studentName);
                    app.setJobTitle(jobTitle);
                    app.setCompanyName(companyName);
                    app.setStatus(status);

                    parsedList.add(app);
                } catch (Exception e) {
                    System.err.println("[WARN] Skipping invalid data row at line " + lineNumber + ": " + e.getMessage());
                }
            }
        }

        // 2. Write updated records with audit status to another file using try-with-resources
        File outputFile = new File(targetCsvPath);
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write("Application ID,Student Roll No,Student Name,Job Title,Company,Status,Verification Status");
            writer.newLine();

            for (Application app : parsedList) {
                writer.write(String.format("%d,%s,%s,%s,%s,%s,%s",
                        app.getId(),
                        app.getStudentRollNumber(),
                        app.getStudentName(),
                        app.getJobTitle(),
                        app.getCompanyName(),
                        app.getStatus().name(),
                        "AUDITED_VALID"));
                writer.newLine();
            }
        }

        return parsedList;
    }
}
