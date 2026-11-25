# Keshav — Database Architect & Data Models Specialist

## 1. Aapka Project Mein Role:
Aapne **NextStep** ka Database foundation aur Java Data Models (Entities) design kiye hain.

---

## 2. Aapki Assigned Files:
1. `schema.sql` — MySQL Database Tables, Primary Keys, Foreign Keys, Indexes.
2. `sample_data.sql` — College campus demo data (companies, jobs, student profiles).
3. `DatabaseConnection.java` — JDBC Connection management using **Singleton Pattern**.
4. `User.java` & `Role.java` — Base user entity with Role Enum (`STUDENT`, `PLACEMENT_OFFICER`).
5. `Student.java` — Student profile entity (USN, CGPA, Branch, Backlogs, Skills).
6. `Company.java` — Recruiting company entity.
7. `Job.java` — Job drive requirements (Salary/LPA, Min CGPA, Max Backlogs, Required Skills).
8. `Application.java` & `ApplicationStatus.java` — Job applications & Status Enum (`APPLIED`, `SHORTLISTED`, `REJECTED`, `PLACED`).

---

## 3. Sir ko 1-Minute Mein Kya Bolna Hai:
> *"Sir, maine NextStep ka Database Schema aur Java Model Layer design kiya hai. MySQL mein humne 8 relational tables banaye hain jisme Foreign Keys aur cascading rules define hain. Java side par maine DatabaseConnection class mein Singleton Pattern lagaya hai taaki database ka sirf ek instance create ho aur connection leaks na hon. Saari entities encapsulation follow karti hain with proper getters, setters aur constructors."*

---

## 4. Sir Kya Questions Puch Sakte Hain? (Answers ke sath):

* **Q1: DatabaseConnection mein Singleton Pattern kyun use kiya?**
  * **Ans:** Agar har query ke liye naya connection banayenge toh MySQL server par load badhega aur memory leak hoga. Singleton pattern ensure karta hai ki poore application mein ek single database connection pool reuse ho.

* **Q2: Schema mein kaun kaun se main tables hain?**
  * **Ans:** `users`, `students`, `companies`, `jobs`, `applications`, `job_eligible_branches`, aur `job_required_skills`.

* **Q3: Enum use karne ka kya fayda hua (Role aur ApplicationStatus)?**
  * **Ans:** Enums type-safety provide karte hain. Koi invalid role ya status enter nahi kar sakta (jaise spelling mistake 'admin' vs 'STUDENT').
