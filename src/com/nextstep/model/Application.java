package com.nextstep.model;

import java.time.LocalDateTime;

/**
 * Application
 * 
 * Domain model representing a job application submitted by a student.
 * Tracks recruitment lifecycle status and submission timestamp.
 */
public class Application {

    private int id;
    private int studentId;
    private int jobId;
    private ApplicationStatus status;
    private LocalDateTime appliedAt;

    // Optional display helper fields for console views
    private String studentName;
    private String studentRollNumber;
    private String jobTitle;
    private String companyName;

    /**
     * Default constructor.
     */
    public Application() {
        this.status = ApplicationStatus.APPLIED;
    }

    /**
     * Constructor for creating a new application prior to database insertion.
     */
    public Application(int studentId, int jobId) {
        this();
        this.studentId = studentId;
        this.jobId = jobId;
    }

    /**
     * Full constructor for retrieving applications from the database.
     */
    public Application(int id, int studentId, int jobId, ApplicationStatus status, LocalDateTime appliedAt) {
        this.id = id;
        this.studentId = studentId;
        this.jobId = jobId;
        this.status = status;
        this.appliedAt = appliedAt;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentRollNumber() {
        return studentRollNumber;
    }

    public void setStudentRollNumber(String studentRollNumber) {
        this.studentRollNumber = studentRollNumber;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    @Override
    public String toString() {
        return "Application{" +
                "id=" + id +
                ", studentId=" + studentId +
                ", jobId=" + jobId +
                ", status=" + status +
                ", appliedAt=" + appliedAt +
                (jobTitle != null ? ", jobTitle='" + jobTitle + '\'' : "") +
                (companyName != null ? ", companyName='" + companyName + '\'' : "") +
                '}';
    }
}
