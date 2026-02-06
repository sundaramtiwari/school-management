# School Management System — Project Context

Last Updated: Feb 2026

---

## 1. Project Goal

Build a SaaS School Management System where:

- Super Admin onboards schools
- Schools manage students, teachers, fees, academics
- Multi-tenant system (school-based isolation)
- Role-based Auth (SUPER_ADMIN, PLATFORM_ADMIN, etc.)

---

## 2. Tech Stack

### Backend
- Java 17
- Spring Boot 3.x
- Spring Data JPA
- PostgreSQL (prod), H2 (test)
- Tenant Isolation (Hibernate Filter + AOP)
- Swagger (springdoc)

### Frontend
- Next.js (App Router)
- Tailwind CSS
- Axios API client

### Build
- Gradle
- Flyway migrations

---

## 3. Architecture

### Pattern:
Controller → Service → Repository → Entity

### Modules:
- **school**: Schools, Academic Sessions
- **user**: Staff, Roles
- **core**: Students, Enrollment, Attendance
- **fee**: Fee structures, Collection, Challans
- **common**: Tenant filters, Base entities

All major entities link to `school_id` and are filtered automatically.

---

## 4. BaseEntity
All entities extend `TenantEntity` or `BaseEntity`:
- createdAt / updatedAt
- schoolId (Multi-tenancy)

---

## 5. Core Entities

### AcademicSession [NEW]
- name (e.g., "2024-25")
- startDate / endDate
- isCurrent (Boolean flag)

### Student
- admissionNumber, firstName, lastName
- currentClass (ManyToOne)
- school (ManyToOne)

### SchoolClass
- name, section, session (Linked to AcademicSession)

### Attendance [NEW]
- studentId
- date
- status (PRESENT, ABSENT, etc.)
- schoolId

---

## 7. Current API Structure

### Sessions
/api/academic-sessions (CRUD)

### Attendance
/api/attendance/bulk (Batch marking)
/api/attendance/class/{id}

### Fees
- Receipt PDF generation via `FeeReceiptService`

---

## 8. Frontend Status

### Implemented
- **Sessions**: Management page + reusable `SessionSelect`.
- **Attendance**: Paginated roster (50/page), bulk present/absent marking.
- **Staff**: Role-based creation (SUPER_ADMIN can create PLATFORM_ADMIN).
- **Schools**: Dashboard with deletion support for admins.

---

## 11. Key Design Decisions
- **Tenant Isolation**: Implemented via `@Filter` and `TenantFilterAspect`.
- **Standardized Sessions**: Replaced manual text inputs with selected session IDs to prevent data corruption.
- **Attendance Persistence**: Uses a global `attendanceMap` to handle state across paginated views.

---

## 12. Known Issues / Tech Debt
- **FeeChallanService.java**: Unresolved `com.lowagie` imports causing lint errors.
- **Auth**: Frontend auth guards are basic; JWT session persistence needs work.

---

## 14. Next Planned Steps
1. Resolve PDF service lint errors.
2. Implement Marksheet calculation logic.
3. Enhance Reports module.
