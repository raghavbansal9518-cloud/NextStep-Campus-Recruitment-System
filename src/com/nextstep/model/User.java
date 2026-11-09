package com.nextstep.model;

import java.time.LocalDateTime;

/**
 * User
 * 
 * Domain model representing a system user for authentication and authorization.
 * Encapsulates credentials, roles, and account metadata.
 * 
 * Security rule: Never expose passwordHash in toString() or logs.
 */
public class User {

    private int id;
    private String username;
    private String passwordHash;
    private Role role;
    private String email;
    private LocalDateTime createdAt;

    /**
     * Default constructor.
     */
    public User() {
    }

    /**
     * Constructor for creating a new user before database insertion.
     */
    public User(String username, String passwordHash, Role role, String email) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.email = email;
    }

    /**
     * Full constructor for retrieving users from the database.
     */
    public User(int id, String username, String passwordHash, Role role, String email, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.email = email;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Safe string representation.
     * Note: passwordHash is explicitly redacted to protect credentials.
     */
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", role=" + role +
                ", email='" + email + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
