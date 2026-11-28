# NextStep — Project Overview, Problem Statement & Objectives

---

## 1. Project Title & Domain

- **Official Project Title:** NextStep — Campus Recruitment System (Automated Eligibility, Skill Matching & Placement Management)
- **Academic Context:** Semester 3 Continuous Assessment 3 (CA-3) Project for Course **Programming in Java (25CSE0202)**
- **Domain:** Enterprise Campus Placements / Human Resource Technology (HRTech) & Educational Administration
- **Core Technology:** Pure Java SE (Standard Edition 17/21 LTS), Java Database Connectivity (JDBC 4.2), and MySQL Server 8.0+

---

## 2. Project Overview

**NextStep** is an enterprise-grade, console-based campus placement automation system designed to eliminate manual inefficiencies in college placement drives. Built strictly without external web frameworks or heavyweight ORM layers, it showcases core Java engineering, relational database normalization, cryptographic security, and syllabus concepts such as the Stream API, Lambda expressions, Java Generics, and RFC-4180 CSV File I/O.

The platform provides a dual-portal console environment:
1. **Student Placement Hub:** Students register their academic credentials and acquired technical skill sets, inspect company openings, evaluate their eligibility and skill match percentage in real-time against corporate criteria, submit digital applications, and track application lifecycle statuses (`APPLIED` $\rightarrow$ `SHORTLISTED` $\rightarrow$ `SELECTED` / `REJECTED`).
2. **Placement Officer Administration Dashboard:** Training & Placement Officers (TPOs) register with institution-level activation codes, publish new hiring drives with granular academic and technical cutoffs, audit candidate applications campus-wide, update candidate statuses, and export accreditation-ready placement ledgers to CSV files.
3. **Centralized Demo Data Manager:** A dedicated demonstration control subsystem that allows immediate, repeatable evaluation of 3 benchmark student profiles (Scenario A: Eligible, Scenario B: Skill-Gap, Scenario C: Ineligible) using quick console shortcuts (`A`, `A1`, `A2`, `A3`).

---

## 3. Background & Real-World Problem Statement

### The College Placement Reality
In universities and engineering institutions, campus recruitment season involves coordinating hundreds of student applicants across dozens of visiting companies (such as cloud vendors, fintech firms, and software consultancies), each imposing distinct eligibility cutoffs:
- Minimum cumulative grade point average (CGPA) thresholds (e.g., $\ge 7.50$).
- Strict active backlog limits (e.g., maximum 0 or 1 active backlog).
- Prescribed graduation batches (e.g., Class of 2026).
- Permitted degree disciplines/branches (e.g., Computer Science, Information Technology, Electronics).
- Mandatory technical skill proficiencies (e.g., Java, SQL, OOP, Git).

### Limitations of Existing Systems
In many collegiate environments, placement management is executed through disjointed spreadsheets (Excel/Google Sheets), manual paper forms, and bulletin boards. This legacy approach suffers from severe operational vulnerabilities:

1. **Human Error in Academic Auditing:** Manually cross-referencing candidate transcripts against changing company cutoffs is error-prone. Ineligible students slip through, wasting recruiter interview slots.
2. **Subjectivity & Blind Applications:** Students apply blindly to job titles without understanding their skill gaps, leading to high rejection rates and student frustration.
3. **Duplicate & Conflicting Submissions:** Spreadsheets cannot enforce atomic uniqueness. Students often submit multiple conflicting entries for the same drive.
4. **Security & Data Privacy Risks:** Storing student passwords and contact details in shared spreadsheets exposes sensitive personal data to unauthorized access.
5. **No Auditable Candidate Lifecycle:** Placement coordinators struggle to maintain an immutable audit trail showing when an application was filed, when status changed to `SHORTLISTED`, and how final statistics were derived.
6. **Inefficient Reporting:** Compiling placement statistics and accreditation metrics (such as NBA or NAAC placement audit reports) requires days of manual data sorting.

---

## 4. The Proposed Solution: NextStep

NextStep replaces manual spreadsheets with a **centralized, terminal-driven relational platform**:

