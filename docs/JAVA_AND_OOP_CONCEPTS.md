# NextStep — Java SE, OOP Principles & Syllabus Concepts Guide

---

## 1. Overview & Syllabus Alignment

NextStep was engineered to serve as a comprehensive, working demonstration of the **Semester 3 "Programming in Java" (25CSE0202)** curriculum. Every core language construct, object-oriented principle, and advanced library feature taught in lectures is directly utilized in the application's source code.

This document identifies each programming concept, explains why it was used, pinpoints its exact source code location, and provides concrete snippets for viva discussions.

---

## 2. Object-Oriented Programming (OOP) Principles

### A. Encapsulation (Data Hiding & Validation)
- **Concept:** Bundling data (fields) and the methods that operate on them inside a single unit (class), while restricting direct external access using `private` modifiers and exposing controlled access via `public` getters/setters.
- **Why Used:** Prevents unauthorized external code from corrupting entity state (e.g., setting a negative CGPA or null username).
- **Project Location:** All classes in [`src/com/nextstep/model/`](file:///c:/Semister-3/Programming%20in%20Java/JavaProjects/NextStep-Campus-Recruitment-System/src/com/nextstep/model/) (e.g., `Student.java`, `Job.java`, `User.java`).
- **Code Example (`Student.java`):**
  ```java
  public class Student {
      private int id;
      private String studentId; // USN
      private String fullName;
      private double cgpa;
      private int activeBacklogs;
      private List<String> skills;

      public double getCgpa() {
          return cgpa;
      }

      public void setCgpa(double cgpa) {
          if (cgpa >= 0.0 && cgpa <= 10.0) {
              this.cgpa = cgpa;
          } else {
              throw new IllegalArgumentException("CGPA must be between 0.00 and 10.00");
          }
      }
  }
  ```

---

### B. Inheritance (Code Reuse & Extensibility)
- **Concept:** A mechanism where a child class acquires the fields and behaviors of a parent class.
- **Why Used:** Java's standard I/O library uses inheritance to extend stream functionality. NextStep extends `java.io.FilterOutputStream` to build a custom responsive console margin stream.
- **Project Location:** [`src/com/nextstep/util/ConsoleTheme.java`](file:///c:/Semister-3/Programming%20in%20Java/JavaProjects/NextStep-Campus-Recruitment-System/src/com/nextstep/util/ConsoleTheme.java#L201-L238).
- **Code Example (`ConsoleTheme.AutoMarginOutputStream`):**
  ```java
  public static class AutoMarginOutputStream extends java.io.FilterOutputStream {
      private volatile int margin;
      private boolean atLineStart = true;
      private volatile byte[] marginBytes;

      public AutoMarginOutputStream(java.io.OutputStream out, int margin) {
          super(out); // Calls parent FilterOutputStream constructor
          setMargin(margin);
      }

      @Override
      public synchronized void write(int b) throws java.io.IOException {
          if (margin > 0 && atLineStart && b != '\n' && b != '\r') {
              out.write(marginBytes); // Inject margin at beginning of line
              atLineStart = false;
          }
          out.write(b);
          if (b == '\n' || b == '\r') {
              atLineStart = true;
          }
      }
  }
  ```

---

### C. Polymorphism (Runtime & Compile-Time)
- **Concept:** The ability of an object or method to take many forms.
  - **Compile-time Polymorphism (Method Overloading):** Methods sharing the same name but differing in parameter count or types.
  - **Runtime Polymorphism (Method Overriding):** A subclass providing a specific implementation of a method defined in its superclass or interface.
- **Why Used:** Overloading allows flexible helper invocation; overriding allows custom stream behavior and custom object comparisons.
- **Project Location:**
  - *Overloading:* [`JobDAO.java`](file:///c:/Semister-3/Programming%20in%20Java/JavaProjects/NextStep-Campus-Recruitment-System/src/com/nextstep/dao/JobDAO.java) (`createJobPosting(Job)` vs legacy 7-parameter overload).
  - *Overriding:* [`AutoMarginOutputStream.write()`](file:///c:/Semister-3/Programming%20in%20Java/JavaProjects/NextStep-Campus-Recruitment-System/src/com/nextstep/util/ConsoleTheme.java#L221) and lambda `Comparator.compare()`.
- **Code Example:**
  ```java
  // Method Overloading in JobDAO.java:
  public int createJobPosting(Job job) throws SQLException { ... }

  public int createJobPosting(int companyId, String title, String description,
                              double minCgpa, int gradYear, List<String> branches,
                              List<String> skills) throws SQLException {
      // Overloaded convenience method delegating to primary object method
  }
  ```

---

### D. Abstraction (Hiding Internal Complexity)
- **Concept:** Hiding complex implementation details and showing only the essential features to the user.
- **Why Used:** The Service layer abstracts away database connection management, SQL statement generation, and result set traversal. The presentation layer simply requests `applyForJob(student, jobId)` without knowing SQL exists.
- **Project Location:** [`RecruitmentService.java`](file:///c:/Semister-3/Programming%20in%20Java/JavaProjects/NextStep-Campus-Recruitment-System/src/com/nextstep/service/RecruitmentService.java) and [`DatabaseConnection.java`](file:///c:/Semister-3/Programming%20in%20Java/JavaProjects/NextStep-Campus-Recruitment-System/src/com/nextstep/database/DatabaseConnection.java).

---

## 3. Java Collections Framework

NextStep relies heavily on Java Collections for data manipulation:

| Collection Interface | Concrete Class | Where Used | Why It Was Chosen |
|---|---|---|---|
| `List<E>` | `java.util.ArrayList<E>` | `Student.getSkills()`, `Job.getEligibleBranches()`, `ApplicationDAO.findAll()` | Fast random access ($O(1)$) and dynamic resizing for ordered lists. |
| `Set<E>` | `java.util.HashSet<E>` | `RecruitmentService.evaluateEligibility()` | $O(1)$ lookup complexity for set-theoretic skill intersection without duplicate entries. |
| `Map<K, V>` | `java.util.Map<K, V>` | `RecruitmentService.getApplicationsByStatusSummary()` | Key-value mapping produced by Stream `Collectors.groupingBy()`. |

### Code Example: Skill Matching Using `HashSet`
```java
// Set-Theoretic intersection in RecruitmentService.java
Set<String> studentSkillSet = new HashSet<>();
for (String s : student.getSkills()) {
    studentSkillSet.add(s.trim().toLowerCase());
}

List<String> matchedSkills = new ArrayList<>();
List<String> missingSkills = new ArrayList<>();

for (String req : job.getRequiredSkills()) {
    if (studentSkillSet.contains(req.trim().toLowerCase())) {
        matchedSkills.add(req);
    } else {
        missingSkills.add(req);
    }
}
```

---

## 4. Lambda Expressions & Stream API (Lectures 43–46)

Course handout lectures 43–46 focus on modern functional Java: custom comparators via Lambdas and pipeline operations via the Stream API. NextStep implements every required stream operation:

### 1. Lambda Expression in Sorting (Lectures 43–44)
Sorting active job drives by CTC package descending:
```java
// In RecruitmentService.java:
public List<Job> getJobsSortedByCtcDescending() throws SQLException {
    List<Job> jobs = jobDAO.findAll();
    // Lambda expression implementing Comparator<Job>
    jobs.sort((j1, j2) -> Double.compare(j2.getCtcLpa(), j1.getCtcLpa()));
    return jobs;
}
```

### 2. Stream Pipeline: `filter`, `map`, `distinct`, `sorted`, `collect` (Lectures 45–46)
Retrieving distinct company names with active recruitment drives sorted alphabetically:
```java
// In RecruitmentService.java:
public List<String> getDistinctHiringCompanies() throws SQLException {
    List<Job> allJobs = jobDAO.findAll();
    return allJobs.stream()
            .filter(j -> "OPEN".equalsIgnoreCase(j.getStatus())) // Filter active drives
            .map(Job::getCompanyName)                           // Method reference mapping to Company Name
            .filter(name -> name != null && !name.isBlank())     // Filter blank names
            .distinct()                                          // Deduplicate company names
            .sorted()                                            // Sort alphabetically
            .collect(Collectors.toList());                       // Collect into immutable List
}
```

### 3. Terminal Operations: `count()`, `anyMatch()`, `mapToDouble()`, `average()`
```java
// In RecruitmentService.java:
public long countOpenJobs() throws SQLException {
    return jobDAO.findAll().stream()
            .filter(j -> "OPEN".equalsIgnoreCase(j.getStatus()))
            .count(); // Terminal reduction operation
}

public double getAverageCtcLpa() throws SQLException {
    return jobDAO.findAll().stream()
            .mapToDouble(Job::getCtcLpa)
            .average()
            .orElse(0.0);
}
```

### 4. Advanced Collectors: `groupingBy()` and `partitioningBy()`
Generating aggregated placement statistics for institutional reporting:
```java
// Grouping applications by status:
public Map<ApplicationStatus, Long> getApplicationsByStatusSummary(User officer) throws SQLException {
    List<Application> apps = getAllApplications(officer);
    return apps.stream()
            .collect(Collectors.groupingBy(Application::getStatus, Collectors.counting()));
}

// Partitioning applications into Selected vs. In-Progress/Rejected:
public Map<Boolean, Long> partitionApplicationsBySelection(User officer) throws SQLException {
    List<Application> apps = getAllApplications(officer);
    return apps.stream()
            .collect(Collectors.partitioningBy(
                    a -> a.getStatus() == ApplicationStatus.SELECTED,
                    Collectors.counting()
            ));
}
```

---

## 5. Java Generics (Lectures 47–48)

NextStep demonstrates Generics at both the **class level** and **method level**:

### 1. Generic Class: `Result<T>`
Provides a type-safe container for service operations returning either a payload of type `T` or an error string:
```java
// In src/com/nextstep/util/Result.java:
public class Result<T> {
    private final boolean success;
    private final T data;
    private final String errorMessage;

    private Result(boolean success, T data, String errorMessage) {
        this.success = success;
        this.data = data;
        this.errorMessage = errorMessage;
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(true, data, null);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(false, null, message);
    }

    public boolean isSuccess() { return success; }
    public T getData() { return data; }
    public String getErrorMessage() { return errorMessage; }
}
```

### 2. Generic Method: `ValidationUtil.paginate<T>`
A generic utility method capable of slicing any collection type for pagination:
```java
// In src/com/nextstep/util/ValidationUtil.java:
public static <T> List<T> paginate(List<T> source, int pageNumber, int pageSize) {
    if (source == null || source.isEmpty() || pageNumber < 1 || pageSize < 1) {
        return Collections.emptyList();
    }
    int fromIndex = (pageNumber - 1) * pageSize;
    if (fromIndex >= source.size()) {
        return Collections.emptyList();
    }
    int toIndex = Math.min(fromIndex + pageSize, source.size());
    return source.subList(fromIndex, toIndex);
}
```

---

## 6. Exception Handling Architecture

NextStep follows defensive exception handling principles:
1. **Checked Exceptions (`SQLException`, `IOException`):** Caught at service boundaries, logged cleanly, and translated into meaningful user-facing messages or domain exceptions.
2. **Unchecked Exceptions (`IllegalArgumentException`, `IllegalStateException`, `SecurityException`):** Used to prevent invalid state transitions (e.g., non-officer attempting status update throws `SecurityException`).
3. **`try-with-resources`:** Every database connection and file stream is enclosed in a try-with-resources block guaranteeing automatic closure without explicit `finally` blocks.

```java
// Example from StudentDAO.java:
public Student findByUserId(int userId) throws SQLException {
    String sql = "SELECT * FROM students WHERE user_id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setInt(1, userId);
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return mapResultSetToStudent(rs);
            }
        }
    } // conn, stmt, and rs closed automatically here
    return null;
}
```

---

## 7. File I/O & CSV Processing (Lectures 49–54)

NextStep implements standard **RFC-4180 CSV generation and parsing** using `BufferedReader` and `BufferedWriter` with try-with-resources:

- **Exporting Records:** Formats application data into CSV rows with header metadata (`ApplicationID,StudentUSN,StudentName,JobTitle,Company,Status,AppliedDate`). Fields containing commas or quotes are properly escaped with enclosing double-quotes.
- **Importing & Auditing:** Reads external CSV records line-by-line, parses delimited fields while honoring quoted tokens, and verifies persisted database state.
- **Project Location:** [`RecruitmentService.java`](file:///c:/Semister-3/Programming%20in%20Java/JavaProjects/NextStep-Campus-Recruitment-System/src/com/nextstep/service/RecruitmentService.java#L450-L620).

---

## 8. Enumerations (Type-Safe Constants)

Instead of error-prone string constants, NextStep enforces compile-time role and status validation using Java Enums:

1. **`Role.java`:**
   ```java
   public enum Role {
       STUDENT,
       PLACEMENT_OFFICER
   }
   ```
2. **`ApplicationStatus.java`:**
   ```java
   public enum ApplicationStatus {
       APPLIED,
       SHORTLISTED,
       SELECTED,
       REJECTED
   }
   ```
These enums directly match the `ENUM('STUDENT', 'PLACEMENT_OFFICER')` and `ENUM('APPLIED', 'SHORTLISTED', 'SELECTED', 'REJECTED')` definitions in MySQL, ensuring zero impedance mismatch between Java and the database.
