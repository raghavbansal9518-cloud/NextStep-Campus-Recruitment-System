# NextStep — Campus Recruitment System

NextStep is a console-based Java application built to streamline campus placement workflows for students, recruiters, and placement officers. Designed for the Semester 3 **Programming in Java (25CSE0202)** course (Continuous Assessment CA-3), the project emphasizes Core Java principles, JDBC database connectivity, transaction management, Object-Oriented design, modern Java Streams/Lambdas, and secure role-based access control.

---

## 1. Project Introduction & Objectives

Campus recruitment drives often involve fragmented communication, manual eligibility checks, paper resumes, and spreadsheet tracking. **NextStep** addresses this by providing an integrated terminal application that:
- Centralizes student academic records, technical skills, and partner company job postings.
- Automatically verifies academic cutoffs (minimum CGPA, allowed backlogs, degree branch, and required technical skills) before permitting job applications.
- Prevents duplicate applications at the database constraint and application service layers.
- Grants placement administrators oversight to review applicants, update statuses (`APPLIED` → `SHORTLISTED` → `SELECTED` / `REJECTED`), generate metrics, and export/audit CSV recruitment reports.
- Implements core syllabus topics without unnecessary third-party frameworks, keeping the codebase easy to understand and explain during viva examinations.

---

## 2. Features

### Student Portal
- **Self-Service Registration:** Creates user credentials, student profile, and technical skills atomically in a single database transaction with PBKDF2 password encryption.
- **Job Browsing:** View active placement drives sorted by highest CTC (using Lambda comparators) or filtered by company.
- **Detailed Drive Inspection:** Inspect job requirements including minimum CGPA, maximum backlogs, eligible branches, and required technical skills.
- **Automated Eligibility Engine:** Evaluates student academic standing and technical skills against job criteria before application submission.
- **Application Tracking:** Track personal application statuses in real time.
- **Access Restrictions:** Blocked from administrative and placement officer operations.

### Placement Officer Portal
- **First-Run Bootstrap:** Automatic prompt to initialize an administrative officer account on a clean database install.
- **Centralized Applicant Oversight:** View all student applications across companies with roll numbers, names, and current statuses.
- **Status Lifecycle Updates:** Move applicants between `APPLIED`, `SHORTLISTED`, `SELECTED`, and `REJECTED`.
- **Recruitment Analytics:** View real-time placement statistics powered by Java Streams and Collectors (`groupingBy`, `partitioningBy`, `counting`).
- **CSV Report Export:** Export complete applicant data to an RFC-4180 compliant CSV file using try-with-resources.
- **CSV Ingestion & Audit Archiving:** Parse exported CSV files into `ArrayList<Application>`, search records, update candidate statuses in-memory, and generate a verified audit CSV file.

