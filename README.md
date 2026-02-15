# School Management System (SaaS)

A secure, multi-tenant School Management SaaS built using:

- Spring Boot 3 (Backend)
- Next.js 14 (Frontend)
- JWT-based tenant isolation
- Strict session-aware academic model

This system is designed for scalable institutional management in India.

---

## 🎯 Core Philosophy

- Strict tenant isolation (school-based)
- Strict academic session enforcement
- Clean service architecture
- Production-ready validation logic
- Premium UI/UX

---

## 🚀 Tech Stack

### Backend
- Java 17
- Spring Boot 3.x
- Spring Security (JWT)
- Hibernate + JPA
- OpenPDF
- H2 (Dev) → PostgreSQL (Planned)

### Frontend
- Next.js 14 (App Router)
- TypeScript
- Tailwind CSS
- Context API for global UI state

---

## 🏗 Architecture

Controller → Service → Repository → Entity


Multi-tenancy:
- schoolId derived from JWT
- SUPER_ADMIN bypass supported
- No cross-tenant data exposure

Session isolation:
- Academic data filtered by sessionId
- StudentEnrollment is source of academic truth

---

## ✅ Implemented Modules

### 🏫 School & RBAC
- Multi-school onboarding
- Role-based access control
- JWT authentication

### 🎓 Students & Academics
- Student lifecycle
- Enrollment per session
- Attendance (bulk marking)
- Exam Management
- Marksheets (PDF generation)

### 💰 Fee System
- Fee Structures per class/session
- Automatic student assignment
- Snapshot-based dues
- Payment tracking
- Defaulter reporting

### 📝 Exam Lifecycle
Strict enforcement:

DRAFT → PUBLISHED → LOCKED

- Publish validation checks
- Lock enforcement
- Read-only protection
- Unique examId-based StudentMark constraint

---

## 🧪 Local Setup

### Backend

```bash
cd backend
./gradlew bootRun

Swagger:
http://localhost:8080/swagger-ui.html

Frontend

cd frontend
npm install
npm run dev

Portal:
http://localhost:3000

---

## 🧪 Testing & Quality

- Integration tests with H2 + RestAssured
- Multi-tenant isolation verified
- UI audit completed
- Performance optimizations applied

---

## 📌 Current Status

Fully functional locally

H2 development database

Manual verification ongoing

No production deployment yet

Infra planning phase (AWS vs DO)

## 🔜 Upcoming Work

Parent/Guardian UI integration

Transport module refinement

PostgreSQL migration

Production infrastructure setup

Subscription & billing

Advanced analytics

---

## 👨‍💻 Author

Sundaram Tiwari

---

## 📄 License

Private & proprietary. All rights reserved.
