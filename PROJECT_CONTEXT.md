# School Management System — Project Context

Last Updated: Feb 2026

---

## 1. Project Goal

Build a SaaS School Management System where:

- **Super Admin**: Onboards schools and has global access across all tenants.
- **Schools**: Manage students, teachers, fees, academics with strict tenant isolation.
- **Multi-tenant system**: School-based isolation enforced via JWT and Hibernate filters.
- **Role-based Auth**: `SUPER_ADMIN`, `SCHOOL_ADMIN`, `TEACHER`, `ACCOUNTANT`, etc.

---

## 2. Tech Stack

### Backend
- **Java 17 / Spring Boot 3.x**
- **Spring Security**: RBAC + JWT-based school identification.
- **Spring Data JPA / Hibernate**: Tenant Isolation via `@Filter` + `SecurityUtil`.
- **Flyway**: Managed database migrations.

### Frontend
- **Next.js 14+ (App Router)**
- **Tailwind CSS**: Custom "Premium" design tokens.
- **Global UI State**: Context-based Toast system, Modal system, and Breadcrumbs.

---

## 3. Architecture

### Pattern:
Controller → Service → Repository → Entity

### Security Layer (RBAC & Multi-tenancy)
- **JWT Context**: Every request must carry a JWT. The `schoolId` is extracted from the JWT in the backend, not provided by the client (prevents cross-tenant IDOR).
- **Global Admin**: `SUPER_ADMIN` bypasses tenant filters to manage all institutions.

### Modules:
- **school**: Schools, Academic Sessions.
- **user**: Staff, Roles, RBAC.
- **core**: Students, Enrollment, Attendance (Bulk processing).
- **fee**: Fee structures, Collection, Challans, Transaction auditing.
- **marksheets**: Exam cycles, Result calculation, PDF report cards.

---

## 4. UI/UX Framework

The frontend uses a standardized "Premium" aesthetic:
- **Navigation**: Sidebar with dynamic active states + Global Breadcrumbs.
- **Feedback**: 
    - `useToast`: Non-blocking global notifications.
    - `Modal`: Standardized dialogs for forms and confirmations.
    - `Skeleton`: Visual feedback for all async loading states.
- **Typography**: Bold, high-contrast layouts with modern sans-serif fonts.

---

## 5. Core Entities

### AcademicSession
- `name` (e.g., "2024-25"), `startDate`, `endDate`, `isCurrent`.

### Student
- `admissionNumber`, `firstName`, `lastName`.
- `currentClass` (Linked to `SchoolClass`).
- Automatic `schoolId` assignment via JWT.

### SchoolClass
- Linked to `AcademicSession`.

### Attendance
- Bulk marking logic with 50-student-per-page pagination.
- Persistence across pages via frontend `attendanceMap`.

---

## 8. Frontend Status

### Implemented
- **Dashboard**: Institutional overview with real-time summary stats (Schools, Students, Collections).
- **Fees**: Dashboard, Structure Configuration, and Transactional Collection flows.
- **Attendance**: Global roster with bulk status toggles.
- **Staff/Management**: Role-aware user creation and class/session configuration.
- **Marksheets**: PDF generation for student report cards.

---

## 11. Key Design Decisions
- **Strict School Context**: Removed `schoolId` from client parameters. Backend now uses `SecurityUtil.schoolId()` for all queries. (Secure SaaS model).
- **Unified Navigation**: Centralized `ClientLayout` handles Sidebar and Breadcrumbs.
- **Async Feedback**: All data-fetching components MUST implement `<TableSkeleton />`.

---

## 12. Known Issues / Resolved
- **PDF Logic**: Resolved `com.lowagie` dependency issues (OpenPDF).
- **Tenant Leakage**: Fixed potential leakage by enforcing JWT-based filtering.

---

## 14. Next Planned Steps
- Parent/Guardian Portal.
- Advanced Financial Reporting (Excel/CSV exports).
- Mobile Application (PWA).
