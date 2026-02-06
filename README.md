# School Management System (SaaS)

A full-stack School Management System built using **Spring Boot (Backend)** and **Next.js (Frontend)**.

This project aims to provide a scalable SaaS platform for managing schools, students, fees, exams, and administration.

---

## 🚀 Tech Stack

### Backend
- Java 17
- Spring Boot 3.x
- Spring Data JPA
- Hibernate
- PostgreSQL / H2 (for testing)
- Spring Security (basic setup)
- Flyway (DB migrations)
- OpenAPI / Swagger
- Gradle

### Frontend
- Next.js (App Router)
- React
- Tailwind CSS
- TypeScript

---

## 📁 Project Structure

school-management/
├── backend/ → Spring Boot application
└── frontend/ → Next.js application


### Backend Modules

com.school.backend
├── school
├── user
├── core
│ ├── student
│ ├── guardian
│ ├── teacher
│ └── classsubject
├── fee
├── testmanagement
└── common

---

## ✅ Current Features

### Implemented
- School Management (Role-based CRUD)
- Academic Session Management (Standarized years)
- Class & Subject Management
- Student Registration & Bulk Attendance
- Enrollment & Promotion
- Guardian Module (basic)
- Fee Management (Collection, Receipt PDFs)
- REST APIs
- Integration Tests (Multi-tenant)
- Pagination Support (50 students/page)
- DTO Mapping
- Global Exception Handling

### In Progress
- Student Dashboard
- Fee Reports
- Marksheet Generation (UI Ready)
- Authentication (Basic)

---

## ⚙️ Backend Setup

### Requirements
- Java 17+
- Gradle
- PostgreSQL (optional, H2 for dev)

### Run Backend

```bash
cd backend
./gradlew bootRun
Backend runs at:
http://localhost:8080
```

Swagger UI:
http://localhost:8080/swagger-ui.html

⚙️ Frontend Setup
Requirements
Node.js (18+ recommended)

```bash
npm / yarn

Run Frontend
  cd frontend
  npm install
  npm run dev

Frontend runs at:
http://localhost:3000
```

🧪 Running Tests

```bash
Backend tests:

cd backend
./gradlew test
Uses H2 in-memory DB.
```


🗄️ Environment Profiles
dev → Local development

test → Integration tests

prod → Production (planned)

---

📌 Development Guidelines
Use DTOs for API communication

Keep entities internal

Prefer pagination for list APIs

Write integration tests for major flows

Commit stable code frequently

---

📈 Roadmap
Planned Features:

Authentication (JWT / Session-based)

Multi-tenant SaaS support (Backend Verified)

School-wise grading system

Timetable

Parent Portal

Mobile-friendly UI

Payment Gateway Integration

---

👨‍💻 Author
Developed by: Sundaram Tiwari

Backend-focused SaaS platform for Indian schools.

📄 License
This project is currently private and under active development.

---
