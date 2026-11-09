# NextStep — Campus Recruitment System

A console-based Java application designed to streamline campus recruitment and placement workflows for students, recruiters, and placement administrators.

---

## 📌 Project Overview
- **Course:** Semester 3 — Programming in Java
- **Application Type:** Console-based Application (CLI)
- **Database:** MySQL (using JDBC for persistent storage)
- **Key Concepts:** Object-Oriented Programming (OOP), Collections Framework, Exception Handling, File I/O, JDBC, and Layered Architecture.

---

## 🏗️ Layered Architecture
The application follows a clean separation of concerns:

```text
NextStep-Campus-Recruitment-System/
├── src/
│   └── com/
│       └── nextstep/
│           ├── model/        # Data models / POJOs (Entities)
│           ├── repository/   # DAO layer (Direct SQL / JDBC operations)
│           ├── service/      # Business logic and input validation
│           ├── ui/           # Console menus, Prompts, and CLI views
│           ├── database/     # Database connection management
│           └── Main.java     # Application entry point
├── .gitignore
└── README.md
```

*(Note: Package folders will be created incrementally in their respective phases.)*

---

## 🚀 How to Compile and Run

### 1. Compile:
From the root project directory:
```bash
javac -d bin src/com/nextstep/Main.java
```

### 2. Run:
```bash
java -cp bin com.nextstep.Main
```

---

## 📋 Development Roadmap
- **Phase 0:** Project setup, Git initialization, and baseline verification *(Current)*
- **Phase 1:** Core Domain Models (OOP principles, Encapsulation, POJOs)
- **Phase 2:** Database Design & JDBC Connection Layer (MySQL persistence)
- **Phase 3:** Data Access Object (DAO) & Repository Layer
- **Phase 4:** Service Layer & Business Validation Rules
- **Phase 5:** Interactive Console UI Menus
- **Phase 6:** Testing, Edge-case Handling & Viva Preparation
