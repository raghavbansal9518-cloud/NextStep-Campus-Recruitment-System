# 🧪 NextStep — Comprehensive Testing Guide

> **Project:** NextStep — Campus Recruitment System  
> **Course:** Programming in Java (25CSE0202)  
> **Testing Approach:** Standalone Java SE Regression & Integration Test Suites (Pure JDBC, No External Frameworks)  
> **Test Status:** 212 / 212 Passed (100% Passing)

---

## 1. Overview & Testing Philosophy

NextStep adopts a pure Core Java SE testing approach. Rather than introducing heavy external dependencies like JUnit or Mockito, all test runners are built using standard Java assertions and JDBC connection pipelines. 

This ensures that:
- Any collaborator, evaluator, or teacher can execute the entire test suite on any machine equipped solely with standard JDK 17+ and MySQL Server.
- Tests execute real database transactions against MySQL, verifying real table constraints, unique indexes, cascading rules, and foreign keys.
- Each test suite isolates test data and runs automated cleanup routines in `finally` blocks, leaving seed records and database tables clean.

---

## 2. Test Suites Summary

The project repository includes 5 comprehensive test runners located in the `tests/` directory:

| Test Suite | File Path | Scope & Syllabus Focus | Tests | Status |
| :--- | :--- | :--- | :---: | :---: |
| **Console Verification** | `tests/ConsoleInputEdgeCaseTestRunner.java` | Pages 1–11 console UI, 24-bit TrueColour ANSI, navigation shortcuts (`B`/`ESC`, `N`/`TAB`, `Enter`), Page 8 company selection from MySQL, edge-case handling | **72** | **PASS** |
| **Phase 3 Test Suite** | `tests/Phase3TestRunner.java` | Student self-registration, validation rules, PBKDF2 hashing with salt, `StudentDAO`, `CompanyDAO`, `JobDAO` operations | **28** | **PASS** |
| **Phase 4 Test Suite** | `tests/Phase4TestRunner.java` | Job browsing, eligibility criteria evaluation, job application lifecycle, duplicate prevention, officer authorization checks | **27** | **PASS** |
| **Phase 5 Test Suite** | `tests/Phase5TestRunner.java` | Full CA-3 syllabus, Java Streams/Lambdas, RFC-4180 CSV export and import audit archiving, presentation toggles | **54** | **PASS** |
| **Phase 6 Test Suite** | `tests/Phase6IntegrationTestRunner.java` | Seed data idempotency, end-to-end student & officer workflows, ACID rollback verification (`conn.rollback()`), simulated restart persistence | **31** | **PASS** |
| **Grand Total** | **All 5 Test Suites** | **Complete System Verification** | **212** | **100% PASS** |

---

## 3. Database Requirements for Testing

Before running test suites:
1. Ensure your local MySQL service is running (`net start MySQL80` on Windows).
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

---

### Step 2: Run Individual Test Suites

#### 1. Console Input & UI Verification Suite (72 tests)
Verifies all 11 console pages, TrueColour rendering, inline shortcut navigation, and Page 8 live company selection:
```powershell
java -Dnextstep.officer.activation.code=CAMPUS_OFFICER_2026 -cp "bin;lib/*" ConsoleInputEdgeCaseTestRunner
```

#### 2. Phase 3 Test Suite (28 tests)
Verifies authentication, PBKDF2 encryption, and core DAOs:
```powershell
java -cp "bin;lib/*" Phase3TestRunner
```

#### 3. Phase 4 Test Suite (27 tests)
Verifies recruitment workflows, eligibility checks, and application states:
```powershell
java -cp "bin;lib/*" Phase4TestRunner
```

#### 4. Phase 5 Syllabus Test Suite (54 tests)
Verifies Java Streams, Collectors, and RFC-4180 CSV I/O:
```powershell
java -cp "bin;lib/*" Phase5TestRunner
```

#### 5. Phase 6 Integration & Stability Suite (31 tests)
Verifies database idempotency, atomic rollbacks, and simulated restart persistence:
```powershell
java -cp "bin;lib/*" Phase6IntegrationTestRunner
```

---

### Step 3: Run All Tests Sequentially (One-Liner)

#### In Windows PowerShell:
```powershell
java -cp "bin;lib/*" Phase3TestRunner; java -cp "bin;lib/*" Phase4TestRunner; java -cp "bin;lib/*" Phase5TestRunner; java -cp "bin;lib/*" Phase6IntegrationTestRunner; java -Dnextstep.officer.activation.code=CAMPUS_OFFICER_2026 -cp "bin;lib/*" ConsoleInputEdgeCaseTestRunner
```

---

## 5. Database State Inspection Utility

The repository includes a standalone database verification tool: `tests/InspectDatabase.java`.

Run this utility to check current table counts, sample partner companies, jobs, unique constraints, and foreign key integrity:
```powershell
java -cp "bin;lib/*" InspectDatabase
```

### Expected Clean State Output:
```text
=== 1. TABLE COUNTS ===
Table users: 2 rows
Table students: 1 rows
Table student_skills: 0 rows
Table companies: 3 rows
Table jobs: 3 rows
Table job_eligible_branches: 8 rows
Table job_required_skills: 10 rows
Table applications: 0 rows

=== 2. SAMPLE COMPANIES ===
ID: 1 | Nexis Cloud Technologies (Cloud & DevOps) - Bengaluru, KA
ID: 2 | Apex Financial Software (FinTech & Banking) - Mumbai, MH
ID: 3 | Quantum Robotics Labs (Embedded Systems & AI) - Hyderabad, TS
```

---

## 6. Known Limitations & Edge Cases Handled

1. **Terminal Line Buffering:** Interactive password masking in standard Java SE uses `System.console().readPassword()`. In headless/automated test runners, tests provide piped input streams where `readPasswordField` cleanly reads credentials without leaking passwords to output streams.
2. **TrueColour ANSI Detection:** Tests verify that when `-Dnextstep.ansi=false` or non-interactive pipes are detected, ANSI escape sequences are completely stripped from the output stream.
3. **Rollback Resilience:** If an unhandled exception or crash occurs during multi-table student creation, JDBC `conn.rollback()` guarantees zero orphaned user accounts.
