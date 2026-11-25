# Sahaj — Security, Authentication & User Data Lead

## 1. Aapka Project Mein Role:
Aapne **NextStep** ka Security architecture, Password Encryption, User Authentication aur Student/User Data Access Object (DAO) develop kiya hai.

---

## 2. Aapki Assigned Files:
1. `AuthService.java` — Login, registration, password hashing, and role verification.
2. `UserDAO.java` — Database operations for user credentials and existence checks.
3. `StudentDAO.java` — Database CRUD for student academic profiles and technical skills.
4. `ValidationUtil.java` — Input validation (Email regex, phone digits, CGPA 0-10, valid graduation year).
5. `Result.java` — Generic operation wrapper pattern (`Result<T>`) for clean error handling.

---

## 3. Sir ko 1-Minute Mein Kya Bolna Hai:
> *"Sir, maine project ka Authentication aur Security subsystem implement kiya hai. Security ke liye humne industry-standard PBKDF2 with HMAC-SHA256 algorithm use kiya hai jisme cryptographic salt aur 10,000 iterations lagte hain taaki passwords brute-force attacks se safe rahein. Student registration mein maine JDBC Transactions use kiye hain with `conn.setAutoCommit(false)` taaki agar skills save hone mein fail ho jayein toh user table automatic rollback ho jaye aur inconsistent data na bache."*

---

## 4. Sir Kya Questions Puch Sakte Hain? (Answers ke sath):

* **Q1: Password encryption ke liye MD5 ya SHA-256 kyun nahi use kiya?**
  * **Ans:** Plain SHA-256 ya MD5 fast hote hain aur Rainbow Table attacks se easily break ho jaate hain. PBKDF2 with Salt aur 10,000 hash rounds computationally expensive hota hai, jisse password cracking mathematically impractical ho jaati hai.

* **Q2: Registration transaction mein rollback kyun zaroori tha?**
  * **Ans:** Student registration mein 3 tables affect hote hain: `users`, `students`, aur `student_skills`. Agar 3rd step mein error aaye toh pehle ke 2 steps delete (rollback) ho jaane chahiye, varna database corrupt ho jayega.

* **Q3: Placement Officer registration secure kaise hai?**
  * **Ans:** Officer account koi bhi student nahi bana sakta; system ek secure administrative activation code verify karta hai (`CAMPUS_OFFICER_2026`).
