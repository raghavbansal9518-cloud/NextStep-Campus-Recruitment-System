# NextStep — Complete Testing, Quality Assurance & Verification Report

---

## 1. Testing Philosophy & Architecture

NextStep employs a rigorous, multi-layered automated verification strategy to guarantee that business rules, database transactions, cryptographic functions, and visual terminal formatting operate flawlessly.

### Key Verification Principles
1. **Real Database Testing:** Tests execute against real MySQL InnoDB tables in `nextstep_db` via standard JDBC, verifying genuine foreign keys and constraints.
2. **Self-Cleaning Test Isolation:** Every test suite provisions fictional test entities and registers cleanup hooks to purge temporary records upon completion, leaving the database pristine.
3. **Piped Terminal Emulation:** Tests simulate real console keystrokes using byte streams (`ByteArrayInputStream` / `ByteArrayOutputStream`), validating input guards, retry prompts, and screen clear sequences.
4. **Visual Layout Verification:** Strips ANSI escape sequences and measures actual rendered character widths, guaranteeing strict compliance with the **64-column framed layout**.

---

## 2. Test Suites Summary & Pass Rate

```
========================================================================================
TEST SUITE RUNNER                           FOCUS / DOMAIN              TESTS   STATUS
========================================================================================
1. CentralizedDemoManagerTestRunner          Demo Profiles & Autofill      11    100% PASS
2. DemoModeVerificationTestRunner            Scenarios A/B/C & Idempotence 24    100% PASS
3. Phase3TestRunner                          Auth, Validation & DAOs       41    100% PASS
4. Phase4TestRunner                          Job Creation & Eligibility    47    100% PASS
5. Phase5TestRunner                          Streams, CSV, Generics, Lambdas54   100% PASS
6. Phase6IntegrationTestRunner               Transactions, Rollback & ACID 31    100% PASS
7. ConsoleInputEdgeCaseTestRunner            Menu Guards, B/N/A Shortcuts  89    100% PASS
----------------------------------------------------------------------------------------
TOTAL AUTOMATED TEST CASES                                               297    100% PASS
========================================================================================
8. VisualInspectionVerifier                  30 Screens Layout & Centering 30    100% PASS
========================================================================================
```

---

## 3. Detailed Breakdown of Test Suites

