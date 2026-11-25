-- =====================================================================
-- NextStep — Campus Recruitment System
-- Sample Test Data (Phase 1 / Phase 2 Idempotent)
-- Fictional companies, jobs, eligible branches, and required skills.
-- Safe to execute repeatedly without generating duplicate records.
-- =====================================================================

USE nextstep_db;

-- 1. Fictional Hiring Companies (Idempotent via UNIQUE name)
INSERT INTO companies (name, industry, email, location, website) VALUES
('Nexis Cloud Technologies', 'Cloud & DevOps', 'careers@nexiscloud.example.com', 'Bengaluru, KA', 'https://nexiscloud.example.com'),
('Apex Financial Software', 'FinTech & Banking', 'campus@apexfin.example.com', 'Mumbai, MH', 'https://apexfin.example.com'),
('Quantum Robotics Labs', 'Embedded Systems & AI', 'recruitment@quantumrobotics.example.com', 'Hyderabad, TS', 'https://quantumrobotics.example.com')
ON DUPLICATE KEY UPDATE name=name;

-- 2. Fictional Job Openings (Idempotent via NOT EXISTS guard)
-- Job 1: Junior Backend Engineer at Nexis Cloud Technologies (Requires CSE/IT, Min CGPA 7.50, 0 Backlogs, 12.00 LPA)
INSERT INTO jobs (company_id, title, description, ctc_lpa, location, min_cgpa, max_backlogs, graduation_year, status)
SELECT c.id, 'Junior Backend Engineer', 'Develop and maintain scalable REST APIs and microservices using Core Java and SQL databases.', 12.00, 'Bengaluru, KA', 7.50, 0, 2026, 'OPEN'
FROM companies c
WHERE c.name = 'Nexis Cloud Technologies'
  AND NOT EXISTS (
      SELECT 1 FROM jobs j WHERE j.company_id = c.id AND j.title = 'Junior Backend Engineer'
  )
LIMIT 1;

-- Job 2: FinTech Software Associate at Apex Financial Software (Allows CSE/IT/ECE, Min CGPA 7.00, Max 1 Backlog, 9.50 LPA)
INSERT INTO jobs (company_id, title, description, ctc_lpa, location, min_cgpa, max_backlogs, graduation_year, status)
SELECT c.id, 'Software Development Associate', 'Design high-throughput transaction processing workflows, database scripts, and financial reporting modules.', 9.50, 'Mumbai, MH', 7.00, 1, 2026, 'OPEN'
FROM companies c
WHERE c.name = 'Apex Financial Software'
  AND NOT EXISTS (
      SELECT 1 FROM jobs j WHERE j.company_id = c.id AND j.title = 'Software Development Associate'
  )
LIMIT 1;

-- Job 3: Systems & Firmware Trainee at Quantum Robotics (Allows ECE/EEE/CSE, Min CGPA 6.50, Max 2 Backlogs, 8.00 LPA)
INSERT INTO jobs (company_id, title, description, ctc_lpa, location, min_cgpa, max_backlogs, graduation_year, status)
SELECT c.id, 'Firmware & Systems Trainee', 'Build low-level controllers, real-time telemetry processing, and embedded interfaces.', 8.00, 'Hyderabad, TS', 6.50, 2, 2026, 'OPEN'
FROM companies c
WHERE c.name = 'Quantum Robotics Labs'
  AND NOT EXISTS (
      SELECT 1 FROM jobs j WHERE j.company_id = c.id AND j.title = 'Firmware & Systems Trainee'
  )
LIMIT 1;

-- 3. Normalized Eligible Branches for Jobs (Idempotent via INSERT IGNORE & UNIQUE uq_job_branch)
-- Job 1 (Nexis): CSE, IT
INSERT IGNORE INTO job_eligible_branches (job_id, branch_name)
SELECT j.id, 'Computer Science' FROM jobs j JOIN companies c ON j.company_id = c.id WHERE c.name = 'Nexis Cloud Technologies' AND j.title = 'Junior Backend Engineer'
UNION ALL
SELECT j.id, 'Information Technology' FROM jobs j JOIN companies c ON j.company_id = c.id WHERE c.name = 'Nexis Cloud Technologies' AND j.title = 'Junior Backend Engineer';

