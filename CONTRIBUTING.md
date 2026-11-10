# Contributing to NextStep

Thank you for your interest in contributing to **NextStep — Campus Recruitment System**. 

NextStep is an academic Java console and MySQL project developed for the **Programming in Java (25CSE0202)** course. We welcome contributions, bug fixes, documentation improvements, and feature suggestions from classmates, collaborators, and open-source contributors.

---

## 📌 Important Environment Notice

Before contributing, please note the fundamental architecture of the project:

- **What GitHub Shares:** Java source code (`src/`), test suites (`tests/`), SQL migration scripts (`database/`), documentation (`docs/`), build scripts (`run.bat`), and project dependencies (`lib/`).
- **What Remains Local:** Your MySQL database instance, database records, private credentials (`config/db.properties`), and environment variables (`NEXTSTEP_OFFICER_ACTIVATION_CODE`).
- **Data Privacy:** Local database configurations are registered in `.gitignore` and must **never** be committed to GitHub.

---

## 🛠️ Getting Started: Development Setup

### 1. Prerequisites
- **Java Development Kit (JDK):** Version 17 LTS, 21 LTS, or newer.
- **MySQL Server:** Community Server 8.0 or newer.
- **Git:** Command-line Git or any modern Git GUI.

---

### 2. Workflow for Teammates & Direct Collaborators (Write Access)

If you are a member of the project team with repository collaborator permissions:

1. **Clone the repository:**
   ```bash
   git clone https://github.com/raghavbansal9518-cloud/NextStep-Campus-Recruitment-System.git
   cd NextStep-Campus-Recruitment-System
   ```

2. **Initialize your local MySQL database:**
   ```bash
   mysql -u root -p < database/schema.sql
   mysql -u root -p < database/sample_data.sql
   ```

3. **Configure your local database credentials:**
   ```cmd
   copy config\db.properties.example config\db.properties
   ```
   Open `config/db.properties` in your text editor and set your local MySQL root password:
   ```properties
   db.url=jdbc:mysql://localhost:3306/nextstep_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   db.user=root
   db.password=YOUR_LOCAL_MYSQL_PASSWORD
   ```

4. **Configure your local Placement Officer activation code (optional, for officer registration):**
   ```cmd
   setx NEXTSTEP_OFFICER_ACTIVATION_CODE "YOUR_LOCAL_SECRET_CODE"
   ```
   *(Close and re-open your terminal window after running `setx` for the variable to take effect).*

5. **Verify your setup by running the launcher:**
   ```cmd
   run.bat
   ```

---

### 3. Workflow for External Contributors (Fork & Pull Request)

If you do not have direct push access to the main repository:

1. **Fork the repository** on GitHub by clicking the **Fork** button at the top right of the repository page.
2. **Clone your fork locally:**
   ```bash
   git clone https://github.com/<your-username>/NextStep-Campus-Recruitment-System.git
   cd NextStep-Campus-Recruitment-System
   ```
3. Set up your local MySQL database and `config/db.properties` as described in Step 2 above.
4. **Create a descriptive feature branch:**
   ```bash
   git checkout -b feature/your-feature-name
   # or for bug fixes:
   git checkout -b fix/issue-description
   ```
5. Make your modifications, adhering to the project coding standards.
6. Compile the code and run the regression test suites:
   ```powershell
   javac -d bin -cp "bin;lib/*" src/com/nextstep/model/*.java src/com/nextstep/database/*.java src/com/nextstep/dao/*.java src/com/nextstep/service/*.java src/com/nextstep/util/*.java src/com/nextstep/Main.java tests/*.java
   java -cp "bin;lib/*" Phase3TestRunner
   ```
7. Commit your changes using [Conventional Commits](https://www.conventionalcommits.org/):
   ```bash
   git commit -m "feat(module): brief description of change"
   ```
8. Push your branch to your fork on GitHub:
   ```bash
   git push -u origin feature/your-feature-name
   ```
9. Open a **Pull Request (PR)** against the `main` branch of the upstream repository.

---

## 📋 Code Quality & Architecture Guidelines

When contributing code:

- **Keep It Pure Core Java SE:** Avoid adding third-party web frameworks, ORMs, or Maven/Gradle plugins. The project is specifically designed to showcase pure Core Java SE and JDBC fundamentals.
- **Maintain Layered Architecture:**
  - Domain models belong in `com.nextstep.model`.
  - Database queries and SQL mapping belong in `com.nextstep.dao`.
  - Business rules, validation, and stream logic belong in `com.nextstep.service`.
  - Terminal presentation and interaction belong in `com.nextstep.Main` and `com.nextstep.util`.
- **SQL Security:** Always use parameterized `PreparedStatement` queries to prevent SQL injection. Never concatenate raw user input into SQL strings.
- **Resource Management:** Always use `try-with-resources` for `Connection`, `PreparedStatement`, `ResultSet`, and file I/O streams.
- **ANSI Console Consistency:** Use `ConsoleTheme` methods (`border`, `divider`, `prompt`, `text`, `green`, `red`) to preserve terminal color and column alignment.
- **Zero Secrets Rule:** Never commit real passwords, activation secrets, personal machine paths, or private configuration files.

---

## 💬 Getting Help

If you encounter any questions or issues while setting up the project:
- Refer to the [Demonstration & Operational User Manual](docs/DEMO_USER_MANUAL.md).
- Refer to the [Testing Guide](docs/TESTING.md).
- Open an Issue on GitHub describing your question or proposal.
