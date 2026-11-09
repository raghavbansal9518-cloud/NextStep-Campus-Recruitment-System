-- =====================================================================
-- NextStep — Campus Recruitment System
-- Sample Test Data (Phase 1)
-- Fictional companies, jobs, eligible branches, and required skills.
-- =====================================================================

USE nextstep_db;

-- 1. Fictional Hiring Companies
INSERT INTO companies (name, industry, email, location, website) VALUES
('Nexis Cloud Technologies', 'Cloud & DevOps', 'careers@nexiscloud.example.com', 'Bengaluru, KA', 'https://nexiscloud.example.com'),
('Apex Financial Software', 'FinTech & Banking', 'campus@apexfin.example.com', 'Mumbai, MH', 'https://apexfin.example.com'),
('Quantum Robotics Labs', 'Embedded Systems & AI', 'recruitment@quantumrobotics.example.com', 'Hyderabad, TS', 'https://quantumrobotics.example.com')
ON DUPLICATE KEY UPDATE name=name;

-- 2. Fictional Job Openings
-- Job 1: Junior Backend Engineer at Nexis Cloud Technologies (Requires CSE/IT, Min CGPA 7.50, 0 Backlogs, 12.00 LPA)
INSERT INTO jobs (company_id, title, description, ctc_lpa, location, min_cgpa, max_backlogs, graduation_year, status)
SELECT id, 'Junior Backend Engineer', 'Develop and maintain scalable REST APIs and microservices using Core Java and SQL databases.', 12.00, 'Bengaluru, KA', 7.50, 0, 2026, 'OPEN'
FROM companies WHERE name = 'Nexis Cloud Technologies' LIMIT 1;

-- Job 2: FinTech Software Associate at Apex Financial Software (Allows CSE/IT/ECE, Min CGPA 7.00, Max 1 Backlog, 9.50 LPA)
INSERT INTO jobs (company_id, title, description, ctc_lpa, location, min_cgpa, max_backlogs, graduation_year, status)
SELECT id, 'Software Development Associate', 'Design high-throughput transaction processing workflows, database scripts, and financial reporting modules.', 9.50, 'Mumbai, MH', 7.00, 1, 2026, 'OPEN'
FROM companies WHERE name = 'Apex Financial Software' LIMIT 1;

-- Job 3: Systems & Firmware Trainee at Quantum Robotics (Allows ECE/EEE/CSE, Min CGPA 6.50, Max 2 Backlogs, 8.00 LPA)
INSERT INTO jobs (company_id, title, description, ctc_lpa, location, min_cgpa, max_backlogs, graduation_year, status)
SELECT id, 'Firmware & Systems Trainee', 'Build low-level controllers, real-time telemetry processing, and embedded interfaces.', 8.00, 'Hyderabad, TS', 6.50, 2, 2026, 'OPEN'
FROM companies WHERE name = 'Quantum Robotics Labs' LIMIT 1;

-- 3. Normalized Eligible Branches for Jobs
-- Job 1 (Nexis): CSE, IT
INSERT INTO job_eligible_branches (job_id, branch_name)
SELECT j.id, 'Computer Science' FROM jobs j WHERE j.title = 'Junior Backend Engineer'
UNION ALL
SELECT j.id, 'Information Technology' FROM jobs j WHERE j.title = 'Junior Backend Engineer';

-- Job 2 (Apex): CSE, IT, ECE
INSERT INTO job_eligible_branches (job_id, branch_name)
SELECT j.id, 'Computer Science' FROM jobs j WHERE j.title = 'Software Development Associate'
UNION ALL
SELECT j.id, 'Information Technology' FROM jobs j WHERE j.title = 'Software Development Associate'
UNION ALL
SELECT j.id, 'Electronics and Communication' FROM jobs j WHERE j.title = 'Software Development Associate';

-- Job 3 (Quantum): ECE, EEE, CSE
INSERT INTO job_eligible_branches (job_id, branch_name)
SELECT j.id, 'Electronics and Communication' FROM jobs j WHERE j.title = 'Firmware & Systems Trainee'
UNION ALL
SELECT j.id, 'Electrical and Electronics' FROM jobs j WHERE j.title = 'Firmware & Systems Trainee'
UNION ALL
SELECT j.id, 'Computer Science' FROM jobs j WHERE j.title = 'Firmware & Systems Trainee';

-- 4. Normalized Required Skills for Jobs
-- Job 1 (Nexis Backend): Java, SQL, OOP, Git
INSERT INTO job_required_skills (job_id, skill_name)
SELECT j.id, 'Java' FROM jobs j WHERE j.title = 'Junior Backend Engineer'
UNION ALL
SELECT j.id, 'SQL' FROM jobs j WHERE j.title = 'Junior Backend Engineer'
UNION ALL
SELECT j.id, 'OOP' FROM jobs j WHERE j.title = 'Junior Backend Engineer'
UNION ALL
SELECT j.id, 'Git' FROM jobs j WHERE j.title = 'Junior Backend Engineer';

-- Job 2 (Apex FinTech): Java, SQL, Problem Solving
INSERT INTO job_required_skills (job_id, skill_name)
SELECT j.id, 'Java' FROM jobs j WHERE j.title = 'Software Development Associate'
UNION ALL
SELECT j.id, 'SQL' FROM jobs j WHERE j.title = 'Software Development Associate'
UNION ALL
SELECT j.id, 'Problem Solving' FROM jobs j WHERE j.title = 'Software Development Associate';

-- Job 3 (Quantum Firmware): C, Java, Embedded C, Problem Solving
INSERT INTO job_required_skills (job_id, skill_name)
SELECT j.id, 'C' FROM jobs j WHERE j.title = 'Firmware & Systems Trainee'
UNION ALL
SELECT j.id, 'Java' FROM jobs j WHERE j.title = 'Firmware & Systems Trainee'
UNION ALL
SELECT j.id, 'Problem Solving' FROM jobs j WHERE j.title = 'Firmware & Systems Trainee';
