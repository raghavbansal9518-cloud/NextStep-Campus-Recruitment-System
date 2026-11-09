# NextStep — Campus Recruitment System

A console-based Java application designed to streamline campus recruitment and placement workflows for students, recruiters, and placement administrators.

---

## 📌 Project Overview & System Requirements
- **Course:** Semester 3 — Programming in Java (CA-3 Scope)
- **Application Type:** Console-based Application (CLI)
- **Java Development Kit:** JDK 21 LTS recommended (Compatible with installed JDK 26)
- **Database:** MySQL Server 8.0 (InnoDB engine, utf8mb4 charset)
- **JDBC Driver:** MySQL Connector/J 8.4.0 (`lib/mysql-connector-j-8.4.0.jar`)
- **Key Concepts:** Object-Oriented Programming (OOP), Encapsulation, POJO Data Models, Enums, JDBC (`DriverManager`, `PreparedStatement`, `ResultSet`), DAO Pattern, and Layered Architecture.

---

## 🏗️ Layered Architecture
The application follows a clean separation of concerns:

```text
NextStep-Campus-Recruitment-System/
├── lib/
│   └── mysql-connector-j-8.4.0.jar      # Official MySQL JDBC Driver
├── config/
│   ├── db.properties.example             # Git-tracked configuration template
│   └── db.properties                     # Local credentials (ignored by Git)
├── database/
│   ├── schema.sql                        # DDL: 8 normalized relational tables
│   └── sample_data.sql                   # DML: Realistic fictional recruitment data
├── src/
│   └── com/
│       └── nextstep/
│           ├── model/                    # Domain POJOs & Enums (Phase 1)
│           │   ├── Role.java
│           │   ├── ApplicationStatus.java
│           │   ├── User.java
│           │   ├── Student.java
│           │   ├── Company.java
│           │   ├── Job.java
│           │   └── Application.java
│           ├── database/                 # JDBC Connection Manager (Phase 2)
│           │   └── DatabaseConnection.java
│           ├── dao/                      # Data Access Objects (Phase 2)
│           │   └── UserDAO.java
│           └── Main.java                 # Application entry point
├── .gitignore
└── README.md
```

---

## ⚙️ Local Database Configuration Setup

1. **Copy the configuration template:**
   Create a local file named `config/db.properties` based on `config/db.properties.example`.
   *(Note: `config/db.properties` is registered in `.gitignore` and is never committed).*

   ```properties
   db.url=jdbc:mysql://localhost:3306/nextstep_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   db.user=root
   db.password=YOUR_LOCAL_MYSQL_PASSWORD
   ```

2. **Initialize Database Tables and Sample Data:**
   Run the scripts in order using MySQL CLI with the interactive password prompt:
   ```bash
   mysql -u root -p < database/schema.sql
   mysql -u root -p < database/sample_data.sql
   ```

---

## 🚀 How to Compile and Run

### 1. Compile All Sources (with Connector/J in Classpath):
```powershell
javac -cp ".;lib/*" -d bin (Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName })
```

### 2. Run Database Connection Test:
```powershell
java -cp "bin;lib/*" com.nextstep.database.DatabaseConnection
```

### 3. Run User DAO Verification Test:
```powershell
java -cp "bin;lib/*" com.nextstep.dao.UserDAO
```

### 4. Run Main Application:
```powershell
java -cp "bin;lib/*" com.nextstep.Main
```

---

## 📋 Completed Features & Progress
- **Phase 0:** Project setup, Git initialization, and baseline verification *(Merged into `main`)*
- **Phase 1:** Core Domain Models (OOP principles, Enums, POJOs) & Database Schema (8 normalized tables) *(Merged into `main`)*
- **Phase 2:** JDBC Connection (`DatabaseConnection.java`), MySQL Connector/J 8.4.0 setup, and Basic DAO (`UserDAO.java`) *(Current Branch: `feature/phase-2-jdbc-dao`)*