### Presentation Controls
- **Live Demo Visibility Toggles:** [`PresentationConfig.java`](file:///c:/Semister-3/Programming%20in%20Java/JavaProjects/NextStep-Campus-Recruitment-System/src/com/nextstep/util/PresentationConfig.java) allows toggling menu items during viva presentations without modifying database records.

---

## 3. Technologies Used

- **Language:** Java 21+ (Compatible with JDK 21 LTS through JDK 26)
- **Database:** MySQL Server 8.0 (InnoDB Storage Engine, UTF-8 Charset)
- **Database Driver:** MySQL Connector/J 8.4.0 (`mysql-connector-j-8.4.0.jar`)
- **Architecture:** Layered Console Architecture (Model – DAO – Service – Presentation)
- **Security:** PBKDF2 with HMAC-SHA256 and 16-byte random salt
- **Build Tool:** Native JDK command-line tools (`javac`, `java`) — no Maven or Gradle dependencies required

---

## 4. Project Folder Structure

```text
NextStep-Campus-Recruitment-System/
├── config/
│   ├── db.properties.example             # Git-tracked configuration template
│   └── db.properties                     # Local database credentials (Git-ignored)
├── database/
│   ├── schema.sql                        # DDL: 8 normalized relational tables & indexes
│   └── sample_data.sql                   # DML: Idempotent seed data (companies, jobs, skills)
├── docs/
│   └── VIVA_PREPARATION.md               # Comprehensive viva and oral examination guide
├── lib/
│   └── mysql-connector-j-8.4.0.jar      # Official MySQL JDBC Driver
├── reports/                              # Default directory for generated CSV reports
├── src/
│   └── com/
│       └── nextstep/
│           ├── model/                    # Domain POJOs and Enums
│           │   ├── Role.java             # User roles: STUDENT, PLACEMENT_OFFICER
│           │   ├── ApplicationStatus.java# APPLIED, SHORTLISTED, SELECTED, REJECTED
│           │   ├── User.java             # Base user account entity
│           │   ├── Student.java          # Student academic profile entity
│           │   ├── Company.java          # Hiring partner entity
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
│           ├── service/                  # Business logic and operations
│           │   ├── AuthService.java      # Registration, PBKDF2 hashing, login
│           │   └── RecruitmentService.java# Eligibility, lifecycle, streams, CSV I/O
│           ├── util/                     # Generic utilities and configurations
│           │   ├── PresentationConfig.java# Viva demo menu visibility toggles
│           │   ├── Result.java           # Generic Result<T> container
│           │   └── ValidationUtil.java   # Input sanitization and generic paginate<T>
│           └── Main.java                 # Interactive console application entry point
├── .gitignore                            # Excludes credentials, compiled bytecode, reports
└── README.md                             # Project documentation
```

---

## 5. Software Prerequisites

1. **Java Development Kit (JDK):** Version 21 LTS or newer.
   - Verify by running: `java -version` and `javac -version`
2. **MySQL Server:** Version 8.0 or newer running locally on default port `3306`.
   - Verify service is active: `net start MySQL80` (Windows PowerShell)

---

## 6. MySQL Database Setup Instructions

1. **Log in to MySQL:**
   ```bash
   mysql -u root -p
   ```

2. **Initialize Schema and Seed Data:**
   Run the SQL scripts in order using MySQL CLI:
   ```bash
   mysql -u root -p < database/schema.sql
   mysql -u root -p < database/sample_data.sql
   ```
   *Note: Both scripts are completely idempotent. Running them multiple times will not create duplicate entries.*

3. **Verify Tables in MySQL:**
   ```sql
   USE nextstep_db;
   SHOW TABLES;
   ```
   You should see 8 normalized tables:
   - `users`
   - `students`
   - `student_skills`
   - `companies`
   - `jobs`
   - `job_eligible_branches`
   - `job_required_skills`
   - `applications`

---

## 7. JDBC Connector/J Configuration

1. Create your local credentials file by copying the template:
   ```powershell
   Copy-Item config/db.properties.example config/db.properties
   ```

2. Edit `config/db.properties` with your local MySQL credentials:
   ```properties
   db.url=jdbc:mysql://localhost:3306/nextstep_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   db.user=root
   db.password=YOUR_MYSQL_PASSWORD_HERE
   ```
   > [!IMPORTANT]
   > `config/db.properties` is registered in `.gitignore`. Never commit your actual database password to GitHub or any public repository.

3. The official JDBC driver `mysql-connector-j-8.4.0.jar` is already included in the `lib/` directory.

---

## 8. How to Compile and Run the Project

### Fast Launch via Windows Launcher (`run.bat`):

For the easiest experience on Windows without typing manual classpath and compilation commands:

1. **One-Time Configuration:**
   - Ensure MySQL is running and `config/db.properties` contains your database password.
   - Configure the placement officer activation code once in Windows (see Section 8.1 below).
2. **Launch:**
   - Double-click **`run.bat`** in the project root directory (or run `.\run.bat` from PowerShell/CMD).
   - The script automatically:
     - Verifies Java JDK and MySQL Connector/J driver.
     - Confirms database configuration file exists.
     - Compiles all Java source files into `bin/`.
     - Launches `com.nextstep.Main` with the correct classpath.
     - Keeps the terminal window open if an error occurs so you can read diagnostic details.
3. **Safe Application Exit:**
   - Enter `0` at any screen or menu (Option 0: Exit / Sign Out / Back).
   - Alternatively, press `Ctrl+C` in the terminal to terminate safely.

### Manual Command-Line Compilation & Execution:

#### On Windows (PowerShell):

1. **Compile all Java source files into `bin/`:**
   ```powershell
   javac -cp ".;lib/*" -d bin src/com/nextstep/model/*.java src/com/nextstep/database/*.java src/com/nextstep/dao/*.java src/com/nextstep/service/*.java src/com/nextstep/util/*.java src/com/nextstep/Main.java
   ```

2. **Test Database Connectivity (Optional Verification):**
   ```powershell
   java -cp "bin;lib/*" com.nextstep.database.DatabaseConnection
   ```

3. **Run the Application:**
   ```powershell
   java -cp "bin;lib/*" com.nextstep.Main
   ```

#### On Linux / macOS (Bash):

1. **Compile:**
   ```bash
   javac -cp ".:lib/*" -d bin src/com/nextstep/model/*.java src/com/nextstep/database/*.java src/com/nextstep/dao/*.java src/com/nextstep/service/*.java src/com/nextstep/util/*.java src/com/nextstep/Main.java
   ```

2. **Run:**
   ```bash
   java -cp "bin:lib/*" com.nextstep.Main
   ```

---

## 8.1 Placement Officer Activation Code Setup

To protect administrative accounts, NextStep requires an authorized activation code for Placement Officer self-registration (Page 5). This code is never hardcoded in source code and can be configured persistently or per-session:

### 1. One-Time Persistent Windows Setup (Recommended)
Set the user environment variable once so it remains permanently available across all future terminal sessions and `run.bat` launches:

- **Via PowerShell / Command Prompt:**
  ```cmd
  setx NEXTSTEP_OFFICER_ACTIVATION_CODE "CAMPUS_OFFICER_2026"
  ```
  *(Restart any open terminal windows after running `setx` for the new environment variable to take effect).*

- **Via Windows GUI:**
  1. Press `Win + R`, type `sysdm.cpl`, and press **Enter**.
  2. Select the **Advanced** tab and click **Environment Variables...**.
  3. Under **User variables**, click **New...**.
  4. Variable name: `NEXTSTEP_OFFICER_ACTIVATION_CODE`
  5. Variable value: `CAMPUS_OFFICER_2026`
  6. Click **OK** to save.

### 2. Per-Session System Property
Alternatively, supply the activation code directly to the JVM at launch:
```powershell
java -Dnextstep.officer.activation.code=CAMPUS_OFFICER_2026 -cp "bin;lib/*" com.nextstep.Main
```

If no activation code is configured, officer registration is safely disabled with a clear administrator configuration notice.

---

## 9. How First-Time Placement Officer Provisioning Works

Because NextStep enforces strict separation between students and administrators, normal students cannot register as placement officers through the standard public menu.

NextStep provides two clean, automated ways to provision the initial placement administrator:

1. **Interactive Bootstrap Prompt (Default):**
   When `Main` starts, `checkFirstRunOfficerSetup()` checks if any user with the `PLACEMENT_OFFICER` role exists in the database. If none is found, it automatically displays:
   ```text
   [SYSTEM NOTICE] No Placement Officer account found in database.
   Would you like to initialize an administrative Placement Officer account now? (y/n): 
   ```
   Typing `y` prompts for username, password, and official email, hashing the password with PBKDF2 and creating the administrator record immediately.

2. **Direct CLI Flag:**
   Administrators can seed an officer account directly using the CLI flag:
   ```powershell
   java -cp "bin;lib/*" com.nextstep.Main --init-officer
   ```

---

## 10. Sample Demonstration Workflow

A typical evaluator presentation workflow in 5 simple steps:

1. **First-Run Officer Setup:**
   - Launch application: `java -cp "bin;lib/*" com.nextstep.Main`.
   - Accept prompt to create officer account (e.g. `admin_officer` / `Admin@123` / `placement@college.edu`).

2. **Student Self-Registration:**
   - Select `1. Student Registration`.
   - Register a student (e.g. `rohit_kumar`, `Pass@123`, `24BCE1001`, `Computer Science`, CGPA: `8.5`, Backlogs: `0`, Skills: `Java, SQL, Spring Boot, Git, OOP`).

3. **Student Job Application & Eligibility Check:**
   - Select `2. Login` with student credentials.
   - Select `1. View Available Jobs` (observe drives sorted by highest CTC using Java Lambdas).
   - Select `3. Check Eligibility for a Job` (Job ID 1) — verify academic cutoff satisfaction and matched skills.
   - Select `4. Apply for a Job` (Job ID 1) — verify application submission.
   - Attempt to apply for Job ID 1 again — observe duplicate application rejection.
   - Select `5. View My Applications` — observe application status: `APPLIED`.
   - Select `6. Logout`.

4. **Placement Officer Oversight & Status Update:**
   - Select `2. Login` with officer credentials (`admin_officer`).
   - Select `2. View All Student Applications` — view student applicant with roll number, name, and job.
   - Select `3. Update Application Status` — advance Application #1 to `SHORTLISTED`.
   - Select `4. View Recruitment Summary` — view status breakdown via `Collectors.groupingBy()` and placement partition via `Collectors.partitioningBy()`.

5. **CSV Export & Audit Archival:**
   - Select `5. Export Applications Report to CSV` — export report to `reports/applications_report.csv`.
   - Select `6. Process & Archive Applications CSV` — read CSV with try-with-resources, parse into `ArrayList<Application>`, update status to `SELECTED`, and write verified audit record to `reports/applications_audited.csv`.
   - Select `7. Logout` followed by `3. Exit`.

---

## 11. Important Implementation Concepts

| Concept | Course Handout Mapping | Implementation in NextStep |
| :--- | :--- | :--- |
| **Object-Oriented Programming** | Lectures 1–18 | Encapsulated POJOs ([`Student`](file:///c:/Semister-3/Programming%20in%20Java/JavaProjects/NextStep-Campus-Recruitment-System/src/com/nextstep/model/Student.java), [`Job`](file:///c:/Semister-3/Programming%20in%20Java/JavaProjects/NextStep-Campus-Recruitment-System/src/com/nextstep/model/Job.java), [`Application`](file:///c:/Semister-3/Programming%20in%20Java/JavaProjects/NextStep-Campus-Recruitment-System/src/com/nextstep/model/Application.java)), Type-safe enums ([`Role`](file:///c:/Semister-3/Programming%20in%20Java/JavaProjects/NextStep-Campus-Recruitment-System/src/com/nextstep/model/Role.java), [`ApplicationStatus`](file:///c:/Semister-3/Programming%20in%20Java/JavaProjects/NextStep-Campus-Recruitment-System/src/com/nextstep/model/ApplicationStatus.java)). |
| **JDBC & SQL Protection** | Lectures 35–42 | Parameterized queries with `PreparedStatement` preventing SQL injection; `Statement.RETURN_GENERATED_KEYS`. |
| **ACID Transactions** | Lectures 37–38 | `conn.setAutoCommit(false)`, `conn.commit()`, and `conn.rollback()` for atomic student registration across 3 tables. |
| **Lambda Expressions** | Lectures 43–44 | Custom `Comparator` lambda sorting open jobs by package CTC in [`RecruitmentService.getJobsSortedByCtc`](file:///c:/Semister-3/Programming%20in%20Java/JavaProjects/NextStep-Campus-Recruitment-System/src/com/nextstep/service/RecruitmentService.java#L297-L306). |
| **Stream API Pipelines** | Lectures 45–46 | `.filter()`, `.map()`, `.distinct()`, `.sorted()`, `.count()`, `.anyMatch()` in [`RecruitmentService`](file:///c:/Semister-3/Programming%20in%20Java/JavaProjects/NextStep-Campus-Recruitment-System/src/com/nextstep/service/RecruitmentService.java). |
| **Collectors API** | Lectures 45–46 | `Collectors.groupingBy()` and `Collectors.partitioningBy()` with `Collectors.counting()` for recruitment summaries. |
| **Java Generics** | Lectures 47–48 | Generic class [`Result<T>`](file:///c:/Semister-3/Programming%20in%20Java/JavaProjects/NextStep-Campus-Recruitment-System/src/com/nextstep/util/Result.java) and generic slicing method [`ValidationUtil.paginate(List<T>, int, int)`](file:///c:/Semister-3/Programming%20in%20Java/JavaProjects/NextStep-Campus-Recruitment-System/src/com/nextstep/util/ValidationUtil.java#L93-L103). |
| **File I/O & CSV Processing** | Lectures 49–54 | `BufferedReader`, `BufferedWriter`, `FileReader`, `FileWriter` with try-with-resources and RFC-4180 quotation escaping. |
| **Defensive Exception Handling** | Lectures 25–28 | Checked `SQLException`/`IOException` propagation alongside custom validation via `IllegalArgumentException` and `SecurityException`. |

---

## 12. Viva & Oral Exam Guide

For detailed explanations of every class, architecture diagrams, database schemas, and common viva questions with answers, see [**docs/VIVA_PREPARATION.md**](file:///c:/Semister-3/Programming%20in%20Java/JavaProjects/NextStep-Campus-Recruitment-System/docs/VIVA_PREPARATION.md).
