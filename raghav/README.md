# Raghav — Recruitment Engine & Business Logic Specialist

## 1. Aapka Project Mein Role:
Aapne **NextStep** ka sabse important dimaag—**Recruitment Business Logic aur Automated Eligibility Engine** banaya hai jo student ke academic metrics aur skills ko company ke criteria se real-time match karta hai.

---

## 2. Aapki Assigned Files:
1. `RecruitmentService.java` — Core eligibility evaluation engine, application processing, audit reports.
2. `JobDAO.java` — Database queries for jobs, eligible branches, and required technical skills.
3. `CompanyDAO.java` — Database operations for recruiting companies and placement partners.
4. `ApplicationDAO.java` — Database persistence for student applications, status changes, and history.

---

## 3. Sir ko 1-Minute Mein Kya Bolna Hai:
> *"Sir, maine NextStep ka Core Business Logic aur Automated Eligibility Engine design kiya hai. Jab student kisi job ko view karta hai ya apply karta hai, hamara algorithm 4 levels par check karta hai: 
> 1) CGPA Check (Student CGPA >= Minimum Cutoff)
> 2) Backlog Check (Active backlogs <= Max allowed backlogs)
> 3) Branch Check (Student branch matches eligible branches)
> 4) Skill Match Score (Student ke skills vs Job required skills ka set intersection)
> Agar student fail hota hai, system clear reason batata hai. Ineligible student job par apply hi nahi kar sakta. Sath hi maine TPO ke liye Application Management aur CSV Audit Export feature implement kiya hai."*

---

## 4. Sir Kya Questions Puch Sakte Hain? (Answers ke sath):

* **Q1: Automated Eligibility Engine kaise calculate karta hai?**
  * **Ans:** `RecruitmentService.java` mein `evaluateEligibility(student, job)` method hai. Wo student ke CGPA, backlogs, branch aur technical skills ko job ke criteria se compare karta hai aur `EligibilityResult` object return karta hai.

* **Q2: Kya student ek hi job par do baar apply kar sakta hai?**
  * **Ans:** Nahi sir! `ApplicationDAO.existsByStudentAndJob()` check karta hai agar application pehle se exist karti hai toh duplicate application allow nahi hoti.

* **Q3: Placement Officer application kaise manage karta hai?**
  * **Ans:** Officer student ki application ka status change kar sakta hai: `APPLIED` se `SHORTLISTED`, `REJECTED`, ya `PLACED`.
