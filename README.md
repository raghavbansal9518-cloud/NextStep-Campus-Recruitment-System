# NextStep — Campus Recruitment System

[![Java Version](https://img.shields.io/badge/Java-17%20%7C%2021%20LTS-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Database](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![JDBC Driver](https://img.shields.io/badge/MySQL%20Connector%2FJ-8.4.0-blue)](https://dev.mysql.com/downloads/connector/j/)
[![Build Tool](https://img.shields.io/badge/Build-Native%20JDK%20%2F%20run.bat-success)](run.bat)
[![Tests](https://img.shields.io/badge/Tests-212%20Passed%20(100%25)-brightgreen)](docs/TESTING.md)
[![Course](https://img.shields.io/badge/Course-Programming%20in%20Java%20(25CSE0202)-blueviolet)](docs/VIVA_PREPARATION.md)

> A console-based campus placement automation system built with pure Java SE, JDBC, and MySQL.  
> Developed as part of the Semester 3 **Programming in Java (25CSE0202)** curriculum (Continuous Assessment CA-3).

---

## 📚 Documentation Quick Links

| Document | Purpose |
| :--- | :--- |
| 📘 [**Demo & Operational User Manual**](docs/DEMO_USER_MANUAL.md) | Step-by-step presentation guide, copy-ready demo data, Pages 1–11 walkthroughs, and troubleshooting. |
| 🧪 [**Testing & Verification Guide**](docs/TESTING.md) | Complete documentation of all 5 regression test suites, execution commands, and database isolation. |
| 🎓 [**Viva Preparation Guide**](docs/VIVA_PREPARATION.md) | Syllabus mappings, architecture explanations, database schemas, and 50+ oral exam Q&A. |
| 🤝 [**Contributing Guide**](CONTRIBUTING.md) | Guidelines for collaborators, Git branching workflow, and local database isolation rules. |

---

## 1. Project Overview & Purpose

**NextStep** is an interactive, role-based terminal application designed to streamline campus recruitment drives for students, placement cells, and hiring partner companies.

In traditional university placement drives, candidate management frequently encounters manual eligibility checking across spreadsheets, paper resumes, communication bottlenecks, and duplicate applications. **NextStep** addresses these challenges by centralizing academic records, automated eligibility rules, skill-matching algorithms, and administrative oversight into a unified console interface powered by a relational MySQL backend.

The project strictly emphasizes Core Java SE principles, JDBC transaction management, Object-Oriented design patterns, modern Java Streams/Lambdas, and cryptographic security—built without heavy third-party web frameworks so that every component remains transparent, demonstrable, and defensible in academic evaluations.

---

## 2. Key Implemented Features

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
| **Language** | Java SE | Java 17 LTS / Java 21 LTS (Compatible with JDK 17 through JDK 26) |
| **Database** | MySQL Server | Version 8.0+ (InnoDB Storage Engine, UTF-8 Charset) |
| **JDBC Driver** | MySQL Connector/J | Version 8.4.0 (`lib/mysql-connector-j-8.4.0.jar`) |
| **Security** | PBKDF2 with HMAC-SHA256 | 16-byte random salt, 65,536 iterations, 256-bit key length |
| **Architecture** | Layered Console Architecture | Model – DAO – Service – Presentation |
| **Build & Launch** | Native JDK CLI + Batch | Windows `run.bat` launcher; native `javac` and `java` commands |

---

## 4. Project Structure

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
│   ├── TESTING.md                        # Comprehensive test suites documentation
│   └── VIVA_PREPARATION.md               # Oral examination preparation guide
├── lib/
│   └── mysql-connector-j-8.4.0.jar      # Official MySQL JDBC Driver
├── reports/                              # Output directory for generated CSV reports (Git-ignored)
├── src/
│   └── com/
│       └── nextstep/
│           ├── model/                    # Domain POJOs (Role, User, Student, Company, Job, Application)
│           ├── database/                 # DatabaseConnection (centralized JDBC manager)
│           ├── dao/                      # Data Access Objects (UserDAO, StudentDAO, CompanyDAO, JobDAO, ApplicationDAO)
│           ├── service/                  # Business logic (AuthService, RecruitmentService)
│           ├── util/                     # Utilities (ConsoleTheme, PresentationConfig, Result, ValidationUtil)
│           └── Main.java                 # Interactive console application entry point
├── tests/                                # Standalone regression & integration test suites
│   ├── ConsoleInputEdgeCaseTestRunner.java
│   ├── Phase3TestRunner.java
│   ├── Phase4TestRunner.java
│   ├── Phase5TestRunner.java
│   ├── Phase6IntegrationTestRunner.java
│   └── InspectDatabase.java
├── .gitignore                            # Excludes credentials, compiled bytecode, reports
├── CONTRIBUTING.md                       # Guidelines for collaborators and contributors
├── run.bat                               # One-click Windows build and launch script
└── README.md                             # Main project overview and technical documentation
```

---

## 5. System Requirements

- **Operating System:** Windows 10/11, Linux (Ubuntu/Debian/Fedora), or macOS.
- **Java Development Kit (JDK):** Version 17 LTS, 21 LTS, or newer.  
  - Verify: `java -version` and `javac -version`
- **MySQL Server:** Community Server 8.0 or newer.  
  - Verify service: `net start MySQL80` (Windows) or `systemctl status mysql` (Linux).
- **Terminal:** Windows Terminal, PowerShell, Command Prompt, or Unix bash/zsh with ANSI color support (minimum recommended: 80 columns × 25 lines).

---

## 6. Installation & Database Setup

### Step 1: Clone Repository
```bash
git clone https://github.com/raghavbansal9518-cloud/NextStep-Campus-Recruitment-System.git
cd NextStep-Campus-Recruitment-System
```

### Step 2: Initialize Database Schema & Seed Data
Execute the provided idempotent SQL scripts in order using MySQL CLI:
```bash
mysql -u root -p < database/schema.sql
mysql -u root -p < database/sample_data.sql
```
*Both scripts are idempotent; re-running them will safely update or maintain existing seed records without creating duplicates.*

### Step 3: Configure Local Database Credentials
Copy the configuration template:
```cmd
copy config\db.properties.example config\db.properties
```
Open `config/db.properties` in a text editor and enter your local MySQL root password:
```properties
db.url=jdbc:mysql://localhost:3306/nextstep_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=YOUR_LOCAL_MYSQL_PASSWORD
```
> [!IMPORTANT]
> `config/db.properties` is registered in `.gitignore` and is never committed to GitHub. Each collaborator maintains their own private credentials.

---

## 7. Running the Application

### Option A: One-Click Windows Launcher (`run.bat`) — Recommended
Double-click **`run.bat`** in File Explorer or run it from your terminal:
```cmd
run.bat
```
The script validates your JDK and JDBC driver, checks for `config/db.properties`, compiles all source files into `bin/`, and launches `com.nextstep.Main`.

### Option B: Manual Command-Line Compilation & Execution

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
# 1. Compile
javac -cp ".:lib/*" -d bin $(find src -name "*.java")

# 2. Run
java -cp "bin:lib/*" com.nextstep.Main
```

---

## 8. Placement Officer Activation Code Setup

To protect administrative accounts, Placement Officer registration on Page 5 requires an authorized **Activation Code**.

> [!NOTE]
> - The activation code is an **application-level safeguard**, not a database password.
> - It is **never stored in the database** and **never hardcoded in Java source files**.
> - Each collaborator configures their own local code on their computer.

### Configuring on Windows (One-Time Setup):
Run this command in Command Prompt:
```cmd
setx NEXTSTEP_OFFICER_ACTIVATION_CODE "YOUR_LOCAL_SECRET_CODE"
```
*(Close and reopen your terminal window after running `setx` for the environment variable to take effect).*

### Per-Session JVM Property Alternative:
Pass the activation code directly to the JVM when launching:
```powershell
java -Dnextstep.officer.activation.code=YOUR_LOCAL_SECRET_CODE -cp "bin;lib/*" com.nextstep.Main
```

If neither is configured, officer registration is safely disabled with an administrator notice.

---

## 9. Application Workflow (Pages 1–11)

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

*(Note: Dashboard options marked `[Coming Soon]` are reserved for future CAT-4 milestones and display informative notices).*

---

## 10. Testing Overview

NextStep includes 5 automated regression and integration test suites in `tests/`, covering authentication, database transactions, input validation, Streams/Lambdas, CSV I/O, and console UI:

| Test Suite | Purpose | Tests | Status |
| :--- | :--- | :---: | :---: |
| **`ConsoleInputEdgeCaseTestRunner`** | Pages 1–11 console UI, TrueColour ANSI, `B`/`N`/`ESC`/`TAB` shortcuts, Page 8 company selection | **72** | **PASS** |
| **`Phase3TestRunner`** | Registration, PBKDF2 hashing, StudentDAO, CompanyDAO, JobDAO | **28** | **PASS** |
| **`Phase4TestRunner`** | Job browsing, eligibility rules, application states, officer access control | **27** | **PASS** |
| **`Phase5TestRunner`** | Full CA-3 syllabus, RFC-4180 CSV export/import, presentation visibility controls | **54** | **PASS** |
| **`Phase6IntegrationTestRunner`** | Transaction rollback atomicity, simulated restart persistence, data sanitization | **31** | **PASS** |
| **Grand Total** | **Verified Across All 5 Suites** | **212** | **212 / 212 Passed (100%)** |

To compile and run tests, see the comprehensive guide: 👉 [**docs/TESTING.md**](docs/TESTING.md).

---

## 11. Current Project Status & Future Scope

- **Implemented CA-3 Scope:** Pages 1 through 11 fully implemented with MySQL persistence, PBKDF2 password security, TrueColour ANSI theme, navigation shortcuts, eligibility checking, skill matching, and application tracking.
- **Future Scope (CAT-4 Milestones):**
  - Graphical User Interface (JavaFX / Swing).
  - Resume upload and PDF document parsing.
  - Automated email dispatch via JavaMail API.
  - Recruiter interview scheduling and candidate rating dashboard.

---

## 12. Academic Notice & License

This project was developed for educational coursework and laboratory evaluation under the **Programming in Java (25CSE0202)** course. It is intended for academic study, review, and demonstration.
