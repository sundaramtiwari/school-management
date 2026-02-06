# School Management System (SaaS)

A premium, full-stack School Management System built using **Spring Boot 3 (Backend)** and **Next.js 14 (Frontend)**. This platform is designed as a secure, multi-tenant SaaS for managing institutional operations including academics, finance, and administration.

---

## 🎨 Design Philosophy: Premium UI/UX

This project features a custom-built, modern design system:
- **Rich Aesthetics**: Vibrant colors, sleek dark modes, and high-contrast layouts.
- **Glassmorphism & Micro-animations**: Subtle hover effects and smooth transitions for a high-end feel.
- **Robust Feedback**: Integrated global Toast notifications and professional Modal systems.
- **Skeleton Screens**: Every loading state is handled with skeleton loaders for perceived performance.

---

## 🚀 Tech Stack

### Backend
- **Java 17 / Spring Boot 3.x**
- **Security**: Spring Security with JWT (Strict tenant isolation via JWT-derived `schoolId`).
- **Data**: Spring Data JPA & Hibernate (PostgreSQL).
- **Automation**: Flyway for database versioning.
- **Documentation**: OpenAPI / Swagger.

### Frontend
- **Next.js 14 (App Router)**
- **Tailwind CSS**: Utility-first styling with custom premium design tokens.
- **State Management**: React Context for global UI (Modals, Toasts).
- **Type Safety**: TypeScript.

---

## 📁 Project Structure

```text
school-management/
├── backend/  → Spring Boot application (REST API)
└── frontend/ → Next.js application (Management Portal)
```

---

## ✅ Core Features

### 🏛️ Institutional Management
- **School Onboarding**: Global administration for multi-school setups.
- **Academic Sessions**: Managed timelines with standard session selectors.
- **RBAC**: Role-Based Access Control (SUPER_ADMIN, SCHOOL_ADMIN, TEACHER, etc.).

### 🎓 Academic Operations
- **Student Roster**: Full lifecycle management (Admission, Promotion).
- **Attendance**: High-performance bulk marking with 50-student pagination and persistence.
- **Marksheets**: Exam cycle management and official PDF report card generation.

### 💰 Finance & Billing
- **Fee Structures**: Granular fee head configuration per class/session.
- **Collection**: Digital and cash payment recording with transactional auditing.
- **Documents**: Auto-generated Fee Challans and Payment Receipts.

---

## ⚙️ Setup Instructions

### ☕ Backend
1. **Requirements**: Java 17+, Gradle.
2. **Run**:
   ```bash
   cd backend
   ./gradlew bootRun
   ```
   API: `http://localhost:8080/swagger-ui.html`

### ⚛️ Frontend
1. **Requirements**: Node.js 18+.
2. **Run**:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   Portal: `http://localhost:3000`

---

## 🧪 Testing & Quality
- **Integration Tests**: Multi-tenant isolation verified with H2/RestAssured.
- **UI Audit**: Comprehensive polish verified across all management flows.

---

## 👨‍💻 Author
**Developed by: Sundaram Tiwari**  
A state-of-the-art SaaS platform for the modern Indian educational landscape.

---

## 📄 License
This project is private and proprietary. All rights reserved.
