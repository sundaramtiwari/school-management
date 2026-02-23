# School Management System — Project Context

Last Updated: Feb 2026

---

## 1. Project Goal

Build a production-grade SaaS School Management System with:

- **Super Admin**: Onboards schools and manages tenants.
- **Schools**: Manage students, teachers, fees, exams, attendance.
- **Strict Multi-Tenancy**: Enforced via JWT-based `schoolId`.
- **Strict Session Isolation**: Academic data is session-scoped.
- **Role-based Auth**: SUPER_ADMIN, PLATFORM_ADMIN, SCHOOL_ADMIN, TEACHER, ACCOUNTANT.

This is designed as a secure, scalable SaaS platform.

---

## 2. Tech Stack

### Backend
- Java 17 / Spring Boot 3.x
- Spring Security (JWT-based RBAC)
- Spring Data JPA / Hibernate
- Hibernate Tenant Filter (school-based isolation)
- Flyway (planned for PostgreSQL migration)
- OpenPDF (Report generation)

### Frontend
- Next.js 14 (App Router)
- TypeScript
- Tailwind CSS (Premium Design System)
- Context-based Global State (Session, Toasts, Modals)

---

## 3. Core Architecture

### Pattern:
Controller → Service → Repository → Entity

### Security Model

- `schoolId` is derived from JWT.
- Client NEVER sends schoolId.
- SUPER_ADMIN bypasses tenant filter.
- All academic modules are strictly school-scoped.

---

## 4. Academic Session Model (STRICT)

The system enforces session-based academic isolation.

### Academic Data = Session Aware:
- Students (via StudentEnrollment)
- Classes
- Exams
- Marks
- Attendance
- Fees
- Fee Assignments

### Non Session Aware:
- School
- Staff
- FeeType
- User / Roles

### StudentEnrollment = Academic Source of Truth

Student entity is permanent.
Academic participation is determined by:

StudentEnrollment

studentId
classId
sessionId
active


All counts and academic queries must use StudentEnrollment.

---

## 5. Fee Module (Enhanced)

### Features:

- FeeStructure is class + session scoped.
- FeeStructure = Template (editable)
  - StudentFeeAssignment = Financial Record (immutable)
  - FeeStructure updates must NEVER modify existing StudentFeeAssignment rows.
- Automatic assignment:
  - On fee creation → assigned to enrolled students.
  - On student enrollment → existing fees assigned.
- Snapshot model:
  - StudentFeeAssignment stores `amount` at assignment time.
  - Future FeeStructure edits do not affect old dues.
- Frequency Support:
  - ONE_TIME (lifetime)
  - ANNUALLY (per session)
  - MONTHLY (aggregated as yearly total for MVP)

### Optimization:
- Removed N+1 queries.
- Aggregated dues calculations.
- Session-aware student filtering.

---

## 6. Exam Lifecycle Enforcement

Strict lifecycle implemented:

DRAFT → PUBLISHED → LOCKED


### Rules:

DRAFT:
- Subjects editable
- Marks editable

PUBLISHED:
- Read-only marks
- No subject edits
- Marksheets allowed

LOCKED:
- Fully immutable
- Finalized state

### Validations Before Publish:

- At least 1 subject
- Active students exist in class/session
- All subjects have maxMarks > 0
- At least some marks entered
- Tenant validation

StudentMark now contains `examId` with unique constraint:
(exam_id, student_id, exam_subject_id)

---

## 7. Parent / Guardian Module

Backend:
- Parent entity exists.
- Student ↔ Parent relationship defined.

Frontend:
- Parent data input UI pending implementation.

Next Immediate Task:
- Add Parent section in Student Create/Edit UI.

---

## 8. Current State

- Running fully in local environment.
- Database: H2 (development mode).
- No production deployment yet.
- Manual flow verification in progress.
- Infrastructure decision pending (AWS vs DigitalOcean).

---

## 9. Key Design Decisions

- Strict session enforcement.
- Strict tenant enforcement.
- No schoolId from client.
- Snapshot-based fee system.
- Exam lifecycle control.
- No premature microservice splitting.
- Optimize N+1 only when necessary.

---

## 10. Next Planned Steps

- Parent UI implementation
- Transport module refinement
- PostgreSQL migration
- Production infra setup
- Billing & subscription system
- Advanced reporting

---

## 11. Long-Term Vision

- Parent portal
- Teacher portal
- Mobile PWA
- Analytics dashboards
- AI-based performance insights

