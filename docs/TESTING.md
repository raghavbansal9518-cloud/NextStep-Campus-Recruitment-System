# 🧪 NextStep — Comprehensive Testing & Verification Guide

> **Project:** NextStep — Campus Recruitment System  
> **Course:** Programming in Java (25CSE0202) — CA-3  
> **Testing Approach:** Standalone Java SE Regression & Integration Test Suites (Pure JDBC, Zero External Build Frameworks)  
> **Test Status:** **297 / 297 Passed (100% Success Rate across 7 Automated Test Runners)**  
> **Visual Layout Status:** **30 / 30 Inspections Passed (0 Lines Exceeding 64 Columns across All Screens)**

---

## 1. Testing Philosophy & Architecture

NextStep adopts a pure Core Java SE testing philosophy. Rather than introducing heavy external dependencies like JUnit, TestNG, or Mockito, all test runners are built using standard Java assertions and live JDBC connection pipelines. 

This guarantees:
1. **Zero External Tool Friction:** Any evaluator, professor, or student can execute the entire test suite on any machine equipped solely with standard JDK 17+ and MySQL Server without requiring Maven, Gradle, or IDE plugins.
2. **Real Database Transactions:** Tests execute genuine relational queries and transactions against MySQL, verifying real table constraints, unique indexes, foreign key cascading deletes, and rollback atomicity.
3. **Data Isolation & Cleanup:** Each test runner isolates temporary test records using timestamps or distinct prefixes (`test_`, `%_temp`), purging them in automated `finally` blocks to leave permanent seed data pristine.
4. **Visual Layout Verification:** Automated terminal inspection verifies that every rendered console line conforms to the established 64-column boxed frame (`FRAME_WIDTH = 64`) with zero line wraps or border overflows.

---

## 2. Test Suites Inventory

The repository includes 7 automated functional test runners and 1 visual layout verifier located in `tests/`:

| Test Suite Runner | File Path | Scope & Syllabus Focus | Tests | Status |
| :--- | :--- | :--- | :---: | :---: |
| **Centralized Demo Manager** | `tests/CentralizedDemoManagerTestRunner.java` | Centralized demo profiles, MySQL edits, application-wide `A` shortcut on Student/Officer login and Job Drive selection, application tracking | **11** | **PASS** |
| **Demo Mode Verification** | `tests/DemoModeVerificationTestRunner.java` | Repeatable demo initialization, idempotence (0 duplicates), Scenarios A/B/C, deterministic credentials (`NextStep@2026`), safe reset, real account protection, CSV export | **24** | **PASS** |
| **Console Edge-Case Verification** | `tests/ConsoleInputEdgeCaseTestRunner.java` | Pages 1–10 console UI, TrueColour ANSI, shortcuts (`B`/`ESC`, `N`/`TAB`, `Enter`), Page 7 10-field job persistence, one-click demo autofill, collision checks, fail-safe error handling | **89** | **PASS** |
| **Phase 3 Test Suite** | `tests/Phase3TestRunner.java` | Student registration, validation rules, PBKDF2 hashing with 16-byte random salt, `UserDAO.existsByEmail`, `StudentDAO`, `CompanyDAO`, `JobDAO` operations | **41** | **PASS** |
| **Phase 4 Test Suite** | `tests/Phase4TestRunner.java` | Job browsing, 10-field persistence (CTC, location, backlogs), partner company validation, eligibility rules, application lifecycle, duplicate prevention, backward compatibility | **47** | **PASS** |
| **Phase 5 Syllabus Suite** | `tests/Phase5TestRunner.java` | Full CA-3 syllabus, Java Streams, Collectors (`groupingBy`, `partitioningBy`), RFC-4180 CSV export and import audit archiving, presentation toggles | **54** | **PASS** |
| **Phase 6 Integration Suite** | `tests/Phase6IntegrationTestRunner.java` | Seed data idempotency, end-to-end student & officer workflows, ACID rollback verification (`conn.rollback()`), simulated restart persistence | **31** | **PASS** |
| **Total Automated Functional Tests** | **All 7 Test Runners** | **Complete System & Business Logic Verification** | **297** | **100% PASS** |
| **Visual Layout Inspector** | `tests/VisualInspectionVerifier.java` | Visual column measurements ($\le 64$ columns) and responsive dimension verification (50, 64, 80, 100, 120 columns) | **30** | **100% PASS** |

---

## 3. Database Requirements for Testing

Before running test suites:
1. Ensure your local MySQL service is running (`net start MySQL80` on Windows or `systemctl status mysql` on Linux).
2. Ensure `nextstep_db` has been created using `database/schema.sql`.
3. Ensure seed data has been imported using `database/sample_data.sql`.
4. Ensure `config/db.properties` contains your valid local MySQL credentials.

