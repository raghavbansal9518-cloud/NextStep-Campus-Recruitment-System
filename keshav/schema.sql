-- =====================================================================
-- NextStep — Campus Recruitment System
-- Database Schema Definition (Phase 1)
-- Database Name: nextstep_db
-- =====================================================================

-- 1. Create database if it does not exist
CREATE DATABASE IF NOT EXISTS nextstep_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE nextstep_db;

-- =====================================================================
-- TABLE 1: users
-- Purpose: Central authentication table for all system users.
-- Role-based separation for STUDENT and PLACEMENT_OFFICER.
-- =====================================================================
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('STUDENT', 'PLACEMENT_OFFICER') NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_users_username (username),
    INDEX idx_users_email (email)
) ENGINE=InnoDB;

-- =====================================================================
-- TABLE 2: students
-- Purpose: Academic and personal placement profile for each student.
-- Enforces 1-to-1 relationship with users via unique user_id.
-- =====================================================================
CREATE TABLE IF NOT EXISTS students (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    student_id VARCHAR(30) NOT NULL UNIQUE, -- University Roll No / USN (e.g. 24BCE1001)
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(15) NOT NULL,
    branch VARCHAR(50) NOT NULL,            -- e.g. Computer Science and Engineering
    cgpa DECIMAL(3, 2) NOT NULL,            -- Range 0.00 to 10.00
    active_backlogs INT NOT NULL DEFAULT 0,
    graduation_year INT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_students_student_id (student_id),
    INDEX idx_students_branch (branch),
    INDEX idx_students_cgpa (cgpa)
) ENGINE=InnoDB;

-- =====================================================================
-- TABLE 3: student_skills
-- Purpose: Normalized 1-to-many relationship storing individual skills
-- acquired by students (e.g. Java, Python, SQL) without comma-separated strings.
-- =====================================================================
CREATE TABLE IF NOT EXISTS student_skills (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    skill_name VARCHAR(50) NOT NULL,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT uq_student_skill UNIQUE (student_id, skill_name),
    INDEX idx_student_skills_skill (skill_name)
) ENGINE=InnoDB;

-- =====================================================================
-- TABLE 4: companies
-- Purpose: Registered hiring partners and recruiters.
-- =====================================================================
CREATE TABLE IF NOT EXISTS companies (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    industry VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    location VARCHAR(100) NOT NULL,
    website VARCHAR(100) NULL,
    INDEX idx_companies_name (name)
) ENGINE=InnoDB;

-- =====================================================================
-- TABLE 5: jobs
-- Purpose: Job openings posted by recruiters/placement officers.
-- Contains baseline eligibility criteria (CGPA, backlogs, year).
-- Notice: ON DELETE RESTRICT prevents deleting companies with active job history.
-- =====================================================================
CREATE TABLE IF NOT EXISTS jobs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    company_id INT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    ctc_lpa DECIMAL(10, 2) NOT NULL,        -- CTC in Lakhs Per Annum (e.g. 12.50)
    location VARCHAR(100) NOT NULL,
    min_cgpa DECIMAL(3, 2) NOT NULL DEFAULT 0.00,
    max_backlogs INT NOT NULL DEFAULT 0,
    graduation_year INT NOT NULL,
    status ENUM('OPEN', 'CLOSED') NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE RESTRICT,
    INDEX idx_jobs_company (company_id),
    INDEX idx_jobs_status (status)
) ENGINE=InnoDB;

-- =====================================================================
-- TABLE 6: job_eligible_branches
-- Purpose: Normalized table storing branches eligible for a specific job
-- (e.g., CSE, IT, ECE) avoiding comma-separated columns.
-- =====================================================================
CREATE TABLE IF NOT EXISTS job_eligible_branches (
    id INT AUTO_INCREMENT PRIMARY KEY,
    job_id INT NOT NULL,
    branch_name VARCHAR(50) NOT NULL,
    FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,
    CONSTRAINT uq_job_branch UNIQUE (job_id, branch_name),
    INDEX idx_job_branches_branch (branch_name)
) ENGINE=InnoDB;

-- =====================================================================
-- TABLE 7: job_required_skills
-- Purpose: Normalized table storing technical/domain skills required for a job
-- for automated skill-matching calculation.
-- =====================================================================
CREATE TABLE IF NOT EXISTS job_required_skills (
    id INT AUTO_INCREMENT PRIMARY KEY,
    job_id INT NOT NULL,
    skill_name VARCHAR(50) NOT NULL,
    FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,
    CONSTRAINT uq_job_skill UNIQUE (job_id, skill_name),
    INDEX idx_job_skills_skill (skill_name)
) ENGINE=InnoDB;

-- =====================================================================
-- TABLE 8: applications
-- Purpose: Records student job applications and recruitment lifecycle.
-- Critical:
-- 1. CONSTRAINT uq_student_job prevents duplicate applications at DB level.
-- 2. ON DELETE RESTRICT protects audit trail so completed applications
--    are not accidentally wiped if a student or job record is touched.
-- =====================================================================
CREATE TABLE IF NOT EXISTS applications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    job_id INT NOT NULL,
    status ENUM('APPLIED', 'SHORTLISTED', 'SELECTED', 'REJECTED') NOT NULL DEFAULT 'APPLIED',
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE RESTRICT,
    FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE RESTRICT,
    CONSTRAINT uq_student_job UNIQUE (student_id, job_id),
    INDEX idx_applications_student (student_id),
    INDEX idx_applications_job (job_id),
    INDEX idx_applications_status (status)
) ENGINE=InnoDB;
