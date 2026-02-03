# School Management System — Project Context

Last Updated: Jan 2026

---

## 1. Project Goal

Build a SaaS School Management System where:

- Super Admin onboards schools
- Schools manage students, teachers, fees, academics
- Multi-tenant system (school-based isolation)
- Future: role-based auth

---

## 2. Tech Stack

### Backend
- Java 17
- Spring Boot 3.3+ / 3.5+
- Spring Data JPA
- PostgreSQL (prod), H2 (test)
- JWT (planned)
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
- school
- student
- classsubject
- fee
- guardian
- teacher
- common
- auth (planned)


All major entities link to `school_id`.

---

## 4. BaseEntity

All entities extend:

- createdAt
- updatedAt
- createdBy
- updatedBy

---

## 5. Core Entities

### School
- name
- displayName
- board
- medium
- schoolCode (unique)
- address, city, state, pincode
- contactEmail, contactNumber
- website, logoUrl
- active

### Student
- admissionNumber
- firstName, lastName
- dob, gender
- aadhar, pen
- address, contact
- previousSchoolDetails
- currentClass (ManyToOne)
- school (ManyToOne)
- status, active

### SchoolClass
- name
- section
- session
- capacity
- schoolId

### StudentEnrollment
- studentId
- classId
- session
- rollNumber
- active

### PromotionRecord
- studentId
- fromClassId
- toClassId
- fromSection
- toSection
- session
- promotedOn
- feePending

### Fee System
- FeeType
- FeeStructure
- StudentFeeAssignment
- FeePayment

---

## 6. Academic Flow

1. Student registered
2. Student enrolled (StudentEnrollment)
3. Promotion creates:
   - PromotionRecord
   - New Enrollment
4. History preserved

---

## 7. Current API Structure

### Schools
GET /api/schools
POST /api/schools
PATCH /api/schools/{code}
DELETE /api/schools/{id}

### Students
POST /api/students
GET /api/students/{id}
GET /api/students/by-school/{schoolId}
PUT /api/students/{id}
DELETE /api/students/{id}


### Classes
GET /api/classes/by-school/{schoolId}


### Enrollments
POST /api/enrollments
GET /api/enrollments/by-class/{classId}

### Fees
/api/fees/types
/api/fees/structures
/api/fees/assignments
/api/fees/payments


---

## 8. Frontend Status

### Implemented

- Sidebar layout
- Schools page (list/create/edit modal)
- Students page:
  - Select school
  - Select class
  - List students

### In Progress

- Student create + auto enroll
- Student edit UI
- Pagination
- Better validation

### Pending

- Auth UI
- Role-based menus
- Teacher dashboard
- Reports

---

## 9. Authentication (Planned)

Roles:

- SUPER_ADMIN
- SCHOOL_ADMIN
- TEACHER
- ACCOUNTANT

JWT token will include:

userId
schoolId
role

School isolation via token.

---

## 10. Testing

Integration tests:

- StudentFlowIntegrationTest
- PromotionFlowIntegrationTest
- FeeFlowIntegrationTest

Each test:
- Creates data
- Runs flow
- Cleans DB

---

## 11. Key Design Decisions

- No table-per-school (single DB, school_id filter)
- History via enrollment + promotion tables
- No auth in MVP (temporarily)
- Backend-first design
- Pagination everywhere

---

## 12. Known Issues / Tech Debt

- Auth not implemented yet
- Swagger instability (version conflicts)
- No tenant filter yet
- No Redis cache
- No analytics pipeline

---

## 13. Current Phase

Phase: MVP + Frontend

Focus:

- Student flows
- Admin usability
- Reduce manual backend work
- Prepare for auth

---

## 14. Next Planned Steps

1. Finish Students UI
2. Add Teacher module UI
3. Add Auth backend
4. Add User management
5. Lock school context
6. Remove school dropdown

---

## 15. Repository

Main repo contains full code.
All decisions documented here.

This file is the source of truth.