### Test Data Isolation & Cleanup
Each test runner executes within its own try-finally context. Fictional test accounts (e.g., users prefixed with `test_` or timestamped USNs) and temporary job applications created during test execution are purged from MySQL during the cleanup phase. Permanent seed companies (IDs 1, 2, 3) and sample job openings remain preserved.

---

## 4. How to Compile & Run Tests

### Step 1: Compile Application Sources and Tests
From the project root directory, compile all application code and test suites into `bin/`:

#### In Windows PowerShell:
```powershell
javac -d bin -cp "bin;lib/*" src/com/nextstep/model/*.java src/com/nextstep/database/*.java src/com/nextstep/dao/*.java src/com/nextstep/service/*.java src/com/nextstep/util/*.java src/com/nextstep/Main.java tests/*.java
```

#### In Windows Command Prompt (CMD):
```cmd
javac -d bin -cp "lib\mysql-connector-j-8.4.0.jar" src\com\nextstep\model\*.java src\com\nextstep\database\*.java src\com\nextstep\dao\*.java src\com\nextstep\service\*.java src\com\nextstep\util\*.java src\com\nextstep\Main.java tests\*.java
```

#### In Linux / macOS (Bash):
```bash
javac -cp ".:lib/*" -d bin $(find src tests -name "*.java")
```

---

### Step 2: Run Individual Test Suites

#### 1. Centralized Demo Manager Test Suite (11 tests)
Verifies centralized scenario profiles, canonical MySQL persistence, real-time profile editing, application-wide `A` shortcut fetching on Student and Officer login screens, job selection, and application status tracking:
```powershell
java -cp "bin;lib/*" CentralizedDemoManagerTestRunner
```

#### 2. Demo Mode Verification Suite (24 tests)
Verifies repeatable demo initialization, idempotence (zero duplicate accounts), deterministic scenarios (Scenarios A/B/C), authentication via username and USN with `NextStep@2026`, safe demo-only reset with real-account protection, and gateway demo manager integration:
```powershell
java -cp "bin;lib/*" DemoModeVerificationTestRunner
```

#### 3. Console Input & UI Verification Suite (89 tests)
Verifies all 10 approved console pages, TrueColour rendering, inline shortcut navigation, Page 7 10-field job persistence, one-click demo autofill, and collision-safe validation:
```powershell
java "-Dnextstep.officer.activation.code=CAMPUS_OFFICER_2026" -cp "bin;lib/*" ConsoleInputEdgeCaseTestRunner
```

#### 4. Phase 3 Test Suite (41 tests)
Verifies student self-registration, validation rules, PBKDF2 password derivation with 16-byte random salt, `UserDAO.existsByEmail`, `StudentDAO`, `CompanyDAO`, and `JobDAO`:
```powershell
java -cp "bin;lib/*" Phase3TestRunner
```

#### 5. Phase 4 Test Suite (47 tests)
Verifies recruitment workflows, complete 10-field job persistence, partner company validation, eligibility criteria evaluation, job application states, duplicate application prevention, and backward-compatible service overloads:
```powershell
java -cp "bin;lib/*" Phase4TestRunner
```

#### 6. Phase 5 Syllabus Test Suite (54 tests)
Verifies Java Streams, Collectors (`groupingBy`, `partitioningBy`), and RFC-4180 CSV export and import audit archiving:
```powershell
java -cp "bin;lib/*" Phase5TestRunner
```

#### 7. Phase 6 Integration & Stability Suite (31 tests)
Verifies database idempotency, atomic rollbacks (`conn.rollback()`), and simulated restart persistence across distinct DAO instances:
```powershell
java -cp "bin;lib/*" Phase6IntegrationTestRunner
```

#### 8. Visual Layout & Responsiveness Inspector (30 inspections)
Captures, measures visual column widths, and verifies strict 64-column bounds ($\le 64$ columns) across all 10 application pages, menus, forms, error states, and responsive dimensions (50, 64, 80, 100, 120 columns):
```powershell
java -cp "bin;lib/*" VisualInspectionVerifier
```

---

### Step 3: Run All Tests Sequentially (One-Liner)

#### In Windows PowerShell:
```powershell
java -cp "bin;lib/*" CentralizedDemoManagerTestRunner; java -cp "bin;lib/*" DemoModeVerificationTestRunner; java -cp "bin;lib/*" Phase3TestRunner; java -cp "bin;lib/*" Phase4TestRunner; java -cp "bin;lib/*" Phase5TestRunner; java -cp "bin;lib/*" Phase6IntegrationTestRunner; java "-Dnextstep.officer.activation.code=CAMPUS_OFFICER_2026" -cp "bin;lib/*" ConsoleInputEdgeCaseTestRunner; java -cp "bin;lib/*" VisualInspectionVerifier
```

