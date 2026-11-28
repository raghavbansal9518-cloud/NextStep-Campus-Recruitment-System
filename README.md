# NextStep — Campus Recruitment System

[![Java Version](https://img.shields.io/badge/Java-17%20%7C%2021%20LTS-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Database](https://img.shields.io/badge/MySQL-8.0%2B-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![JDBC Driver](https://img.shields.io/badge/MySQL%20Connector%2FJ-8.4.0-blue)](https://dev.mysql.com/downloads/connector/j/)
[![Build Tool](https://img.shields.io/badge/Build-Native%20JDK%20%2F%20run.bat-success)](run.bat)
[![Tests](https://img.shields.io/badge/Tests-297%20Passed%20(100%25)-brightgreen)](docs/TESTING.md)
[![Visual Regression](https://img.shields.io/badge/Visual%20Layout-30%20Inspected%20(0%20Overflows)-brightgreen)](docs/TESTING.md)
[![Course](https://img.shields.io/badge/Course-Programming%20in%20Java%20(25CSE0202)-blueviolet)](docs/VIVA_PREPARATION.md)

> **NextStep** is an enterprise-grade, console-based campus placement automation system built using pure Java SE, JDBC, and MySQL.  
> Developed as part of the Semester 3 **Programming in Java (25CSE0202)** curriculum for Continuous Assessment (CA-3).

---

## 📚 Master Documentation Index

| Document | Purpose / Highlights |
| :--- | :--- |
| 📋 [**Project Overview & Problem Statement**](docs/PROJECT_OVERVIEW.md) | Problem statement, background, collegiate context, target users, objectives, and scope. |
| 🏛️ [**System Architecture & Design Patterns**](docs/SYSTEM_ARCHITECTURE.md) | 4-Tier layered architecture, DAO pattern, package breakdown, and Mermaid sequence diagrams. |
| ☕ [**Java SE, OOP & Syllabus Concepts**](docs/JAVA_AND_OOP_CONCEPTS.md) | 4 OOP pillars, Collections, Streams, Lambdas, Generics, File I/O, with source code references. |
| 🔌 [**Complete JDBC Architecture Guide**](docs/JDBC_COMPLETE_GUIDE.md) | JDBC API, 4 Driver types, PreparedStatement vs Statement, connection lifecycle, and transactions. |
| 🗄️ [**Database Design & SQL Schema**](docs/DATABASE_DESIGN_AND_SQL.md) | MySQL schema (`nextstep_db`), 8 tables inventory, foreign keys, 3NF normalization, and ER diagram. |
| 🔄 [**Data Lifecycles & Modules Directory**](docs/DATA_FLOW_AND_MODULES.md) | 6 complete data lifecycles (Registration to CSV) and documentation of all 10 application pages. |
| 🔒 [**Security & Validation Architecture**](docs/SECURITY_AND_VALIDATION.md) | Salted PBKDF2 cryptography, RBAC authorization, timing attack defense, and input validation bounds. |
| 🧪 [**Testing & Verification Guide**](docs/TESTING_AND_RESULTS.md) | 297 automated tests (100% pass rate) across 7 test suites, and 30 visual layout inspections. |
| 🚀 [**Future Scope & System Roadmap**](docs/FUTURE_SCOPE.md) | 12 planned future enhancements (Web UI, JavaFX, Resume parsing, Docker, ML matching). |
| 🎓 [**Master Viva Question Bank (105 Q&A)**](docs/VIVA_QUESTION_BANK.md) | 105 professor viva questions and spoken answers across 7 comprehensive categories. |
| 🎙️ [**Professor Presentation Script**](docs/PROJECT_PRESENTATION_SCRIPT.md) | 10–15 minute complete oral demo script and 2-minute quick pitch version. |
| 📘 [**Demo & Operational User Manual**](docs/DEMO_USER_MANUAL.md) | Step-by-step presentation manual, copy-ready demo data, Pages 1–10 walkthroughs. |
| 🤝 [**Contributing Guide**](CONTRIBUTING.md) | Guidelines for collaborators, Git branching workflow, coding standards, and local database isolation rules. |

---

## 1. Project Overview & Problem Solved

Traditional campus placement operations rely heavily on disconnected spreadsheets, physical paper forms, manual eligibility calculations, and uncoordinated communication channels. These manual processes introduce substantial vulnerabilities:

1. **Human Error in Academic Auditing:** Manually cross-referencing degree branches, cumulative CGPA cutoffs, and active student backlog counts against corporate criteria is slow and prone to oversight.
2. **Skill Mismatch & Subjectivity:** Students often lack clear insight into how their technical skill profiles match specific job requirements before submitting applications.
3. **Duplicate & Ineligible Submissions:** Without database-enforced transactional constraints, students frequently submit duplicate applications or apply for openings where they fail hard academic cutoffs.
4. **Security Risks:** Storing passwords in unhashed or reversible formats exposes sensitive student and institutional records.
5. **Lack of Lifecycle Traceability:** Placement administrators struggle to track candidate lifecycles (`APPLIED` → `SHORTLISTED` → `SELECTED` / `REJECTED`) and export audited recruitment ledgers for accreditation compliance.

### The NextStep Solution
**NextStep** resolves these challenges by providing a centralized, terminal-driven recruitment platform powered by a normalized relational MySQL backend and Core Java SE architecture:
- **Relational Integrity:** 8 normalized MySQL tables with foreign key cascades, unique indexes, and ACID transaction rollbacks.
- **Automated Eligibility Engine:** Dual-phase evaluation checking academic cutoffs and set-theoretic technical skill overlap.
- **Cryptographic Security:** Salted PBKDF2 password derivation with HMAC-SHA256 (65,536 iterations, 16-byte cryptographically random salt).
- **Role-Based Access Control (RBAC):** Strict separation of privileges between `STUDENT` and `PLACEMENT_OFFICER` roles.
- **Responsive Terminal Presentation:** 24-bit TrueColour ANSI theme with automatic horizontal centering across 80, 100, and 120-column console windows.
- **Centralized Demonstration Mode:** Dedicated management system maintaining deterministic student, officer, and job profiles with one-click shortcut autofill.

---

## 2. Technology Stack

| Layer | Technology | Specification / Implementation Details |
| :--- | :--- | :--- |
| **Language** | Java SE | Java 17 LTS / Java 21 LTS (Pure Core Java SE, zero third-party web frameworks) |
| **Database** | MySQL Server | Version 8.0+ (InnoDB Storage Engine, UTF-8 Charset, Normalized 3NF Schema) |
| **JDBC Driver** | MySQL Connector/J | Version 8.4.0 (`lib/mysql-connector-j-8.4.0.jar`) |
| **Cryptography** | PBKDF2 with HMAC-SHA256 | 16-byte random salt via `SecureRandom`, 65,536 iterations, 256-bit derived key |
| **Terminal UI** | 24-bit TrueColour ANSI | Cyan `#6FC9FF`, Gold `#E3B341`, Slate `#CAD5E5`, Green `#56D364`, Red `#F85149` |
| **Responsive Layout** | Auto-Margin Engine | Dynamic terminal width detection (`COLUMNS` / `mode con`) with auto-centering |
| **Data Processing** | Java Streams & Lambdas | `filter`, `map`, `distinct`, `sorted`, `Collectors.groupingBy`, `partitioningBy` |
| **File I/O** | RFC-4180 CSV Engine | Try-with-resources `BufferedReader`/`BufferedWriter` with escape sequence parsing |
| **Build & Launch** | Native JDK CLI + Batch | Windows `run.bat` launcher; native `javac` and `java` commands for Linux/macOS |

---

## 3. Architecture & Project Structure

NextStep adheres strictly to a **Layered Architecture (Separation of Concerns)** and the **DAO (Data Access Object) Design Pattern**:

```text
┌────────────────────────────────────────────────────────────────────────┐
│                   PRESENTATION & CONSOLE UI LAYER                      │
│       Main.java (Menus, Input Guards, Mode Selectors, Responsive Centering)
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Calls
┌───────────────────────────────────▼────────────────────────────────────┐
│                    SERVICE LAYER (Business Logic)                      │
│   AuthService.java       RecruitmentService.java       DemoService.java│
│   - PBKDF2 Hashing       - Dynamic Eligibility Engine  - Centralized DB│
│   - Atomic Registration  - Stream Analytics & CSV      - Scenarios A-C │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Calls
┌───────────────────────────────────▼────────────────────────────────────┐
│                   DATA ACCESS LAYER (DAO Pattern)                      │
│   UserDAO       StudentDAO       CompanyDAO      JobDAO       AppDAO   │
│   - PreparedStatements         - Parameterized SQL Queries             │
│   - Transaction Management     - ResultMapping to Domain POJOs         │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Connects via JDBC
┌───────────────────────────────────▼────────────────────────────────────┐
│                    DATABASE LAYER (MySQL Server 8.0+)                  │
│    nextstep_db (8 Normalized Relational Tables with Foreign Key Cascades)│
└────────────────────────────────────────────────────────────────────────┘
```

### Source Tree Directory Breakdown

```text
NextStep-Campus-Recruitment-System/
├── config/
│   ├── db.properties.example             # Git-tracked database credentials template
│   └── db.properties                     # Local database credentials (Git-ignored)
├── database/
│   ├── schema.sql                        # DDL: 8 normalized relational tables & indexes
│   └── sample_data.sql                   # DML: Idempotent seed data (companies, jobs, skills)
├── docs/
│   ├── DEMO_USER_MANUAL.md               # Step-by-step presentation & demo operational manual
│   ├── TESTING.md                        # Comprehensive test suites documentation (297 tests)
│   └── VIVA_PREPARATION.md               # Oral examination preparation guide (50+ Q&A)
├── lib/
│   └── mysql-connector-j-8.4.0.jar      # Official MySQL Connector/J JDBC Driver (v8.4.0)
├── reports/                              # Output directory for exported CSV reports (Git-ignored)
├── src/
│   └── com/
│       └── nextstep/
│           ├── model/                    # Domain POJOs (Role, User, Student, Company, Job, Application)
│           ├── database/                 # DatabaseConnection (centralized JDBC manager)
│           ├── dao/                      # Data Access Objects (UserDAO, StudentDAO, CompanyDAO, JobDAO, ApplicationDAO)
│           ├── service/                  # Business services (AuthService, RecruitmentService, DemoService)
│           ├── util/                     # Utilities (ConsoleTheme, PresentationConfig, Result, ValidationUtil)
│           └── Main.java                 # Interactive console application entry point
├── tests/                                # Standalone regression & integration test suites
│   ├── CentralizedDemoManagerTestRunner.java # Centralized demo data management & A-shortcut suite (11 tests)
│   ├── DemoModeVerificationTestRunner.java   # Repeatable demo mode & scenario suite (24 tests)
│   ├── ConsoleInputEdgeCaseTestRunner.java   # Console UI, shortcuts & edge-case suite (89 tests)
│   ├── Phase3TestRunner.java                 # Registration & PBKDF2 cryptography suite (41 tests)
│   ├── Phase4TestRunner.java                 # Recruitment workflow & persistence suite (47 tests)
│   ├── Phase5TestRunner.java                 # Streams, Collectors & CSV archival suite (54 tests)
│   ├── Phase6IntegrationTestRunner.java      # Transaction rollback & restart stability suite (31 tests)
│   ├── VisualInspectionVerifier.java         # Visual regression & responsiveness inspector (30 screens)
│   └── InspectDatabase.java                  # Standalone database table count inspector
├── .gitignore                            # Excludes credentials, compiled bytecode, reports
├── CONTRIBUTING.md                       # Guidelines for collaborators and contributors
├── run.bat                               # One-click Windows build and launch script
└── README.md                             # Main project overview and technical documentation
```

---

## 4. Key Implemented Features

### 🏛️ Unified Campus Gateway (Page 1)
- **Centralized Access Portal:** Eliminates unnecessary splash screens by combining live database metrics (Active Drives, Registered Candidates, Applications Filed, Average Offered CTC), academic year indicators, and direct portal routing into a 64-column framed dashboard.

### 🎓 Student Recruitment Workflow
- **Student Registration (Page 2):** Self-service onboarding featuring a standardized Two-Pane Mode Choice Box (`[ A ] Fetch Saved Demo Data | [ ENTER ] Manual Entry`). Golden Amber field prompt labels aligned with Sky Blue bracketed input previews `[ Value ]`. Passwords hashed with PBKDF2 with HMAC-SHA256 and a 16-byte salt. Multi-table atomic JDBC transaction across `users`, `students`, and `student_skills`.
- **Student Sign In (Page 3):** Dual-identifier authentication supporting either Username or University Roll Number (USN). Features the standardized mode choice box supporting shortcuts `A` / `A1` (Vihaan), `A2` (Kabir), `A3` (Arjun), with masked password input via `System.console().readPassword()`.
- **Student Placement Hub (Page 6):** Personalized dashboard displaying candidate name, USN, CGPA, registered branch, and active backlog count, with 4 direct service actions (Browse Drives, Check Eligibility & Match, Apply for Opening, View Applications).
- **Unified Eligibility & Skill Match Matrix (Page 8):** Renders academic cutoff checklist (CGPA, backlogs, branch, graduation year) alongside set-theoretic technical skill overlap percentages in a side-by-side corporate layout.
- **Application Review & Instant Digital Receipt (Page 9):** Structured pre-submission review panel verifying student and job specifications before transmission. Upon confirmation, persists the record in MySQL and issues an official Digital Receipt Voucher with unique `APP-2026-XXX` identifier and filing timestamp.

### 👔 Placement Officer Administration Workflow
- **Officer Registration & Onboarding (Page 4):** Administrative account creation featuring upfront mode selection (`[ A ] / [ ENTER ]`). Secured by an application-level Activation Code (`-Dnextstep.officer.activation.code` or `NEXTSTEP_OFFICER_ACTIVATION_CODE`) with zero hardcoded default passwords.
- **Officer Sign In (Page 5):** Administrative authentication gateway supporting mode selection (`[ A ] = officer_vihaan | [ ENTER ] Manual`).
- **Placement Officer Dashboard (Page 7):** Operational workstation displaying recruitment counts and 4 administrative tools (Create Job Drive, Manage Applications, Export CSV, Presentation Controls).
- **Create Job Drive (Tool 1):** Publish new corporate drives selecting from live MySQL partner companies. Collects all 10 critical parameters (role, description, CTC, location, min CGPA, max backlogs, batch, branches, skills) and persists atomically.
- **Master Application Registry (Page 10):** Structured table displaying candidate roll numbers, names, job titles, and colorized status indicators (`APPLIED` in Cyan, `SHORTLISTED` in Green, `REJECTED` in Red) with inline candidate status updating.
- **Recruitment Analytics & Archiving:** Computes live placement partitions using Java Streams (`groupingBy`, `partitioningBy`) and exports RFC-4180 compliant CSV reports with round-trip audit verification.

### 🚀 Centralized Demo Data Manager & Application-Wide `A` Shortcut
- **Gateway Option 4 Control Center:** Dedicated management console providing 8 persistent administrative operations:
  1. *Prepare / Verify Demo Data (Idempotent)*
  2. *View Demo Credentials & Scenario Reference Guide*
  3. *Manage Demo Student Profiles (Scenarios A, B, C)*
  4. *Manage Demo Placement Officer Profile*
  5. *Manage Demo Partner Companies*
  6. *Manage Demo Job Drive Specifications*
  7. *Manage Demo Applications & Status Tracking*
  8. *Reset Demonstration Data Only (Protected with keyword confirmation)*
- **Deterministic Evaluation Scenarios:**
  - **Scenario A (Fully Eligible):** Vihaan Mehta (`vihaan_demo` / USN `2510990750` / `NextStep@2026`) — 100% Eligible for Junior Backend Engineer opening.
  - **Scenario B (Skill-Gap Candidate):** Kabir Malhotra (`kabir_demo` / USN `2510990751` / `NextStep@2026`) — Passes academic cutoffs; matches 50% skills (Java, SQL); missing OOP and Git.
  - **Scenario C (Academically Ineligible):** Arjun Khanna (`arjun_demo` / USN `2510990752` / `NextStep@2026`) — Ineligible due to CGPA below cutoff, backlogs exceeding limit, and branch mismatch.
  - **Placement Officer Profile:** Dr. Ramesh Sharma (`officer_vihaan` / ID `OFF-2026-001` / `NextStep@2026` / Activation Code: `CAMPUS_OFFICER_2026`).
  - **Corporate Drive Profile:** Nexis Cloud Technologies (`Junior Backend Engineer` / 12.00 LPA / Min CGPA 7.50 / Max Backlogs 0 / Java, Spring Boot, MySQL, REST APIs).
- **Application-Wide Shortcut `A`:** Available on Student Sign Up (Page 2), Student Sign In (Page 3), Officer Sign Up (Page 4), Officer Sign In (Page 5), Create Job Drive (Page 7), Check Eligibility (Student Hub), and Apply for Opening (Student Hub).
- **Review Before Submission Guarantee:** Pressing `A` fetches centralized values and populates fixed-width brackets `[ Value ]`, allowing full review and field-by-field editing before explicit user submission. Never prematurely auto-submits.

### 🎨 Responsive Terminal Centering & ANSI Presentation
- **Standardized 64-Column Box Framing:** All 10 pages and submenus use a consistent 64-column layout (`FRAME_WIDTH = 64`) with double borders (`===`) and single dividers (`---`).
- **Dynamic Terminal Width Detection:** Automatically queries console width via `-Dnextstep.columns`, `COLUMNS` environment variable, or Windows `mode con` query.
- **Horizontal Centering:** Calculates responsive left margin `(columns - 64) / 2` when running in wide terminals:
  - **Standard 80-Column Terminal:** Automatically indented by 8 spaces (equal 8-space margins on both sides).
  - **100-Column Terminal:** Automatically indented by 18 spaces.
  - **120-Column Terminal (Windows Terminal):** Automatically indented by 28 spaces, eliminating horizontal dead space.
  - **Narrow Terminal ($\le 64$ Columns):** Margin safely defaults to 0; prevents negative padding or clipping.
- **Clean Scrollback Purging:** `clearConsole()` flushes `\033[H\033[2J\033[3J`, wiping visible viewports and scrollback history on transitions.

---

## 5. Application Pages Map

NextStep organizes campus recruitment operations across 10 approved primary screens plus the centralized Demonstration Mode:

```text
========================================================================================
[Page 1: Welcome & Unified Campus Gateway]
  │
  ├──► [1. Student Portal]
  │       ├──► [Page 2: Student Sign Up / Registration]
  │       │       └── [Two-Pane Mode: 'A' Demo Autofill | 'ENTER' Manual Form]
  │       └──► [Page 3: Student Sign In Portal]
  │               └── [Two-Pane Mode: 'A' / 'A1' (Vihaan) | 'A2' (Kabir) | 'A3' (Arjun)]
  │                       │
  │                       ▼
  │               [Page 6: Student Placement Hub Dashboard]
  │                       ├── [1] Browse Available Recruitment Drives
  │                       ├── [2] Check Eligibility & Skill Match ──► [Page 8: Evaluation Matrix]
  │                       ├── [3] Apply for Recruitment Opening ──► [Page 9: Review & Receipt]
  │                       ├── [4] View My Application History
  │                       └── [0] Sign Out to Campus Gateway
  │
  ├──► [2. Placement Officer Portal]
  │       ├──► [Page 4: Placement Officer Sign Up / Onboarding]
  │       │       └── [Two-Pane Mode: 'A' Demo Profile | 'ENTER' Manual Form]
  │       └──► [Page 5: Placement Officer Sign In Portal]
  │               └── [Two-Pane Mode: 'A' Demo Officer | 'ENTER' Manual Sign In]
  │                       │
  │                       ▼
  │               [Page 7: Placement Officer Administration Dashboard]
  │                       ├── [1] Create & Publish Job Drive ─────► [Page 7 (Tool 1): Job Form]
  │                       ├── [2] Manage Student Applications ───► [Page 10: Master Registry]
  │                       ├── [3] Export Placement Ledger (CSV)
  │                       ├── [4] Presentation Visibility Controls
  │                       └── [0] Sign Out to Campus Gateway
  │
  ├──► [3. View Campus Recruitment Statistics]
  └──► [4. Demonstration Mode & Scenario Setup] (Centralized Demo Data Manager)
```

---

## 6. Installation & Database Setup

### Prerequisites
- **Operating System:** Windows 10/11, Linux (Ubuntu/Debian/Fedora), or macOS.
- **Java Development Kit (JDK):** Version 17 LTS, 21 LTS, or newer.  
  - Verify: `java -version` and `javac -version`
- **MySQL Server:** Community Server 8.0 or newer.  
  - Verify service: `net start MySQL80` (Windows) or `systemctl status mysql` (Linux).
- **Terminal:** Windows Terminal, PowerShell, CMD, or Unix bash/zsh with ANSI color support.

---

### Step 1: Clone Repository
```bash
git clone https://github.com/raghavbansal9518-cloud/NextStep-Campus-Recruitment-System.git
cd NextStep-Campus-Recruitment-System
```

### Step 2: Initialize Database Schema & Seed Data
Execute the idempotent SQL scripts in order using MySQL CLI:

#### On Windows (PowerShell / Command Prompt):
```powershell
Get-Content database/schema.sql | mysql -u root -p
Get-Content database/sample_data.sql | mysql -u root -p
```
*(Or via Command Prompt: `mysql -u root -p < database\schema.sql`)*

#### On Linux / macOS (Bash):
```bash
mysql -u root -p < database/schema.sql
mysql -u root -p < database/sample_data.sql
```
*Both scripts are idempotent; re-running them safely resets or maintains seed data without creating duplicates.*

### Step 3: Configure Database Credentials
Copy the configuration template:
```cmd
copy config\db.properties.example config\db.properties
```
Open `config/db.properties` in a text editor and update your local MySQL root password:
```properties
db.url=jdbc:mysql://localhost:3306/nextstep_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=YOUR_LOCAL_MYSQL_PASSWORD
```
> [!IMPORTANT]
> `config/db.properties` is registered in `.gitignore` and is never committed to GitHub. Each collaborator maintains private credentials locally.

### Step 4: Configure Placement Officer Activation Code
To safeguard administrative account creation on Page 4, configure an application activation code:

#### On Windows (One-Time Setup via Command Prompt):
```cmd
setx NEXTSTEP_OFFICER_ACTIVATION_CODE "CAMPUS_OFFICER_2026"
```
*(Close and re-open your terminal window after running `setx` for the environment variable to take effect).*

#### Per-Session JVM Property Alternative:
Pass the activation code directly to the JVM when launching:
```powershell
java "-Dnextstep.officer.activation.code=CAMPUS_OFFICER_2026" -cp "bin;lib/*" com.nextstep.Main
```

---

## 7. Running the Application

### Option A: One-Click Windows Launcher (`run.bat`) — Recommended
Double-click **`run.bat`** in File Explorer or run it from your terminal:
```cmd
run.bat
```
The script validates your JDK and JDBC driver, verifies `config/db.properties`, compiles all source files into `bin/`, and launches `com.nextstep.Main`.

### Option B: Command-Line Compilation & Execution

#### On Windows (PowerShell):
```powershell
# 1. Compile source files into bin/
javac -d bin -cp "lib/*;src" (Get-ChildItem -Path src -Recurse -Filter *.java | ForEach-Object { $_.FullName })

# 2. Run the application
java -cp "bin;lib/*" com.nextstep.Main
```

#### On Windows Command Prompt (CMD):
```cmd
:: 1. Compile source files into bin/
javac -d bin -cp "lib\mysql-connector-j-8.4.0.jar" src\com\nextstep\model\*.java src\com\nextstep\database\*.java src\com\nextstep\dao\*.java src\com\nextstep\service\*.java src\com\nextstep\util\*.java src\com\nextstep\Main.java

:: 2. Run the application
java -cp "bin;lib\mysql-connector-j-8.4.0.jar" com.nextstep.Main
```

#### On Linux / macOS (Bash):
```bash
# 1. Compile source files into bin/
javac -cp ".:lib/*" -d bin $(find src -name "*.java")

# 2. Run the application
java -cp "bin:lib/*" com.nextstep.Main
```

---

## 8. Demonstration Mode & Scenarios Guide

NextStep includes a complete **Demonstration Mode** designed for live academic presentations:

### Accessing Demonstration Mode
1. **Interactive Menu:** Enter `4` at the Welcome Gateway (Page 1).
2. **CLI Flag:** Launch with `--demo` or `--demo-setup`:
   ```powershell
   java -cp "bin;lib/*" com.nextstep.Main --demo
   ```

### Deterministic Demonstration Profiles
All demo accounts use the standard demonstration password: **`NextStep@2026`** (encrypted in MySQL with PBKDF2 salt:hash):

| Scenario / Profile | Role | Username | Identifier (USN/ID) | Academic / Criteria Profile | Expected Evaluation |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **Scenario A** | Student | `vihaan_demo` | `2510990750` | CGPA 8.90, 0 Backlogs, CSE, Skills: Java, Spring Boot, MySQL, REST APIs | **100% ELIGIBLE** (Passes all cutoffs & skills) |
| **Scenario B** | Student | `kabir_demo` | `2510990751` | CGPA 8.10, 0 Backlogs, CSE, Skills: Java, SQL (Missing: OOP, Git) | **SKILL GAP** (Academic Pass, 50% Skills Matched) |
| **Scenario C** | Student | `arjun_demo` | `2510990752` | CGPA 5.80, 3 Backlogs, Mechanical Engineering | **NOT ELIGIBLE** (Fails CGPA, backlogs & branch) |
| **Officer Profile** | Officer | `officer_vihaan` | `OFF-2026-001` | Training and Placement Officer (Dept: T&P) | **ADMINISTRATOR** (Full oversight & CSV export) |
| **Demo Job Drive** | Company | *Nexis Cloud* | Drive `#1` | Junior Backend Engineer, 12.00 LPA, Min CGPA 7.50, 0 Backlogs | **OPEN DRIVE** (10 Vacancies) |

### Protected Demo-Only Reset
Option 8 in the Demo Data Manager (`Reset Demonstration Data Only`) cleans up demonstration candidate records while **100% protecting real student accounts and partner companies**. To prevent accidental execution, it requires typing `CONFIRM` or `RESET`.

---

## 9. Comprehensive Testing & Verification

NextStep is validated by **297 automated functional tests across 7 test runners**, complemented by **30 automated visual inspection and responsiveness checks**:

```text
========================================================================================
                      NEXTSTEP AUTOMATED TEST VERIFICATION SUITE
========================================================================================
Test Runner Class                    Scope & Verification Details              Tests    Status
────────────────────────────────────────────────────────────────────────────────────────
CentralizedDemoManagerTestRunner     Demo profiles, MySQL edits, 'A' shortcut   11      PASS
DemoModeVerificationTestRunner       Idempotence, Scenarios A-C, safe reset     24      PASS
ConsoleInputEdgeCaseTestRunner       Pages 1-10 UI, shortcuts, form bounds      89      PASS
Phase3TestRunner                     Student registration, PBKDF2 hashing, DAOs 41      PASS
Phase4TestRunner                     Job persistence, eligibility, applications 47      PASS
Phase5TestRunner                     Java Streams, Collectors, CSV audit archive 54      PASS
Phase6IntegrationTestRunner          Rollback atomicity, simulated restarts     31      PASS
────────────────────────────────────────────────────────────────────────────────────────
Total Automated Functional Tests     100% Database-Verified Success Rate       297      PASS
────────────────────────────────────────────────────────────────────────────────────────
VisualInspectionVerifier             All 10 pages, 30 screens <= 64 columns     30      PASS
========================================================================================
```

### Running All Test Suites Sequentially

#### On Windows PowerShell:
```powershell
javac -cp "lib/*;bin" -d bin tests/*.java
java -cp "bin;lib/*" CentralizedDemoManagerTestRunner
java -cp "bin;lib/*" DemoModeVerificationTestRunner
java -cp "bin;lib/*" Phase3TestRunner
java -cp "bin;lib/*" Phase4TestRunner
java -cp "bin;lib/*" Phase5TestRunner
java -cp "bin;lib/*" Phase6IntegrationTestRunner
java "-Dnextstep.officer.activation.code=CAMPUS_OFFICER_2026" -cp "bin;lib/*" ConsoleInputEdgeCaseTestRunner
java -cp "bin;lib/*" VisualInspectionVerifier
```

For complete testing documentation, test case breakdowns, and database isolation details, see 👉 [**docs/TESTING.md**](docs/TESTING.md).

---

## 10. Academic Notice & Course Context

This project was developed for educational coursework and laboratory evaluation under the **Programming in Java (25CSE0202)** course for Continuous Assessment CA-3. It is intended for academic demonstration, evaluation, and study.
