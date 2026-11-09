package com.nextstep.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Job
 * 
 * Domain model representing a campus recruitment job posting.
 * Contains eligibility criteria and required skill sets.
 */
public class Job {

    private int id;
    private int companyId;
    private String companyName; // Display helper
    private String title;
    private String description;
    private BigDecimal ctcLpa;  // Monetary compensation in Lakhs Per Annum
    private String location;
    private double minCgpa;
    private int maxBacklogs;
    private int graduationYear;
    private String status;      // OPEN or CLOSED
    private LocalDateTime createdAt;
    private List<String> eligibleBranches;
    private List<String> requiredSkills;

    /**
     * Default constructor initializing collections.
     */
    public Job() {
        this.status = "OPEN";
        this.eligibleBranches = new ArrayList<>();
        this.requiredSkills = new ArrayList<>();
    }

    /**
     * Constructor for creating a job opening prior to database insertion.
     */
    public Job(int companyId, String title, String description, BigDecimal ctcLpa,
               String location, double minCgpa, int maxBacklogs, int graduationYear) {
        this();
        this.companyId = companyId;
        this.title = title;
        this.description = description;
        this.ctcLpa = ctcLpa;
        this.location = location;
        this.minCgpa = minCgpa;
        this.maxBacklogs = maxBacklogs;
        this.graduationYear = graduationYear;
    }

    /**
     * Full constructor for retrieving job records from the database.
     */
    public Job(int id, int companyId, String title, String description, BigDecimal ctcLpa,
               String location, double minCgpa, int maxBacklogs, int graduationYear,
               String status, LocalDateTime createdAt, List<String> eligibleBranches, List<String> requiredSkills) {
        this.id = id;
        this.companyId = companyId;
        this.title = title;
        this.description = description;
        this.ctcLpa = ctcLpa;
        this.location = location;
        this.minCgpa = minCgpa;
        this.maxBacklogs = maxBacklogs;
        this.graduationYear = graduationYear;
        this.status = status;
        this.createdAt = createdAt;
        this.eligibleBranches = (eligibleBranches != null) ? new ArrayList<>(eligibleBranches) : new ArrayList<>();
        this.requiredSkills = (requiredSkills != null) ? new ArrayList<>(requiredSkills) : new ArrayList<>();
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCompanyId() {
        return companyId;
    }

    public void setCompanyId(int companyId) {
        this.companyId = companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getCtcLpa() {
        return ctcLpa;
    }

    public void setCtcLpa(BigDecimal ctcLpa) {
        this.ctcLpa = ctcLpa;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public double getMinCgpa() {
        return minCgpa;
    }

    public void setMinCgpa(double minCgpa) {
        this.minCgpa = minCgpa;
    }

    public int getMaxBacklogs() {
        return maxBacklogs;
    }

    public void setMaxBacklogs(int maxBacklogs) {
        this.maxBacklogs = maxBacklogs;
    }

    public int getGraduationYear() {
        return graduationYear;
    }

    public void setGraduationYear(int graduationYear) {
        this.graduationYear = graduationYear;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<String> getEligibleBranches() {
        return Collections.unmodifiableList(eligibleBranches);
    }

    public void setEligibleBranches(List<String> eligibleBranches) {
        this.eligibleBranches = (eligibleBranches != null) ? new ArrayList<>(eligibleBranches) : new ArrayList<>();
    }

    public void addEligibleBranch(String branch) {
        if (branch != null && !branch.trim().isEmpty() && !this.eligibleBranches.contains(branch.trim())) {
            this.eligibleBranches.add(branch.trim());
        }
    }

    public List<String> getRequiredSkills() {
        return Collections.unmodifiableList(requiredSkills);
    }

    public void setRequiredSkills(List<String> requiredSkills) {
        this.requiredSkills = (requiredSkills != null) ? new ArrayList<>(requiredSkills) : new ArrayList<>();
    }

    public void addRequiredSkill(String skill) {
        if (skill != null && !skill.trim().isEmpty() && !this.requiredSkills.contains(skill.trim())) {
            this.requiredSkills.add(skill.trim());
        }
    }

    @Override
    public String toString() {
        return "Job{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", ctcLpa=" + ctcLpa + " LPA" +
                ", location='" + location + '\'' +
                ", minCgpa=" + minCgpa +
                ", maxBacklogs=" + maxBacklogs +
                ", graduationYear=" + graduationYear +
                ", status='" + status + '\'' +
                ", eligibleBranches=" + eligibleBranches +
                ", requiredSkills=" + requiredSkills +
                '}';
    }
}