### 1. `CentralizedDemoManagerTestRunner` (11 Tests)
- **File:** [`tests/CentralizedDemoManagerTestRunner.java`](file:///c:/Semister-3/Programming%20in%20Java/JavaProjects/NextStep-Campus-Recruitment-System/tests/CentralizedDemoManagerTestRunner.java)
- **Coverage:**
  - Initialization of 3 distinct candidate profiles (Scenario A: Vihaan, Scenario B: Kabir, Scenario C: Arjun).
  - Placement Officer profile setup with activation code verification.
  - In-place editing of demo attributes and synchronization with MySQL.
  - Execution of application-wide `A`, `A1`, `A2`, `A3` autofill shortcuts.
  - Status updates and tracking for demo candidates.

### 2. `DemoModeVerificationTestRunner` (24 Tests)
- **File:** [`tests/DemoModeVerificationTestRunner.java`](file:///c:/Semister-3/Programming%20in%20Java/JavaProjects/NextStep-Campus-Recruitment-System/tests/DemoModeVerificationTestRunner.java)
- **Coverage:**
  - Idempotent demo setup: Re-executing demo setup creates zero duplicate accounts.
  - Scenario A evaluation: 100% eligible for Junior Backend Engineer opening.
  - Scenario B evaluation: Academic cutoff passed, 50% technical skills match (Java, SQL; missing OOP, Git).
  - Scenario C evaluation: Rejected due to CGPA, backlogs, and branch cutoffs.
  - Safe reset: Erases demo accounts while strictly preserving all real student records.

### 3. `Phase3TestRunner` (41 Tests)
- **File:** [`tests/Phase3TestRunner.java`](file:///c:/Semister-3/Programming%20in%20Java/JavaProjects/NextStep-Campus-Recruitment-System/tests/Phase3TestRunner.java)
- **Coverage:**
  - Validation guards: Rejection of invalid emails, out-of-bounds CGPA (>10.0), negative backlogs, unrealistic graduation years, and short phone numbers (<10 digits).
  - Duplicate detection: Database enforcement of unique usernames and USNs.
  - Authentication security: PBKDF2 hash verification, rejection of wrong passwords, generic rejection messages without leaking account existence.
  - DAO unit operations: `StudentDAO`, `CompanyDAO`, and `JobDAO`.

### 4. `Phase4TestRunner` (47 Tests)
- **File:** [`tests/Phase4TestRunner.java`](file:///c:/Semister-3/Programming%20in%20Java/JavaProjects/NextStep-Campus-Recruitment-System/tests/Phase4TestRunner.java)
- **Coverage:**
  - Dual-phase eligibility checking: Academic cutoffs and set-theoretic skill matching.
  - Application submission and lifecycle status initialization (`APPLIED`).
  - Duplicate application rejection at both service and database constraints.
  - Security authorization: Students blocked from updating application status (`SecurityException`).
  - Officer job creation: Validation of CTC packages, company foreign keys, and branch assignments.

### 5. `Phase5TestRunner` (54 Tests)
- **File:** [`tests/Phase5TestRunner.java`](file:///c:/Semister-3/Programming%20in%20Java/JavaProjects/NextStep-Campus-Recruitment-System/tests/Phase5TestRunner.java)
- **Coverage:**
  - Lambda Expressions: Sorting job drives by compensation package (CTC) descending.
  - Stream API Pipelines: `filter`, `map`, `distinct`, `sorted`, `count()`, `anyMatch()`.
  - Advanced Collectors: `Collectors.groupingBy()` and `Collectors.partitioningBy()`.
  - Java Generics: Generic wrapper `Result<T>` and generic method `ValidationUtil.paginate<T>`.
  - File I/O & CSV Processing: RFC-4180 CSV export, parsing, quote escaping, and record auditing.

### 6. `Phase6IntegrationTestRunner` (31 Tests)
- **File:** [`tests/Phase6IntegrationTestRunner.java`](file:///c:/Semister-3/Programming%20in%20Java/JavaProjects/NextStep-Campus-Recruitment-System/tests/Phase6IntegrationTestRunner.java)
- **Coverage:**
  - End-to-end student and officer workflows across all tiers.
  - Transaction rollback: Verifies that a failure during student skill insertion rolls back the uncommitted user record cleanly (`conn.rollback()`).
  - Resilience: Graceful skipping of malformed CSV lines without crashing.
  - Simulated restart persistence: Verification that persisted entities survive across independent DAO instances.

### 7. `ConsoleInputEdgeCaseTestRunner` (89 Tests)
- **File:** [`tests/ConsoleInputEdgeCaseTestRunner.java`](file:///c:/Semister-3/Programming%20in%20Java/JavaProjects/NextStep-Campus-Recruitment-System/tests/ConsoleInputEdgeCaseTestRunner.java)
- **Coverage:**
  - Terminal navigation: Out-of-bounds menu inputs, invalid choices, cancel options (0).
  - Form navigation: Backward navigation (`B`), forward skipping (`N`), and autofill (`A`).
  - Activation code gate: Unauthorized Placement Officer onboarding blocked.
  - Password mismatch detection on registration forms.

---

## 4. Visual Inspection & Responsive Layout Verification

[`tests/VisualInspectionVerifier.java`](file:///c:/Semister-3/Programming%20in%20Java/JavaProjects/NextStep-Campus-Recruitment-System/tests/VisualInspectionVerifier.java) inspects 30 discrete console screens, stripping ANSI color codes to measure visual column width:

```text
==================================================================
SUMMARY OF VISUAL REGRESSION & LAYOUT INSPECTIONS
==================================================================
Total Screens Evaluated : 30 Screens
Maximum Allowed Columns : 64 Characters
Lines Exceeding 64 Cols : 0 Lines (100% Compliant)
Horizontal Centering    : Verified at 80 cols (8 spaces), 
                          100 cols (18 spaces), 120 cols (28 spaces)
Top-Border Alignment    : Flawless (Stream race condition eliminated)
ASCII Box Rendering     : 100% Standard ASCII (+, -, |, =)
==================================================================
ALL 30 VISUAL REGRESSION INSPECTIONS COMPLETED SUCCESSFULLY!
```

---

## 5. How to Run All Test Suites Locally

In Windows PowerShell or CMD:
```powershell
# 1. Compile all source and test files
javac -cp "lib/*;bin" -d bin src/com/nextstep/model/*.java src/com/nextstep/database/*.java src/com/nextstep/dao/*.java src/com/nextstep/service/*.java src/com/nextstep/util/*.java src/com/nextstep/Main.java tests/*.java

# 2. Run Test Runners Individually
java -cp "bin;lib/*" CentralizedDemoManagerTestRunner
java -cp "bin;lib/*" DemoModeVerificationTestRunner
java -cp "bin;lib/*" Phase3TestRunner
java -cp "bin;lib/*" Phase4TestRunner
java -cp "bin;lib/*" Phase5TestRunner
java -cp "bin;lib/*" Phase6IntegrationTestRunner
java "-Dnextstep.officer.activation.code=CAMPUS_OFFICER_2026" -cp "bin;lib/*" ConsoleInputEdgeCaseTestRunner

# 3. Run Visual Verifier
java -cp "bin;lib/*" VisualInspectionVerifier
```
All suites execute in under 30 seconds combined and finish with `0 Failed`.
