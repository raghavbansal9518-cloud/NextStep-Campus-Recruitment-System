package com.nextstep.model;

/**
 * Company
 * 
 * Domain model representing an employer or recruiting organization.
 */
public class Company {

    private int id;
    private String name;
    private String industry;
    private String email;
    private String location;
    private String website;

    public Company() {
    }

    public Company(String name, String industry, String email, String location, String website) {
        this.name = name;
        this.industry = industry;
        this.email = email;
        this.location = location;
        this.website = website;
    }

    public Company(int id, String name, String industry, String email, String location, String website) {
        this.id = id;
        this.name = name;
        this.industry = industry;
        this.email = email;
        this.location = location;
        this.website = website;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    @Override
    public String toString() {
        return "Company{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", industry='" + industry + '\'' +
                ", email='" + email + '\'' +
                ", location='" + location + '\'' +
                ", website='" + website + '\'' +
                '}';
    }
}
