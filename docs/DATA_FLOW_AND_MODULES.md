# NextStep — Data Lifecycles, Application Modules & Page Directory

---

## 1. Complete End-to-End Data Lifecycles

This section details how data travels from user keystrokes in the console to the physical MySQL tables on disk, and back to the screen.

---

### Lifecycle 1: Student Registration & Profile Creation

```
[Student Terminal Input]
       |
       v
1. Main.java (readConsoleLine, reads 12 fields: USN, Name, Email, Phone, Branch, CGPA, Skills, Backlogs, Year, User, Pass)
       |
       v
2. ValidationUtil.java (Validates non-empty, email format, phone >= 10 digits, CGPA between 0.0-10.0, Backlogs >= 0)
       |
       v
3. AuthService.java (Executes hashPassword() -> Generates 16-byte salt, computes PBKDF2WithHmacSHA256, 10,000 iterations)
       |
       v
4. JDBC Transaction Initiated (conn.setAutoCommit(false))
       |
       +---> UserDAO.createUser(conn, user)        ---> INSERT INTO users (username, password_hash, role, email)
       |
       +---> StudentDAO.createStudent(conn, std)   ---> INSERT INTO students (user_id, student_id, full_name, ...)
       |
       +---> StudentDAO.saveStudentSkills(conn, ..)---> INSERT INTO student_skills (student_id, skill_name)
       |
       v
5. conn.commit() executed (All 3 tables updated atomically; if any fails, conn.rollback() is triggered)
       |
       v
6. Main.java renders account creation confirmation banner -> navigates to Student Sign In.
```

---

### Lifecycle 2: Student Authentication (Login)

1. **Input:** Student enters Username (or USN) and Password.
2. **Service Delegation:** `Main.java` passes credentials to `AuthService.authenticate(identifier, password)`.
3. **Database Query:** `UserDAO.findByUsername(identifier)` (or `StudentDAO.findByStudentId(identifier)`) executes:
   ```sql
   SELECT id, username, password_hash, role, email FROM users WHERE username = ?;
   ```
4. **PBKDF2 Verification:**
   - Extracts the 16-byte salt and iteration count stored inside `password_hash` (`PBKDF2$10000$<saltHex>$<hashHex>`).
   - Re-hashes the candidate password using the extracted salt and 10,000 iterations.
   - Compares the resulting byte array with the stored hash using `MessageDigest.isEqual` (constant-time comparison).
5. **Session Established:** If hashes match and role is `STUDENT`, the authenticated `User` and linked `Student` profile are loaded into memory and the Student Placement Hub opens.

---

### Lifecycle 3: Dual-Phase Automated Eligibility & Skill Matching