- **Relational Data Integrity:** Backed by **MySQL 8.0**, using an 8-table third-normal-form (3NF) relational schema with foreign key cascades, unique indexes, and ACID transaction rollbacks.
- **Dual-Phase Automated Eligibility Engine:** Combines deterministic boolean checks for academic criteria (CGPA, backlogs, branch, graduation year) with set-theoretic intersection algorithms for technical skills.
- **Set-Theoretic Skill Gap Analysis:** Computes exact percentage match ($\frac{|\text{Student Skills} \cap \text{Job Skills}|}{|\text{Job Skills}|} \times 100$) and explicitly identifies missing skills.
- **Zero Duplicate Applications:** A composite database constraint `UNIQUE (student_id, job_id)` makes duplicate submissions physically impossible at the database level.
- **Cryptographic Security:** Salted **PBKDF2WithHmacSHA256** password derivation (10,000 iterations, 16-byte cryptographically secure random salt) guarantees credentials can never be compromised via rainbow table attacks.
- **Role-Based Access Control (RBAC):** Cryptographically enforces permissions between `STUDENT` and `PLACEMENT_OFFICER` roles.
- **Accreditation-Ready CSV Export:** Generates RFC-4180 compliant CSV audit ledgers with automated formula escaping and quotation handling.

---

## 5. Objectives of the Project

### Primary Objectives
1. **Automate Placement Auditing:** Eliminate human error by calculating academic eligibility automatically via server-side validation rules.
2. **Empower Candidates with Transparent Skill Matching:** Provide candidates with an instant breakdown of matching vs. missing skills before applying.
3. **Enforce Database-Level Consistency:** Utilize MySQL foreign keys, unique constraints, and JDBC transaction rollbacks to guarantee data integrity.
4. **Demonstrate Java SE & JDBC Mastery:** Implement key course syllabus concepts (OOP, JDBC 4.2, Stream API, Lambda expressions, Java Generics, Exception Handling, File I/O) in a production-style architecture.

### Secondary Objectives
1. **Responsive Console UI:** Provide an aesthetically pleasing 24-bit TrueColour console experience with dynamic horizontal centering (auto-margin) across varying terminal widths (80, 100, 120 columns).
2. **Deterministic Demonstration Mode:** Enable smooth viva demonstrations with zero repetitive typing through a Centralized Demo Data Manager.
3. **Zero External Framework Overhead:** Build the entire system using Java Standard Library tools, proving deep mastery of core language mechanics.

---

## 6. Project Scope & Target Users

### Target Users
- **College Students (Undergraduate / Postgraduate Candidates):** Users seeking placement drives, eligibility evaluations, and digital application tracking.
- **Training and Placement Officers (TPOs / System Administrators):** Institutional staff responsible for drive scheduling, student vetting, and ledger exports.
- **Academic Evaluators / Viva Professors:** Faculty inspecting code quality, architectural separation of concerns, and relational database mechanics.

### In Scope
- Interactive console menus with 64-column framed layout and ANSI palette.
- Student account registration, profile management, and credential authentication.
- Officer account onboarding with activation code verification.
- Job drive creation with company selection, package specification (CTC LPA), branch lists, and skill requirements.
- Automated eligibility checking (CGPA, backlogs, graduation year, degree branch).
- Automated technical skill matrix analysis (percentage match, matched skills, missing skills).
- Single-click application submission with instant receipt generation.
- Placement officer application review and status transitions (`APPLIED` $\rightarrow$ `SHORTLISTED` $\rightarrow$ `REJECTED`).
- RFC-4180 CSV export of application ledgers with comma and quote escaping.
- Comprehensive automated regression testing (297 test cases).

### Out of Scope (Current Release)
- Web browser client (HTML/CSS/JavaScript).
- Graphical User Interface (JavaFX/Swing).
- Networked remote client-server sockets.
- Direct integration with commercial third-party job boards (LinkedIn, Indeed).

---

## 7. Advantages and Limitations

### Key Advantages
1. **Lightweight & Portable:** Zero heavy framework dependencies; compiles and executes in seconds using standard JDK tools.
2. **Data Consistency:** Guaranteed by MySQL InnoDB storage engine with strict referential integrity.
3. **Transparent Evaluation:** Students know exactly why they passed or failed eligibility.
4. **Security:** Industry-standard PBKDF2 password encryption and SQL injection immunity via PreparedStatements.
5. **High Test Coverage:** 100% pass rate across 297 automated tests covering database edge cases, input validation, and visual layout.

### Current Limitations
1. **Console Interface:** Requires a command-line terminal; does not support mobile or web browsers.
2. **Single Campus Instance:** Designed for single-university deployment rather than multi-tenant university consortiums.
3. **Local Database:** Requires a reachable MySQL instance configured in `config/db.properties`.
