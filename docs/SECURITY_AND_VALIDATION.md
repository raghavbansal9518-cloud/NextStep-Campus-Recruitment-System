# NextStep — Security Architecture, Cryptography & Validation

---

## 1. Security Overview

NextStep implements enterprise-grade defensive programming standards appropriate for a high-integrity campus application. It enforces multi-layered defense spanning cryptographic storage, role-based authorization, SQL injection prevention, input sanitization, and transaction atomicity.

---

## 2. Password Cryptography: Salted PBKDF2

### The Vulnerability with Plain Text or MD5/SHA-1
- Storing plain-text passwords in a database is an immediate compliance violation.
- Simple hashes (like `MD5` or `SHA-256` without salt) are vulnerable to **Rainbow Table attacks**—pre-computed tables containing billions of plaintext passwords and their corresponding hashes.
- Fast hashing algorithms allow attackers to test billions of candidate passwords per second using GPUs.

### The NextStep Solution: PBKDF2WithHmacSHA256
Implemented in [`src/com/nextstep/service/AuthService.java`](file:///c:/Semister-3/Programming%20in%20Java/JavaProjects/NextStep-Campus-Recruitment-System/src/com/nextstep/service/AuthService.java#L57-L125):
- **Algorithm:** **PBKDF2** (Password-Based Key Derivation Function 2) using **HMAC-SHA256** as the pseudorandom function.
- **Cryptographic Salt:** A **16-byte (128-bit) cryptographically secure random salt** generated via `java.security.SecureRandom`. Every user receives a unique salt, making pre-computed rainbow tables completely useless.
- **Work Factor (Iterations):** **10,000 iterations** of key stretching. This intentionally slows down hash computation for an attacker by a factor of 10,000, rendering brute-force attacks computationally infeasible.
- **Derived Key Length:** 256 bits (32 bytes).
- **Encoded Storage Format:**
  ```text
  PBKDF2$10000$<saltHex>$<hashHex>
  ```

### Constant-Time Hash Comparison (Timing Attack Protection)
Standard Java string comparison (`str1.equals(str2)`) terminates early on the first mismatched character. Attackers can measure response times down to nanoseconds to deduce characters one-by-one (a **timing attack**).

NextStep eliminates timing attacks by using **`MessageDigest.isEqual()`**:
```java
public static boolean verifyPassword(String candidatePassword, String storedHash) {
    // ...
    byte[] testHash = pbkdf2(candidatePassword.toCharArray(), salt, iterations, hash.length);
    // MessageDigest.isEqual executes in constant time regardless of where bytes differ
    return MessageDigest.isEqual(hash, testHash);
}
```

---

## 3. Role-Based Access Control (RBAC)

NextStep enforces strict institutional role boundaries:
- **`Role.STUDENT`:** Permitted to browse jobs, check personal eligibility, file applications, and view their own application history.
- **`Role.PLACEMENT_OFFICER`:** Permitted to publish job drives, view all candidate records campus-wide, advance application statuses, and export placement audit ledgers.

### Security Guards in Service Layer
Every administrative service method inspects caller credentials before executing database queries:
```java
// In RecruitmentService.java:
public List<Application> getAllApplications(User officer) throws SQLException {
    if (officer == null || officer.getRole() != Role.PLACEMENT_OFFICER) {
        throw new SecurityException("Unauthorized: Only verified Placement Officers can inspect campus applications.");
    }
    return applicationDAO.findAll();
}

public boolean updateApplicationStatus(User officer, int applicationId, ApplicationStatus newStatus) throws SQLException {
    if (officer == null || officer.getRole() != Role.PLACEMENT_OFFICER) {
        throw new SecurityException("Unauthorized: Students cannot alter recruitment application status.");
    }
    return applicationDAO.updateStatus(applicationId, newStatus);
}
```

---

## 4. Placement Officer Activation Gate

To prevent unauthorized students from registering themselves as Placement Officers, the onboarding portal requires an institution-level **Placement Officer Activation Code**:
- Configured via system property `-Dnextstep.officer.activation.code` or environment variable.
- In Demo Mode, the official activation code is `CAMPUS_OFFICER_2026`.
- If an invalid code is supplied, account creation is immediately aborted, preventing privilege escalation.

---

## 5. SQL Injection Immunity: 100% PreparedStatements

NextStep **never concatenates raw user strings into SQL queries**. All database operations across all 5 DAO classes use `PreparedStatement` with parameterized placeholders (`?`).

```java
// Safe Parameterized Query in ApplicationDAO.java:
String sql = "SELECT * FROM applications WHERE student_id = ? AND job_id = ?";
try (Connection conn = DatabaseConnection.getConnection();
     PreparedStatement stmt = conn.prepareStatement(sql)) {
    stmt.setInt(1, studentId);
    stmt.setInt(2, jobId);
    try (ResultSet rs = stmt.executeQuery()) { ... }
}
```
Because parameters are transmitted over the MySQL wire protocol separately from the query syntax, malicious inputs (such as `' OR 1=1 --`) are treated as pure data literals and cannot alter the SQL parse tree.

---

## 6. Input Validation & Data Bounds

All user inputs are guarded in [`src/com/nextstep/util/ValidationUtil.java`](file:///c:/Semister-3/Programming%20in%20Java/JavaProjects/NextStep-Campus-Recruitment-System/src/com/nextstep/util/ValidationUtil.java):

| Input Field | Validation Rule | Implementation / Regex |
|---|---|---|
| **Email Address** | Must match RFC-5322 format | `^[a-zA-Z0-9_+&*-]+(?:\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,7}$` |
| **Academic CGPA** | Bounded decimal between 0.00 and 10.00 | `cgpa >= 0.0 && cgpa <= 10.0` |
| **Active Backlogs** | Must be a non-negative integer ($\ge 0$) | `backlogs >= 0` |
| **Phone Number** | Minimum 10 digits; digits only | `phone.replaceAll("[^0-9]", "").length() >= 10` |
| **Graduation Year** | Valid collegiate batch range | `year >= 2020 && year <= 2035` |
| **Job Package (CTC)** | Non-negative numeric value in LPA | `ctc >= 0.0` |

---

## 7. Database Integrity & Duplicate Blocking

Data integrity is protected at three separate architectural tiers:
1. **Console UI Tier:** Validates non-blank inputs before delegating to services.
2. **Service Tier:** Checks logical preconditions (e.g., verifying `!hasApplied()` before attempting insertion).
3. **Database Tier (Ultimate Safeguard):** Enforced by MySQL InnoDB constraints:
   - `users.username` $\rightarrow$ `UNIQUE`
   - `users.email` $\rightarrow$ `UNIQUE`
   - `students.student_id` $\rightarrow$ `UNIQUE` (Unique University Roll Number / USN)
   - `student_skills` $\rightarrow$ `UNIQUE (student_id, skill_name)`
   - `applications` $\rightarrow$ `UNIQUE (student_id, job_id)`

Even if concurrent threads bypass Java-level checks, MySQL rejects duplicate submissions with error code `1062 (ER_DUP_ENTRY)`, ensuring absolute database consistency.
