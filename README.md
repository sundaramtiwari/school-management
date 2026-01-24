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
- School Management
- Class & Subject Management
- Student Registration
- Enrollment & Promotion
- Guardian Module (basic)
- Fee Management (partial)
- REST APIs
- Integration Tests
- Pagination Support
- DTO Mapping
- Global Exception Handling

### In Progress
- Frontend Admin Panel
- Student Dashboard
- Fee Reports
- Marksheet Generation
- Authentication & Authorization
- Role Management

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

Swagger UI:
http://localhost:8080/swagger-ui.html

⚙️ Frontend Setup
Requirements
Node.js (18+ recommended)

npm / yarn

Run Frontend
  cd frontend
  npm install
  npm run dev

Frontend runs at:
http://localhost:3000


🧪 Running Tests

Backend tests:

cd backend
./gradlew test
Uses H2 in-memory DB.

🗄️ Environment Profiles
dev → Local development

test → Integration tests

prod → Production (planned)

📌 Development Guidelines
Use DTOs for API communication

Keep entities internal

Prefer pagination for list APIs

Write integration tests for major flows

Commit stable code frequently

📈 Roadmap
Planned Features:

Authentication (JWT)

Multi-tenant SaaS support

School-wise grading system

Attendance module

Timetable

Parent Portal

Mobile-friendly UI

Payment Gateway Integration

👨‍💻 Author
Developed by: Sundaram Tiwari

Backend-focused SaaS platform for Indian schools.

📄 License
This project is currently private and under active development.

---