-- Job 2 (Apex): CSE, IT, ECE
INSERT IGNORE INTO job_eligible_branches (job_id, branch_name)
SELECT j.id, 'Computer Science' FROM jobs j JOIN companies c ON j.company_id = c.id WHERE c.name = 'Apex Financial Software' AND j.title = 'Software Development Associate'
UNION ALL
SELECT j.id, 'Information Technology' FROM jobs j JOIN companies c ON j.company_id = c.id WHERE c.name = 'Apex Financial Software' AND j.title = 'Software Development Associate'
UNION ALL
SELECT j.id, 'Electronics and Communication' FROM jobs j JOIN companies c ON j.company_id = c.id WHERE c.name = 'Apex Financial Software' AND j.title = 'Software Development Associate';

-- Job 3 (Quantum): ECE, EEE, CSE
INSERT IGNORE INTO job_eligible_branches (job_id, branch_name)
SELECT j.id, 'Electronics and Communication' FROM jobs j JOIN companies c ON j.company_id = c.id WHERE c.name = 'Quantum Robotics Labs' AND j.title = 'Firmware & Systems Trainee'
UNION ALL
SELECT j.id, 'Electrical and Electronics' FROM jobs j JOIN companies c ON j.company_id = c.id WHERE c.name = 'Quantum Robotics Labs' AND j.title = 'Firmware & Systems Trainee'
UNION ALL
SELECT j.id, 'Computer Science' FROM jobs j JOIN companies c ON j.company_id = c.id WHERE c.name = 'Quantum Robotics Labs' AND j.title = 'Firmware & Systems Trainee';

-- 4. Normalized Required Skills for Jobs (Idempotent via INSERT IGNORE & UNIQUE uq_job_skill)
-- Job 1 (Nexis Backend): Java, SQL, OOP, Git
INSERT IGNORE INTO job_required_skills (job_id, skill_name)
SELECT j.id, 'Java' FROM jobs j JOIN companies c ON j.company_id = c.id WHERE c.name = 'Nexis Cloud Technologies' AND j.title = 'Junior Backend Engineer'
UNION ALL
SELECT j.id, 'SQL' FROM jobs j JOIN companies c ON j.company_id = c.id WHERE c.name = 'Nexis Cloud Technologies' AND j.title = 'Junior Backend Engineer'
UNION ALL
SELECT j.id, 'OOP' FROM jobs j JOIN companies c ON j.company_id = c.id WHERE c.name = 'Nexis Cloud Technologies' AND j.title = 'Junior Backend Engineer'
UNION ALL
SELECT j.id, 'Git' FROM jobs j JOIN companies c ON j.company_id = c.id WHERE c.name = 'Nexis Cloud Technologies' AND j.title = 'Junior Backend Engineer';

-- Job 2 (Apex FinTech): Java, SQL, Problem Solving
INSERT IGNORE INTO job_required_skills (job_id, skill_name)
SELECT j.id, 'Java' FROM jobs j JOIN companies c ON j.company_id = c.id WHERE c.name = 'Apex Financial Software' AND j.title = 'Software Development Associate'
UNION ALL
SELECT j.id, 'SQL' FROM jobs j JOIN companies c ON j.company_id = c.id WHERE c.name = 'Apex Financial Software' AND j.title = 'Software Development Associate'
UNION ALL
SELECT j.id, 'Problem Solving' FROM jobs j JOIN companies c ON j.company_id = c.id WHERE c.name = 'Apex Financial Software' AND j.title = 'Software Development Associate';

-- Job 3 (Quantum Firmware): C, Java, Embedded C, Problem Solving
INSERT IGNORE INTO job_required_skills (job_id, skill_name)
SELECT j.id, 'C' FROM jobs j JOIN companies c ON j.company_id = c.id WHERE c.name = 'Quantum Robotics Labs' AND j.title = 'Firmware & Systems Trainee'
UNION ALL
SELECT j.id, 'Java' FROM jobs j JOIN companies c ON j.company_id = c.id WHERE c.name = 'Quantum Robotics Labs' AND j.title = 'Firmware & Systems Trainee'
UNION ALL
SELECT j.id, 'Problem Solving' FROM jobs j JOIN companies c ON j.company_id = c.id WHERE c.name = 'Quantum Robotics Labs' AND j.title = 'Firmware & Systems Trainee';
