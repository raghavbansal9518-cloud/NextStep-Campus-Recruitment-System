# NextStep — Campus Recruitment System

[![Java Version](https://img.shields.io/badge/Java-17%20%7C%2021%20LTS-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Database](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![JDBC Driver](https://img.shields.io/badge/MySQL%20Connector%2FJ-8.4.0-blue)](https://dev.mysql.com/downloads/connector/j/)
[![Build Tool](https://img.shields.io/badge/Build-Native%20JDK%20%2F%20run.bat-success)](run.bat)
[![Tests](https://img.shields.io/badge/Tests-212%20Passed%20(100%25)-brightgreen)](#11-testing-and-verification)
[![Course](https://img.shields.io/badge/Course-Programming%20in%20Java%20(25CSE0202)-blueviolet)](docs/VIVA_PREPARATION.md)

> A console-based campus placement automation system built with pure Java SE, JDBC, and MySQL.  
> Developed as part of the Semester 3 **Programming in Java (25CSE0202)** curriculum (Continuous Assessment CA-3).

---

## 📘 Demo & Testing Manual

> [!TIP]
> **Preparing for an evaluation, live laboratory presentation, or project viva?**  
> We have created a separate, comprehensive, step-by-step presentation manual:  
> 👉 [**docs/DEMO_USER_MANUAL.md**](docs/DEMO_USER_MANUAL.md)  
> It contains copy-ready demo data, live walkthrough scripts, common questions, and a pre-flight inspection checklist.

---

## 1. Project Title & Introduction

**NextStep** is an interactive, role-based terminal application designed to streamline campus recruitment drives for students, placement cells, and hiring partner companies. 

In traditional university placement drives, candidate management often suffers from fragmented communication, manual eligibility verification across spreadsheets, paper resumes, and duplicate applications. **NextStep** addresses these challenges by centralizing academic records, automated eligibility rules, skill-matching algorithms, and administrative oversight into a unified console interface powered by a relational MySQL backend.

The project strictly emphasizes Core Java principles, JDBC transaction management, Object-Oriented design patterns, modern Java Streams/Lambdas, and cryptographic security—built without heavy third-party web frameworks so that every line of code remains transparent, demonstrable, and defensible in viva examinations.

---

## 2. Key Features

### 🎓 Student Portal
- **Self-Service Registration (Page 3):** Creates user credentials, personal profiles, and academic records atomically in MySQL, with passwords encrypted using PBKDF2 with HMAC-SHA256 and a 16-byte cryptographic salt.
- **Secure Authentication (Page 4):** Supports login using either username or University Roll Number (USN), with hidden password entry via `System.console().readPassword()`.
- **Job Browsing (Page 6):** View active recruitment drives sorted by highest package CTC using custom Java Lambda comparators.
- **Automated Eligibility Engine (Page 9 Part A):** Evaluates academic standing (CGPA cutoff, maximum active backlogs, degree branch, and graduation year) against live drive criteria before permitting applications.
- **Skill Match Analysis (Page 9 Part B):** Real-time skill alignment engine calculating matched vs. missing skills and dynamic match percentages.
- **Job Application & Confirmation (Page 10):** Interactive review screen with database-enforced duplicate prevention (`uq_student_job`).
- **Application Tracking (Page 6):** Live tracking of submitted applications and status changes.

### 👔 Placement Officer Portal
- **Administrative Activation Safeguard (Page 5):** Officer onboarding is guarded by a locally configured environment variable (`NEXTSTEP_OFFICER_ACTIVATION_CODE`), preventing unauthorized student privilege escalation.
- **Job Drive Management (Page 8):** Publish new job drives with dynamic partner company lookup directly from MySQL, target batch years, eligible branches, and required technical skills.
- **Centralized Applicant Oversight (Page 11):** Inspect all student applications across partner companies with candidate roll numbers, names, and current recruitment statuses.
- **Status Lifecycle Advancement:** Advance candidate applications through the hiring lifecycle (`APPLIED` → `SHORTLISTED` → `SELECTED` / `REJECTED`) with database consistency.
- **Recruitment Analytics:** Compute real-time placement statistics using Java Streams and Collectors (`groupingBy`, `partitioningBy`, `counting`).
- **RFC-4180 CSV Export & Audit Archiving:** Export applicant records to CSV and ingest/parse exported files into `ArrayList<Application>` for audit verification.

### 🎨 Terminal Presentation & Console Usability
- **24-bit TrueColour ANSI Theme:** Reference palette featuring Sky Blue borders (`#6FC9FF`), Gold input prompts (`#E3B341`), Slate body text (`#CAD5E5`), and Midnight Navy background (`#131B28`).
- **Fixed-Width Column Alignment:** Standardized 24-character label column across all forms with user input beginning at column 25.
- **Form Navigation Shortcuts:** Inline typing corrections supporting `B` / `ESC` (return to previous field), `N` / `TAB` (advance retaining value), and `[Enter]` (confirm/keep existing value).
- **Clean Plain-Text Fallback:** Automatically detects headless environments and stripped ANSI escape codes when running in automated test pipes or non-interactive terminals.

---

## 3. Technology Stack

| Layer | Technology | Details |
| :--- | :--- | :--- |
| **Programming Language** | Java SE | Java 17 LTS / Java 21 LTS (Tested on JDK 17 through JDK 26) |
| **Database Engine** | MySQL Server | Version 8.0+ (InnoDB Storage Engine, UTF-8 Charset) |
| **JDBC Driver** | MySQL Connector/J | Version 8.4.0 (`lib/mysql-connector-j-8.4.0.jar`) |
| **Password Security** | PBKDF2 with HMAC-SHA256 | 16-byte random salt, 65,536 iterations, 256-bit key length |
| **Architecture** | Layered Console Architecture | Model – DAO – Service – Presentation |
| **Build & Launch** | Native JDK CLI + Batch | Windows `run.bat` launcher; native `javac` and `java` commands |

---

## 4. Project Folder Structure

```text
NextStep-Campus-Recruitment-System/
├── config/
│   ├── db.properties.example             # Git-tracked database configuration template
│   └── db.properties                     # Local database credentials (Git-ignored)
├── database/
│   ├── schema.sql                        # DDL: 8 normalized relational tables & indexes
│   └── sample_data.sql                   # DML: Idempotent seed data (companies, jobs, skills)
├── docs/
│   ├── DEMO_USER_MANUAL.md               # Step-by-step presentation & demo operational manual
│   └── VIVA_PREPARATION.md               # Comprehensive oral examination preparation guide
├── lib/
│   └── mysql-connector-j-8.4.0.jar      # Official MySQL JDBC Driver
├── reports/                              # Output directory for generated CSV reports
├── src/
│   └── com/
│       └── nextstep/
│           ├── model/                    # Domain POJOs and Enums
│           │   ├── Role.java             # Roles: STUDENT, PLACEMENT_OFFICER
│           │   ├── ApplicationStatus.java# APPLIED, SHORTLISTED, SELECTED, REJECTED
│           │   ├── User.java             # Base user account entity
│           │   ├── Student.java          # Student profile entity
│           │   ├── Company.java          # Partner hiring entity
│           │   ├── Job.java              # Recruitment drive entity
│           │   └── Application.java      # Job application entity
│           ├── database/
│           │   └── DatabaseConnection.java# Centralized JDBC connection manager
│           ├── dao/                      # Data Access Objects (SQL queries & mapping)
│           │   ├── UserDAO.java
│           │   ├── StudentDAO.java
│           │   ├── CompanyDAO.java
│           │   ├── JobDAO.java
│           │   └── ApplicationDAO.java
│           ├── service/                  # Business logic & operational workflows
│           │   ├── AuthService.java      # Registration, PBKDF2 hashing, login
│           │   └── RecruitmentService.java# Eligibility, lifecycle, streams, CSV I/O
│           ├── util/                     # Utilities & configuration helpers
│           │   ├── ConsoleTheme.java     # 24-bit TrueColour ANSI formatting & fallbacks
│           │   ├── PresentationConfig.java# Presentation visibility toggles
│           │   ├── Result.java           # Generic Result<T> container
│           │   └── ValidationUtil.java   # Input validation & generic pagination
│           └── Main.java                 # Interactive console application entry point
├── .gitignore                            # Excludes credentials, compiled bytecode, reports
├── run.bat                               # One-click Windows build and launch script
└── README.md                             # Main project documentation
```

---

## 5. System Requirements

Before installing and running NextStep, ensure your computer satisfies these prerequisites:

1. **Operating System:** Windows 10/11, Linux (Ubuntu/Debian/Fedora), or macOS.
2. **Java Development Kit (JDK):** Version 17, 21 LTS, or newer.  
   - Verify by running `java -version` and `javac -version` in your terminal.
3. **MySQL Server:** Community Server 8.0 or newer.  
   - Verify service is active: `net start MySQL80` (Windows) or `systemctl status mysql` (Linux).
4. **Terminal:** Windows Terminal, PowerShell, Command Prompt, or Unix bash/zsh supporting ANSI escape codes (minimum recommended buffer: 80 columns × 25 lines).

---

## 6. Installation & Setup

### Step 1: Clone the Repository
```bash
git clone https://github.com/raghavbansal9518-cloud/NextStep-Campus-Recruitment-System.git
cd NextStep-Campus-Recruitment-System
```

### Step 2: Initialize MySQL Database & Seed Data
Log in to your local MySQL server using the command-line client:
```bash
mysql -u root -p
```
Execute the database initialization scripts in order:
```bash
mysql -u root -p < database/schema.sql
mysql -u root -p < database/sample_data.sql
```
> [!NOTE]
> Both scripts are completely idempotent. Running them multiple times will safely update or maintain existing records without creating duplicates.

Verify that the 8 normalized tables were created in `nextstep_db`:
```sql
USE nextstep_db;
SHOW TABLES;
```

### Step 3: Configure Database Credentials
Create your local database configuration file from the provided template:
```powershell
# Windows PowerShell:
Copy-Item config/db.properties.example config/db.properties

# Windows Command Prompt:
copy config\db.properties.example config\db.properties
```

Open `config/db.properties` in your text editor and specify your local MySQL root password:
```properties
db.url=jdbc:mysql://localhost:3306/nextstep_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=YOUR_LOCAL_MYSQL_PASSWORD
```

---

## 7. Running the Application

### Option A: One-Click Windows Launcher (`run.bat`) — Recommended
Double-click **`run.bat`** in File Explorer, or execute it from your terminal:
```cmd
run.bat
```
The batch script automatically:
1. Navigates to the project directory.
2. Verifies that `javac`, `java`, and `lib/mysql-connector-j-8.4.0.jar` are available.
3. Ensures `config/db.properties` exists.
4. Compiles all source packages into `bin/`.
5. Launches `com.nextstep.Main`.
6. Keeps the terminal window open if an error occurs so you can read diagnostic messages.

### Option B: Manual Command-Line Compilation & Execution

#### On Windows (PowerShell):
```powershell
# 1. Compile source files into bin/
javac -cp "lib/*;src" -d bin (Get-ChildItem -Path src -Recurse -Filter *.java | ForEach-Object { $_.FullName })

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
# 1. Compile
javac -cp ".:lib/*" -d bin $(find src -name "*.java")

# 2. Run
java -cp "bin:lib/*" com.nextstep.Main
```

---

## 8. Database Configuration

- Database connectivity is centralized in [`DatabaseConnection.java`](src/com/nextstep/database/DatabaseConnection.java) using standard JDBC `DriverManager`.
- **Local Isolation:** NextStep is designed so each developer or evaluator maintains their own isolated MySQL instance. `config/db.properties` is explicitly registered in `.gitignore` and must **never** be committed to GitHub.
- If `config/db.properties` is missing, `DatabaseConnection` falls back to default localhost settings with clear error handling if authentication fails.

---

## 9. Placement Officer Activation Code

To safeguard administrative registration, Placement Officer self-registration on Page 5 requires an authorized **Activation Code**.

> [!IMPORTANT]
> - The activation code is an **application-level safeguard**, not a database password.
> - It is **never stored in the database** and **never hardcoded in Java source files**.
> - Each collaborator configures their own local code on their computer.

### Configuring on Windows (One-Time Setup):
Run this command in Command Prompt:
```cmd
setx NEXTSTEP_OFFICER_ACTIVATION_CODE "CAMPUS_OFFICER_2026"
```
> [!NOTE]
> Close and reopen your terminal window after running `setx` for the new environment variable to take effect across future sessions and `run.bat` launches.

### Per-Session JVM Property Alternative:
If you prefer passing the code directly to the JVM without modifying environment variables:
```powershell
java -Dnextstep.officer.activation.code=CAMPUS_OFFICER_2026 -cp "bin;lib/*" com.nextstep.Main
```

If neither is configured, officer registration is safely disabled with an administrator notice.

---

## 10. Application Workflow (Pages 1–11)

NextStep organizes placement workflows across 11 structured console screens:

```
[Page 1: Welcome Page]
      │
      ▼
[Page 2: Account Portal]
      ├──► [Student Workflow]
      │         ├──► [Page 3: Student Sign Up]
      │         └──► [Page 4: Student Sign In]
      │                   │
      │                   ▼
      │              [Page 6: Student Dashboard]
      │                   ├── Option 3: Available Job Drives
      │                   ├── Option 4: Check Eligibility ──► [Page 9: Eligibility & Skill Match]
      │                   ├── Option 5: Analyse Skill Match ─► [Page 9 Part B]
      │                   ├── Option 6: Apply for Job ───────► [Page 10: Application Confirmation]
      │                   └── Option 7: View Applications
      │
      └──► [Placement Officer Workflow]
                ├──► [Page 5: Officer Sign Up]
                └──► [Officer Sign In]
                          │
                          ▼
                     [Page 7: Officer Dashboard]
                          ├── Option 3: Manage Job Drives ───► [Page 8: Create Job Drive]
                          ├── Option 4: View Applications ───► [Page 11: Application Records]
                          └── Option 5: Update Application Status
```

*(Note: Options marked `[Coming Soon]` on Dashboards—such as Profile Editing or Company Management—are reserved for CAT-4 milestones and display informative notices).*

---

## 11. Testing & Verification

NextStep includes 5 automated regression test suites covering the entire syllabus, input validation, transaction atomicity, and edge cases.

### Verified Test Results Summary

| Suite Name | Scope & Capabilities Verified | Tests | Status |
| :--- | :--- | :---: | :---: |
| **`ConsoleInputEdgeCaseTestRunner`** | Pages 1–11 layouts, TrueColour ANSI, `B`/`N`/`ESC`/`TAB` shortcuts, Page 8 company selection | **72** | **PASS** |
| **`Phase3TestRunner`** | Registration, PBKDF2 hashing, StudentDAO, CompanyDAO, JobDAO | **28** | **PASS** |
| **`Phase4TestRunner`** | Job browsing, eligibility rules, application states, officer access control | **27** | **PASS** |
| **`Phase5TestRunner`** | Full CA-3 syllabus, RFC-4180 CSV export/import, presentation visibility controls | **54** | **PASS** |
| **`Phase6IntegrationTestRunner`** | Transaction rollback atomicity, simulated restart persistence, input sanitization | **31** | **PASS** |
| **Grand Total** | **All 5 Test Suites Combined** | **212** | **212 / 212 Passed (100%)** |

### How to Run the Tests
```powershell
# Run the Comprehensive Console Verification Suite (Pages 1–11):
java -Dnextstep.officer.activation.code=CAMPUS_OFFICER_2026 -cp "bin;lib/*;scratch" ConsoleInputEdgeCaseTestRunner

# Run Regression Suites:
java -cp "bin;lib/*;scratch" Phase3TestRunner
java -cp "bin;lib/*;scratch" Phase4TestRunner
java -cp "bin;lib/*;scratch" Phase5TestRunner
java -cp "bin;lib/*;scratch" Phase6IntegrationTestRunner
```

---

## 12. GitHub Collaboration for Teammates

If you are working on this project with teammates or cloning it onto another machine:

1. **Clone the Repo:** Clone the project locally. Source code and SQL scripts are synchronized via Git.
2. **Local Isolation:** Git does **not** share your local MySQL server or local database records. Each collaborator must:
   - Run their own local MySQL server.
   - Execute `database/schema.sql` and `database/sample_data.sql`.
   - Create their own `config/db.properties` with their private MySQL root password.
   - Set their own `NEXTSTEP_OFFICER_ACTIVATION_CODE` if testing officer registration.
3. **Branching Workflow:**
   - Create a feature branch before making changes:  
     `git checkout -b feature/your-feature-name`
   - Commit using Conventional Commits (`feat: ...`, `fix: ...`, `docs: ...`).
   - Push your branch and open a reviewed pull request into `main`.

---

## 13. Troubleshooting

| Symptom | Likely Cause | Solution |
| :--- | :--- | :--- |
| `'javac' is not recognized` | JDK not installed or missing from `PATH` | Install JDK 17+ and add its `bin` directory to your system `PATH`. |
| `Communications link failure` | MySQL service is stopped | Start MySQL service via `net start MySQL80` or Services Manager. |
| `Access denied for user 'root'` | Incorrect database password | Update `db.password` in `config/db.properties` with your MySQL password. |
| `Unknown database 'nextstep_db'` | Database schema not imported | Run `mysql -u root -p < database/schema.sql`. |
| `Missing Connector/J JDBC driver` | Missing driver JAR in `lib/` | Ensure `lib/mysql-connector-j-8.4.0.jar` exists in your repository. |
| `Officer registration is disabled` | Activation code unconfigured | Run `setx NEXTSTEP_OFFICER_ACTIVATION_CODE "CAMPUS_OFFICER_2026"` and restart terminal. |
| Password typing doesn't echo | Normal Java console masking | `System.console().readPassword()` hides characters for security. Type your password and press **Enter**. |

---

## 14. Current Project Status & Roadmap

- **Completed CA-3 Scope:** Pages 1 through 11 fully implemented with database persistence, PBKDF2 encryption, TrueColour ANSI theme, navigation shortcuts, eligibility checking, skill matching, and application tracking.
- **Future Roadmap (CAT-4 Milestones):**
  - Graphical User Interface (JavaFX / Swing).
  - Resume upload and PDF document parsing.
  - Email notification dispatch via JavaMail API.
  - Recruiter interview scheduling and rating dashboard.

---

## 15. Documentation Reference

- **📘 Demonstration & Testing Manual:** [docs/DEMO_USER_MANUAL.md](docs/DEMO_USER_MANUAL.md) — Complete walkthrough scripts, copy-ready sample records, and evaluator presentation guide.
- **🎓 Viva & Oral Exam Guide:** [docs/VIVA_PREPARATION.md](docs/VIVA_PREPARATION.md) — Syllabus mappings, architecture explanations, and 50+ oral exam Q&A.

---

*NextStep is an academic project developed for educational purposes under the Department of Computer Science & Engineering.*
