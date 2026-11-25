package com.nextstep.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Student
 * 
 * Domain model representing a student's academic and recruitment profile.
 * Linked to a User account through userId.
 */
public class Student {

    private int id;
    private int userId;
    private String studentId; // University Roll Number / USN (e.g. 24BCE1001)
    private String fullName;
    private String phone;
    private String branch;
    private double cgpa;
    private int activeBacklogs;
    private int graduationYear;
    private List<String> skills;

    /**
     * Default constructor initializing collections.
     */
    public Student() {
        this.skills = new ArrayList<>();
    }

    /**
     * Constructor for creating a student profile before database insertion.
     */
    public Student(int userId, String studentId, String fullName, String phone,
                   String branch, double cgpa, int activeBacklogs, int graduationYear) {
        this();
        this.userId = userId;
        this.studentId = studentId;
        this.fullName = fullName;
        this.phone = phone;
        this.branch = branch;
        this.cgpa = cgpa;
        this.activeBacklogs = activeBacklogs;
        this.graduationYear = graduationYear;
    }

    /**
     * Full constructor for retrieving student data from the database.
     */
    public Student(int id, int userId, String studentId, String fullName, String phone,
                   String branch, double cgpa, int activeBacklogs, int graduationYear, List<String> skills) {
        this.id = id;
        this.userId = userId;
        this.studentId = studentId;
        this.fullName = fullName;
        this.phone = phone;
        this.branch = branch;
        this.cgpa = cgpa;
        this.activeBacklogs = activeBacklogs;
        this.graduationYear = graduationYear;
        this.skills = (skills != null) ? new ArrayList<>(skills) : new ArrayList<>();
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public double getCgpa() {
        return cgpa;
    }

    public void setCgpa(double cgpa) {
        this.cgpa = cgpa;
    }

    public int getActiveBacklogs() {
        return activeBacklogs;
    }

    public void setActiveBacklogs(int activeBacklogs) {
        this.activeBacklogs = activeBacklogs;
    }

    public int getGraduationYear() {
        return graduationYear;
    }

    public void setGraduationYear(int graduationYear) {
        this.graduationYear = graduationYear;
    }

    public List<String> getSkills() {
        return Collections.unmodifiableList(skills);
    }

    public void setSkills(List<String> skills) {
        this.skills = (skills != null) ? new ArrayList<>(skills) : new ArrayList<>();
    }

    public void addSkill(String skill) {
        if (skill != null && !skill.trim().isEmpty() && !this.skills.contains(skill.trim())) {
            this.skills.add(skill.trim());
        }
    }

    @Override
    public String toString() {
        return "Student{" +
                "id=" + id +
                ", studentId='" + studentId + '\'' +
                ", fullName='" + fullName + '\'' +
                ", branch='" + branch + '\'' +
                ", cgpa=" + cgpa +
                ", activeBacklogs=" + activeBacklogs +
                ", graduationYear=" + graduationYear +
                ", skills=" + skills +
                '}';
    }
}