When a student selects a job drive (e.g., Job #1: Junior Backend Engineer at Nexis Cloud Technologies), `RecruitmentService.evaluateEligibility(student, jobId)` executes in two sequential phases:

```
                          [ Candidate Profile & Selected Job ]
                                           |
                                           v
                  +------------------------------------------------+
                  |  PHASE 1: Deterministic Academic Verification   |
                  +------------------------------------------------+
                  |  1. CGPA Cutoff:       Student CGPA >= Job Min  |
                  |  2. Backlogs Limit:    Backlogs <= Job Max     |
                  |  3. Branch Match:      Branch IN EligibleList   |
                  |  4. Batch Year:        GradYear == Job GradYear |
                  +------------------------+-----------------------+
                                           |
                                           v
                  +------------------------------------------------+
                  |  PHASE 2: Set-Theoretic Skill Matrix Intersect |
                  +------------------------------------------------+
                  |  Job Required Skills:   {Java, SQL, OOP, Git}  |
                  |  Student Skills:        {Java, SQL}            |
                  |  Matched:               {Java, SQL} (2 / 4)    |
                  |  Missing:               {OOP, Git}             |
                  |  Match Score:           50%                    |
                  +------------------------+-----------------------+
                                           |
                                           v
                    [ Final EligibilityResult Payload Returned ]
```

- **Academic Checklist:** Renders a 64-column framed table showing the criteria, job cutoff requirement, candidate actual status, and `PASS`/`FAIL` indicator.
- **Skill Breakdown:** Displays matched skills, missing skills, and overall match percentage.
- **Safe Submission Guard:** If the candidate fails academic criteria OR misses mandatory technical skills, the overall status is marked `NOT ELIGIBLE`, and submission is blocked.

---

### Lifecycle 4: Application Submission & Digital Receipt

1. **Eligibility Enforcement:** `RecruitmentService.applyForJob(student, jobId)` re-evaluates eligibility server-side.
2. **Duplicate Application Guard:** Queries `ApplicationDAO.hasApplied(student.getId(), jobId)`:
   ```sql
   SELECT 1 FROM applications WHERE student_id = ? AND job_id = ? LIMIT 1;
   ```
   If a record exists, throws an `IllegalStateException("Candidate has already submitted an application for this drive.")`.
3. **Database Insertion:** `ApplicationDAO.createApplication(app)` executes:
   ```sql
   INSERT INTO applications (student_id, job_id, status) VALUES (?, ?, 'APPLIED');
   ```
4. **Receipt Generation:** Returns the generated `id` (e.g., `201`). `Main.java` formats the digital receipt (`APP-2026-201`), showing student USN, company name, CTC, location, and filing timestamp.

---

### Lifecycle 5: Placement Officer Review & Status Advancement

1. **Querying Applications:** `RecruitmentService.getAllApplications(officer)` verifies that `officer.getRole() == Role.PLACEMENT_OFFICER`. If an unauthenticated user or student calls this method, a `SecurityException` is thrown.
2. **Relational Join Query:** `ApplicationDAO.findAll()` executes:
   ```sql
   SELECT a.id, a.student_id, a.job_id, a.status, a.applied_at,
          s.student_id AS student_roll, s.full_name, s.branch, s.cgpa,
          j.title AS job_title, c.name AS company_name
   FROM applications a
   JOIN students s ON a.student_id = s.id
   JOIN jobs j ON a.job_id = j.id
   JOIN companies c ON j.company_id = c.id
   ORDER BY a.applied_at DESC;
   ```
3. **Status Update:** When the officer selects an application and updates its status to `SHORTLISTED`:
   ```sql
   UPDATE applications SET status = ? WHERE id = ?;
   ```
   The record updates immediately in MySQL and reflects instantly in the student's dashboard.

---

## 2. All 10 Principal Application Pages

NextStep features 10 standardized console screens, each maintaining a **strict 64-column boxed layout** with 24-bit TrueColour styling:

| Page # | Page Title / Component | Target User | Key Information Displayed | Primary User Actions | Classes Involved |
|:---:|---|---|---|---|---|
| **1** | **Welcome & Unified Campus Gateway** | Public / All | System online status, active drives count, registered candidates count, portal choices. | Select Student Portal (1), Officer Portal (2), Stats (3), Demo Mode (4), Exit (0). | [`Main.java`](file:///c:/Semister-3/Programming%20in%20Java/JavaProjects/NextStep-Campus-Recruitment-System/src/com/nextstep/Main.java) |
| **2** | **Student Sign Up / Registration** | New Students | 12 interactive registration fields, upfront demo mode choice (`A` shortcut vs. manual). | Enter credentials, press `A` for autofill, press `B`/`N` to navigate fields, submit (1). | `Main`, `AuthService`, `UserDAO`, `StudentDAO` |
| **3** | **Student Sign In** | Students | Username/USN prompt, password prompt, demo shortcut hint (`A1`, `A2`, `A3`). | Enter credentials or press `A` to load demo profile, authenticate (1). | `Main`, `AuthService`, `UserDAO`, `StudentDAO` |
| **4** | **Placement Officer Sign Up** | New Officers | Onboarding form: Officer ID, name, email, department, username, password, activation code. | Enter details, supply valid institutional activation code, submit. | `Main`, `AuthService`, `UserDAO` |
| **5** | **Placement Officer Sign In** | Officers | Username and password prompt with demo shortcut hint (`officer_vihaan`). | Authenticate as placement officer, unlock admin tools. | `Main`, `AuthService`, `UserDAO` |
| **6** | **Student Placement Hub Dashboard** | Students | Welcome banner, candidate USN, CGPA, backlogs, branch, active drive count. | View Open Drives (1), Check Eligibility & Skills (2), Apply (3), My Applications (4), Sign Out (0). | `Main`, `RecruitmentService`, `JobDAO` |
| **7** | **Placement Officer Admin Dashboard** | Officers | Officer ID, open drives count, total applicants count, shortlisted candidate tally. | Post New Job (1), Manage Applications (2), Export CSV (3), Demo Controls (4), Sign Out (0). | `Main`, `RecruitmentService`, `ApplicationDAO` |
| **8** | **Unified Eligibility & Skill Match Matrix** | Students | Academic checklist table (CGPA, backlogs, branch, year), skill table with match % and missing skills. | Review criteria breakdown, proceed to apply (1), return to dashboard (0). | `Main`, `RecruitmentService`, `JobDAO` |
| **9** | **Create Job Drive Portal** | Officers | Active partner companies list, job title, description, CTC LPA, min CGPA, branches, skills. | Enter job specs, press `A` for demo autofill, publish drive to MySQL. | `Main`, `JobDAO`, `CompanyDAO` |
| **10**| **Master Application Registry & Receipt** | Officers / Students | Table of all applicants: App ID, USN, candidate name, status badge, drive title, CSV export action. | Shortlist candidate (1), Reject candidate (2), Export CSV (3), Return (0). | `Main`, `RecruitmentService`, `ApplicationDAO` |

---

## 3. Auxiliary Subsystems & Workflows

### 1. Centralized Demonstration Mode (Option 4)
- **Accessible from:** Welcome Gateway (Option 4) or during form prompts (`A` shortcut).
- **Capabilities:**
  - View credentials and scenarios reference guide.
  - Inspect and edit Scenario A (`vihaan_demo`), Scenario B (`kabir_demo`), and Scenario C (`arjun_demo`).
  - Safe reset: Purges only demonstration accounts while strictly preserving all real student and officer data.

### 2. Campus Recruitment Statistics (Option 3)
- **Accessible from:** Welcome Gateway (Option 3).
- **Displays:** Registered hiring partner companies, total published recruitment drives, overall applicant counts, and average CTC package computed dynamically via MySQL queries.