---

## 5. Visual Regression & Terminal Responsiveness Results

The automated layout inspector (`VisualInspectionVerifier`) executes high-resolution simulated terminal executions across 30 distinct application scenarios:

```text
========================================================================================
             VISUAL INSPECTION & TERMINAL RESPONSIVENESS SUMMARY
========================================================================================
No.  Screen Scenario Tested                                Lines   Max Col  Over 64 Col
────────────────────────────────────────────────────────────────────────────────────────
1    Student Sign Up [A-Shortcut Autofill]                  91       64         0
2    Student Sign Up [Manual Form Entry]                   104       64         0
3    Officer Sign Up [A-Shortcut Autofill]                  86       64         0
4    Officer Sign Up [Manual Form Entry]                    98       64         0
5    Student Sign In [A-Shortcut Scenario A]                88       64         0
6    Student Sign In [Manual Credentials Entry]             88       64         0
7    Officer Sign In [A-Shortcut officer_vihaan]            88       64         0
8    Officer Sign In [Manual Credentials Entry]             88       64         0
9    Job Creation [A-Shortcut Nexis Cloud]                  96       64         0
10   Job Creation [Manual Parameters Entry]                102       64         0
11   Centralized Demo Data Manager Gateway Menu             79       64         0
12   Demo Manager - Student Profiles List View              89       64         0
13   Demo Manager - Student Profile 1 Edit Screen          102       64         0
14   Demo Manager - Officer Profile Edit Screen             98       64         0
15   Demo Manager - Job Specifications Edit Screen         101       64         0
16   Demo Manager - Applications Tracking Screen            92       64         0
17   Gateway & Campus Recruitment Statistics                82       64         0
18   Student Placement Hub & Available Drives               88       64         0
19   Page 8 - Eligibility Checklist & Skill Matrix [Pass]   96       64         0
20   Page 8 - Eligibility Checklist & Skill Matrix [Fail]   94       64         0
21   Page 9 - Pre-Submission Application Review             88       64         0
22   Page 9 - Rejection & Ineligibility Block Notice        84       64         0
23   Page 9 - Official Digital Recruitment Filing Receipt   86       64         0
24   Page 6 - Student Personal Application History          84       64         0
25   Page 7 - Placement Officer Administration Dashboard    82       64         0
26   Page 10 - Master Application Registry Table View      118       64         0
────────────────────────────────────────────────────────────────────────────────────────
27   Responsive Centering: 80-Column Terminal (Margin: 8)   --       --       PASS
28   Responsive Centering: 100-Column Terminal (Margin: 18) --       --       PASS
29   Responsive Centering: 120-Column Terminal (Margin: 28) --       --       PASS
30   Responsive Fallback: 50-Column Terminal (Margin: 0)    --       --       PASS
========================================================================================
ALL 30 VISUAL REGRESSION & RESPONSIVENESS INSPECTIONS COMPLETED SUCCESSFULLY!
```

---

## 6. Standalone Database Inspection Utility

The repository includes a database verification tool: `tests/InspectDatabase.java`.

Run this utility to check table counts, partner companies, open jobs, unique constraints, and foreign key integrity:
```powershell
java -cp "bin;lib/*" InspectDatabase
```

### Expected Output:
```text
=== 1. TABLE COUNTS ===
Table users: 4 rows
Table students: 3 rows
Table student_skills: 8 rows
Table companies: 3 rows
Table jobs: 3 rows
Table job_eligible_branches: 8 rows
Table job_required_skills: 10 rows
Table applications: 1 rows

=== 2. SAMPLE COMPANIES ===
ID: 1 | Nexis Cloud Technologies (Cloud & DevOps) - Bengaluru, KA
ID: 2 | Apex Financial Software (FinTech & Banking) - Mumbai, MH
ID: 3 | Quantum Robotics Labs (Embedded Systems & AI) - Hyderabad, TS
```

---

## 7. Edge Cases & Resilience Verifications

1. **Terminal Line Buffering:** Password entry uses `System.console().readPassword()`. In headless/automated test environments, `readPasswordField` cleanly handles piped streams without leaking credentials to output streams.
2. **ANSI Color Detection:** Tests confirm that when running in automated testing pipes or with `-Dnextstep.ansi=false`, ANSI escape codes are stripped cleanly without leaving escape sequence artifacts.
3. **Rollback Resilience:** If an unhandled exception or SQL error occurs during student registration, JDBC `conn.rollback()` guarantees zero partial user accounts.
4. **Collision Resilience:** If username or email collisions occur during demo creation, `DemoService` regenerates identifiers up to 1,000 attempts and handles retry exhaustion safely.
