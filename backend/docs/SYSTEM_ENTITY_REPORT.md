# Comprehensive System Entity Report

Generated on: 2026-02-24
Codebase: `/Users/sundaramtiwari/Documents/work/school-management/backend`
Last verified against code on: 2026-02-26
Verification scope: `src/main/java` + `src/test/java` (finance, expense, fee, dashboard, tenant/security touchpoints)

---

## 1) Entity Inventory

### 1.0 Base classes and inherited fields

- `BaseEntity` (`@MappedSuperclass`)
  - `createdAt: LocalDateTime` (`@CreationTimestamp`, `nullable=false`, auto-populated)
  - `updatedAt: LocalDateTime` (`@UpdateTimestamp`, nullable)
  - `createdBy: Long` (nullable)
  - `updatedBy: Long` (nullable)
- `TenantEntity extends BaseEntity` (`@MappedSuperclass`)
  - `schoolId: Long` (`@Column(name="school_id", nullable=false)`)
  - Tenant filter metadata (`tenantFilter`) attached here

Notes:
- Most entities inherit `TenantEntity` and therefore include `school_id`.
- `School` and `User` extend `BaseEntity` (not `TenantEntity`).

Legend:
- `NN`: Not null
- `NULL`: Nullable
- `UQ`: Unique
- `D=`: Default in entity code (`@Builder.Default` or Java default)

### 1.1 Entity list

#### School domain

##### School
- Class: `School`
- Table: `schools`
- PK: `id (Long, identity)`
- Indexes: none declared in entity
- Unique:
  - `schoolCode` (`@Column(unique=true, nullable=false)`)
- Soft delete flags:
  - `active` (`boolean`, `D=true`)
- Fields:
  - `id: Long (PK, NN)`
  - `name: String (NN)`
  - `displayName: String (NULL)`
  - `board: String (NULL)`
  - `medium: String (NULL)`
  - `schoolCode: String (NN, UQ)`
  - `affiliationCode: String (NULL)`
  - `address: String (NULL)`
  - `city: String (NULL)`
  - `state: String (NULL)`
  - `pincode: String (NULL)`
  - `contactNumber: String (NULL)`
  - `contactEmail: String (NULL)`
  - `website: String (NULL)`
  - `logoUrl: String (NULL)`
  - `description: String (NULL)`
  - `currentSessionId: Long (NULL)`
  - `active: boolean (NN, D=true)`
  - Inherited `BaseEntity` fields

##### AcademicSession
- Class: `AcademicSession`
- Table: `academic_sessions`
- PK: `id (Long, identity)`
- Unique:
  - `@UniqueConstraint(name, school_id)`
- Indexes: none declared in entity
- Soft delete flags:
  - `active` (`boolean`, `D=true`)
- Fields:
  - `id: Long (PK, NN)`
  - `name: String (NN)`
  - `startDate: LocalDate (NULL)`
  - `endDate: LocalDate (NULL)`
  - `active: boolean (NN, D=true)`
  - Inherited `TenantEntity` + `BaseEntity` fields

##### ActivityLog
- Class: `ActivityLog`
- Table: `activity_logs`
- PK: `id (Long, identity)`
- Indexes:
  - `idx_activity_logs_school_timestamp (school_id, timestamp)`
- Soft delete flags: none
- Fields:
  - `id: Long (PK, NN)`
  - `userId: Long (NULL)`
  - `action: String (NULL)`
  - `entityType: String (NULL)`
  - `entityId: Long (NULL)`
  - `ipAddress: String (NULL)`
  - `timestamp: LocalDateTime (NULL)`
  - Inherited `TenantEntity` + `BaseEntity` fields

##### Subscription
- Class: `Subscription`
- Table: `subscriptions`
- PK: `id (Long, identity)`
- Indexes/unique: none declared in entity
- Soft delete flags:
  - `active` (`boolean`, no explicit builder default)
- Fields:
  - `id: Long (PK, NN)`
  - `school: ManyToOne School (LAZY, join `school_id`, read-only)`
  - `planName: String (NN)`
  - `studentLimit: Integer (NULL)`
  - `monthlyPrice: BigDecimal (NULL, precision=15, scale=2)`
  - `startDate: LocalDate (NULL)`
  - `endDate: LocalDate (NULL)`
  - `active: boolean (NN, default Java false unless set)`
  - `autoRenew: boolean (NN, default Java false unless set)`
  - Inherited `TenantEntity` + `BaseEntity` fields

##### SubscriptionPayment
- Class: `SubscriptionPayment`
- Table: `subscription_payments`
- PK: `id (Long, identity)`
- Indexes/unique: none declared in entity
- Soft delete flags: none
- Fields:
  - `id: Long (PK, NN)`
  - `subscription: ManyToOne Subscription (LAZY, join `subscription_id`, NN)`
  - `amountPaid: BigDecimal (NULL, precision=15, scale=2)`
  - `paymentDate: LocalDate (NULL)`
  - `paymentMode: String (NULL)`
  - `transactionReference: String (NULL)`
  - `invoiceUrl: String (NULL)`
  - Inherited `TenantEntity` + `BaseEntity` fields

#### User/teacher domain

##### User
- Class: `User`
- Table: `users`
- PK: `id (Long, identity)`
- Unique:
  - `@UniqueConstraint(name="uk_users_email", columnNames="email")`
- Indexes: none declared in entity
- Soft delete flags:
  - `active` (`boolean`, `D=true`)
- Fields:
  - `id: Long (PK, NN)`
  - `email: String (NN, UQ)`
  - `fullName: String (NULL)`
  - `passwordHash: String (NN)`
  - `role: UserRole (NN, enum string length=50)`
  - `school: ManyToOne School (LAZY, join `school_id`, nullable)`
  - `active: boolean (NN, D=true)`
  - Inherited `BaseEntity` fields

##### Teacher
- Class: `Teacher`
- Table: `teachers`
- PK: `id (Long, identity)`
- Indexes/unique: none declared in entity
- Soft delete flags: none
- Fields:
  - `id: Long (PK, NN)`
  - `user: OneToOne User (LAZY, join `user_id`, nullable)`
  - Inherited `TenantEntity` + `BaseEntity` fields

#### Class/subject domain

##### SchoolClass
- Class: `SchoolClass`
- Table: `school_classes`
- PK: `id (Long, identity)`
- Unique:
  - `uk_school_class_school_name_section_session (school_id, name, section, session_id)`
- Indexes:
  - `idx_schoolclass_schoolid_name_session (school_id, name, session_id)`
- Soft delete flags:
  - `active` (`boolean`, `D=true`)
- Fields:
  - `id: Long (PK, NN)`
  - `name: String (NN)`
  - `section: String (NULL)`
  - `sessionId: Long (NN)`
  - `capacity: Integer (NULL)`
  - `classTeacher: ManyToOne Teacher (LAZY, join `class_teacher_id`, nullable)`
  - `school: ManyToOne School (LAZY, join `school_id`, read-only)`
  - `active: boolean (NN, D=true)`
  - `remarks: String (NULL)`
  - Inherited `TenantEntity` + `BaseEntity` fields

##### Subject
- Class: `Subject`
- Table: `subjects`
- PK: `id (Long, identity)`
- Unique:
  - `uk_subject_school_name (school_id, name)`
- Indexes: none declared in entity
- Soft delete flags:
  - `active` (`boolean`, `D=true`)
- Fields:
  - `id: Long (PK, NN)`
  - `name: String (NN)`
  - `code: String (NULL)`
  - `type: String (NULL)`
  - `maxMarks: Integer (NULL)`
  - `minMarks: Integer (NULL)`
  - `active: boolean (NN, D=true)`
  - `remarks: String (NULL)`
  - Inherited `TenantEntity` + `BaseEntity` fields

##### ClassSubject
- Class: `ClassSubject`
- Table: `class_subjects`
- PK: `id (Long, identity)`
- Unique:
  - `uk_class_subject (class_id, subject_id)`
- Indexes: none declared in entity
- Soft delete flags:
  - `active` (`boolean`, `D=true`)
- Fields:
  - `id: Long (PK, NN)`
  - `schoolClass: ManyToOne SchoolClass (LAZY, join `class_id`, NN)`
  - `subject: ManyToOne Subject (LAZY, join `subject_id`, NN)`
  - `teacher: ManyToOne Teacher (LAZY, join `teacher_id`, nullable)`
  - `displayOrder: Integer (NULL)`
  - `school: ManyToOne School (LAZY, join `school_id`, read-only)`
  - `active: boolean (NN, D=true)`
  - Inherited `TenantEntity` + `BaseEntity` fields

#### Student domain

##### Student
- Class: `Student`
- Table: `students`
- PK: `id (Long, identity)`
- Indexes:
  - `idx_student_school_adm (school_id, admission_number)`
  - `idx_student_school_aadhar (school_id, aadhar_number)`
- Unique: none explicitly declared in entity
- Soft delete flags:
  - `active` (`boolean`, `D=true`)
- Fields:
  - `id: Long (PK, NN)`
  - `admissionNumber: String (NN)`
  - `firstName: String (NN)`
  - `lastName: String (NULL)`
  - `dob: LocalDate (NULL)`
  - `gender: Gender enum (NN)`
  - `pen: String (NULL)`
  - `aadharNumber: String (NULL)`
  - `religion: String (NULL)`
  - `caste: String (NULL)`
  - `category: String (NULL)`
  - `address: String (NULL)`
  - `city: String (NULL)`
  - `state: String (NULL)`
  - `pincode: String (NULL)`
  - `contactNumber: String (NULL)`
  - `email: String (NULL)`
  - `bloodGroup: String (NULL)`
  - `photoUrl: String (NULL)`
  - `dateOfAdmission: LocalDate (NULL)`
  - `dateOfLeaving: LocalDate (NULL)`
  - `reasonForLeaving: String (NULL)`
  - previous-school fields (`previousSchoolName`, `previousSchoolContact`, `previousSchoolAddress`, `previousSchoolBoard`, `previousClass`, `previousYearOfPassing`, `transferCertificateNumber`, `reasonForLeavingPreviousSchool`) all nullable
  - `active: boolean (NN, D=true)`
  - `currentStatus: StudentStatus enum string (NULL, length=20)`
  - `currentClass: ManyToOne SchoolClass (LAZY, join `current_class_id`, nullable)`
  - `school: ManyToOne School (LAZY, join `school_id`, read-only)`
  - `remarks: String (NULL)`
  - Inherited `TenantEntity` + `BaseEntity` fields

##### StudentEnrollment
- Class: `StudentEnrollment`
- Table: `student_enrollments`
- PK: `id (Long, identity)`
- Unique:
  - `uk_student_session (student_id, session_id, active)`
- Indexes:
  - `idx_enroll_student_session (student_id, session_id)`
- Soft delete flags:
  - `active` (`boolean`, `D=true`)
- Fields:
  - `id: Long (PK, NN)`
  - `studentId: Long (NN)`
  - `classId: Long (NN)`
  - `section: String (NULL)`
  - `sessionId: Long (NN)`
  - `rollNumber: Integer (NULL)`
  - `enrollmentDate: LocalDate (NULL)`
  - `startDate: LocalDate (NULL)`
  - `endDate: LocalDate (NULL)`
  - `admissionType: AdmissionType enum (NULL)`
  - `active: boolean (NN, D=true)`
  - `remarks: String (NULL)`
  - Inherited `TenantEntity` + `BaseEntity` fields

##### StudentGuardian
- Class: `StudentGuardian`
- Table: `student_guardians`
- PK: `id (Long, identity)`
- Unique:
  - `uk_student_guardian (student_id, guardian_id)`
- Indexes: none declared in entity
- Soft delete flags: none
- Fields:
  - `id: Long (PK, NN)`
  - `studentId: Long (NN)`
  - `guardianId: Long (NN)`
  - `primaryGuardian: boolean (NN, D=false)`
  - Inherited `TenantEntity` + `BaseEntity` fields

##### Guardian
- Class: `Guardian`
- Table: `guardians`
- PK: `id (Long, identity)`
- Unique:
  - `uk_guardian_school_contact (school_id, contactNumber)`
- Indexes:
  - `idx_guardian_school_aadhar (school_id, aadhar_number)`
- Soft delete flags:
  - `active` (`boolean`, `D=true`)
- Fields:
  - `id: Long (PK, NN)`
  - `name: String (NN)`
  - `aadharNumber: String (NULL)`
  - `relation: String (NULL)`
  - `contactNumber: String (NN)`
  - `email: String (NULL)`
  - `address: String (NULL)`
  - `occupation: String (NULL)`
  - `qualification: String (NULL)`
  - `whatsappEnabled: boolean (NN, D=true)`
  - `school: ManyToOne School (LAZY, join `school_id`, read-only)`
  - `photoUrl: String (NULL)`
  - `active: boolean (NN, D=true)`
  - Inherited `TenantEntity` + `BaseEntity` fields

##### StudentDocument
- Class: `StudentDocument`
- Table: `student_documents`
- PK: `id (Long, identity)`
- Indexes:
  - `idx_student_document_student (student_id)`
- Unique: none
- Soft delete flags: none
- Fields:
  - `id: Long (PK, NN)`
  - `studentId: Long (NN)`
  - `fileType: String (NULL)`
  - `fileName: String (NULL)`
  - `fileUrl: String (NULL)`
  - `uploadedAt: LocalDateTime (NULL)`
  - `remarks: String (NULL)`
  - Inherited `TenantEntity` + `BaseEntity` fields

##### PromotionRecord
- Class: `PromotionRecord`
- Table: `promotion_records`
- PK: `id (Long, identity)`
- Indexes:
  - `idx_promotion_student_target_session (student_id, target_session_id)`
- Unique: none declared
- Soft delete flags: none
- Fields:
  - `id: Long (PK, NN)`
  - `studentId: Long (NN)`
  - `sourceSessionId: Long (NN)`
  - `targetSessionId: Long (NN)`
  - `sourceClassId: Long (NN)`
  - `targetClassId: Long (NN)`
  - `promotionType: PromotionType enum (NN)`
  - `promotedBy: String (NN)`
  - `promotedAt: LocalDateTime (NN)`
  - `remarks: String (NULL)`
  - Inherited `TenantEntity` + `BaseEntity` fields

#### Attendance domain

##### StudentAttendance
- Class: `StudentAttendance`
- Table: `student_attendance`
- PK: `id (Long, identity)`
- Unique:
  - `uk_attendance (school_id, class_id, student_id, attendance_date, session_id)`
- Indexes:
  - `idx_attendance_school_date (school_id, attendance_date)`
  - `idx_attendance_student_date (student_id, attendance_date)`
- Soft delete flags: none
- Fields:
  - `id: Long (PK, NN)`
  - `classId: Long (NN)`
  - `sessionId: Long (NN)`
  - `studentId: Long (NN)`
  - `attendanceDate: LocalDate (NN)`
  - `status: AttendanceStatus enum (NN)`
  - `remarks: String (NULL)`
  - `student: ManyToOne Student (LAZY, join `student_id`, read-only)`
  - Inherited `TenantEntity` + `BaseEntity` fields

#### Fee domain

##### FeeType
- Class: `FeeType`
- Table: `fee_types`
- PK: `id (Long, identity)`
- Unique:
  - `(name, school_id)`
- Indexes: none declared
- Soft delete flags:
  - `active` (`boolean`, `D=true`)
- Fields:
  - `id: Long (PK, NN)`
  - `name: String (NN)`
  - `description: String (NULL)`
  - `active: boolean (NN, D=true)`
  - Inherited `TenantEntity` + `BaseEntity` fields

##### FeeStructure
- Class: `FeeStructure`
- Table: `fee_structures`
- PK: `id (Long, identity)`
- Indexes:
  - `idx_fee_structure_school_class_session (school_id, class_id, session_id)`
- Unique: none declared
- Soft delete flags:
  - `active` (`boolean`, `D=true`)
- Fields:
  - `id: Long (PK, NN)`
  - `classId: Long (NULL)`
  - `sessionId: Long (NN)`
  - `feeType: ManyToOne FeeType (LAZY, join `fee_type_id`, NN)`
  - `amount: BigDecimal (NN, precision=15, scale=2)`
  - `frequency: FeeFrequency enum (NN, D=ONE_TIME)`
  - `dueDayOfMonth: Integer (NULL, D=10)`
  - `active: boolean (NN, D=true)`
  - Inherited `TenantEntity` + `BaseEntity` fields

##### LateFeePolicy
- Class: `LateFeePolicy`
- Table: `late_fee_policies`
- PK: `id (Long, identity)`
- Unique/indexes: none declared
- Soft delete flags:
  - `active` (`boolean`, `D=true`)
- Fields:
  - `id: Long (PK, NN)`
  - `feeStructure: OneToOne FeeStructure (LAZY, join `fee_structure_id`, NN)`
  - `type: LateFeeType enum (NN, D=NONE)`
  - `amountValue: BigDecimal (NULL, precision=15, scale=2, D=0)`
  - `graceDays: Integer (NULL, D=0)`
  - `capValue: BigDecimal (NULL, precision=15, scale=2, D=0)`
  - `capType: LateFeeCapType enum (NN, D=NONE)`
  - `active: boolean (NN, D=true)`
  - Inherited `TenantEntity` + `BaseEntity` fields

##### StudentFeeAssignment
- Class: `StudentFeeAssignment`
- Table: `student_fee_assignments`
- PK: `id (Long, identity)`
- Indexes:
  - `idx_student_fee_student_session (student_id, session_id)`
  - `idx_student_fee_school_session (school_id, session_id, active)`
  - `idx_student_fee_student_active (student_id, active)`
- Unique: none declared
- Soft delete flags:
  - `active` (`boolean`, `D=true`)
- Fields:
  - `id: Long (PK, NN)`
  - `studentId: Long (NN)`
  - `feeStructureId: Long (NN)`
  - `sessionId: Long (NN)`
  - `amount: BigDecimal (NN, precision=19, scale=2)`
  - `sponsorCoveredAmount: BigDecimal (NN, D=0)`
  - `version: Long (@Version)`
  - Snapshot fields:
    - `dueDate: LocalDate (NULL)`
    - `lateFeeType: LateFeeType enum (NULL)`
    - `lateFeeValue: BigDecimal (NULL)`
    - `lateFeeGraceDays: Integer (NULL)`
    - `lateFeeCapType: LateFeeCapType enum (NULL, D=NONE)`
    - `lateFeeCapValue: BigDecimal (NULL)`
  - Tracking fields:
    - `lateFeeApplied: boolean (NN, D=false)`
    - `lateFeeAccrued: BigDecimal (NULL, D=0)`
    - `principalPaid: BigDecimal (NULL, D=0)`
    - `lateFeePaid: BigDecimal (NULL, D=0)`
    - `lateFeeWaived: BigDecimal (NULL, D=0)`
    - `totalDiscountAmount: BigDecimal (NULL, D=0)`
    - `active: boolean (NN, D=true)`
  - Inherited `TenantEntity` + `BaseEntity` fields

##### FeePayment
- Class: `FeePayment`
- Table: `fee_payments`
- PK: `id (Long, identity)`
- Indexes:
  - `idx_fee_payment_student (student_id)`
  - `idx_fee_payment_school_session (school_id, session_id)`
  - `idx_fee_payment_school_date (school_id, payment_date)`
- Unique: none declared
- Soft delete flags: none
- Fields:
  - `id: Long (PK, NN)`
  - `version: Long (@Version)`
  - `studentId: Long (NN)`
  - `sessionId: Long (NULL)`
  - `principalPaid: BigDecimal (NN, D=0)`
  - `lateFeePaid: BigDecimal (NN, D=0)`
  - `paymentDate: LocalDate (NN)`
  - `transactionReference: String (NULL, length=100)`
  - `mode: String (NULL)`
  - `remarks: String (NULL)`
  - Inherited `TenantEntity` + `BaseEntity` fields

##### FeePaymentAllocation
- Class: `FeePaymentAllocation`
- Table: `fee_payment_allocations`
- PK: `id (Long, identity)`
- Indexes:
  - `idx_fee_payment_allocation_payment (fee_payment_id)`
  - `idx_fee_payment_allocation_session (session_id)`
- Unique: none declared
- Soft delete flags: none
- Fields:
  - `id: Long (PK, NN)`
  - `feePaymentId: Long (NN)`
  - `assignmentId: Long (NN)`
  - `feeType: ManyToOne FeeType (LAZY, join `fee_type_id`, NN)`
  - `principalAmount: BigDecimal (NN, D=0, precision=19, scale=2)`
  - `lateFeeAmount: BigDecimal (NN, D=0, precision=19, scale=2)`
  - `sessionId: Long (NN)`
  - Inherited `TenantEntity` + `BaseEntity` fields

##### FeeAdjustment
- Class: `FeeAdjustment`
- Table: `fee_adjustments`
- PK: `id (Long, identity)`
- Indexes:
  - `idx_fee_adjustment_assignment (assignment_id)`
- Unique: none declared
- Soft delete flags: none
- Fields:
  - `id: Long (PK, NN)`
  - `assignmentId: Long (NN)`
  - `type: AdjustmentType enum (NN)`
  - `amount: BigDecimal (NN)`
  - `discountDefinitionId: Long (NULL)`
  - `discountNameSnapshot: String (NULL)`
  - `discountTypeSnapshot: DiscountType enum (NULL)`
  - `discountValueSnapshot: BigDecimal (NULL)`
  - `reason: String (NULL)`
  - `createdByStaff: String (NULL)`
  - Inherited `TenantEntity` + `BaseEntity` fields

##### DiscountDefinition
- Class: `DiscountDefinition`
- Table: `discount_definitions`
- PK: `id (Long, identity)`
- Indexes/unique: none declared
- Soft delete flags:
  - `active` (`boolean`, `D=true`)
- Fields:
  - `id: Long (PK, NN)`
  - `name: String (NN, length=100)`
  - `type: DiscountType enum (NN)`
  - `amountValue: BigDecimal (NN)`
  - `active: boolean (NN, D=true)`
  - Inherited `TenantEntity` + `BaseEntity` fields

##### LateFeeLog
- Class: `LateFeeLog`
- Table: `late_fee_logs`
- PK: `id (Long, identity)`
- Indexes:
  - `idx_late_fee_logs_assignment (assignment_id)`
- Unique: none declared
- Soft delete flags: none
- Fields:
  - `id: Long (PK, NN)`
  - `assignmentId: Long (NN)`
  - `computedAmount: BigDecimal (NN)`
  - `appliedDate: LocalDate (NN)`
  - `reason: String (NULL)`
  - Inherited `TenantEntity` + `BaseEntity` fields

##### StudentFundingArrangement
- Class: `StudentFundingArrangement`
- Table: `student_funding_arrangements`
- PK: `id (Long, identity)`
- Indexes:
  - `idx_funding_student_session_active (student_id, session_id, active)`
- Unique: none declared
- Soft delete flags:
  - `active` (`boolean`, `D=true`)
- Fields:
  - `id: Long (PK, NN)`
  - `studentId: Long (NN)`
  - `sessionId: Long (NN)`
  - `coverageType: FundingCoverageType enum (NN)`
  - `coverageMode: FundingCoverageMode enum (NN)`
  - `coverageValue: BigDecimal (NN)`
  - `validFrom: LocalDate (NULL)`
  - `validTo: LocalDate (NULL)`
  - `active: boolean (NN, D=true)`
  - Inherited `TenantEntity` + `BaseEntity` fields

#### Expense domain

##### ExpenseHead
- Class: `ExpenseHead`
- Table: `expense_heads`
- PK: `id (Long, identity)`
- Unique:
  - `uk_expense_head_school_normalized_name (school_id, normalized_name)`
- Indexes: none declared
- Soft delete flags:
  - `active` (`boolean`, `D=true`)
- Fields:
  - `id: Long (PK, NN)`
  - `name: String (NN, length=150)`
  - `normalizedName: String (NN, length=150)`
  - `description: String (NULL, length=500)`
  - `active: boolean (NN, D=true)`
  - Inherited `TenantEntity` + `BaseEntity` fields

##### ExpenseVoucher
- Class: `ExpenseVoucher`
- Table: `expense_vouchers`
- PK: `id (Long, identity)`
- Unique:
  - `uk_expense_voucher_school_voucher_number (school_id, voucher_number)`
- Indexes:
  - `idx_expense_voucher_school_session (school_id, session_id)`
  - `idx_expense_voucher_school_date (school_id, expense_date)`
- Soft delete flags:
  - `active` (`boolean`, `D=true`)
- Fields:
  - `id: Long (PK, NN)`
  - `version: Long (@Version)`
  - `voucherNumber: String (NN, length=50)`
  - `expenseDate: LocalDate (NN)`
  - `expenseHead: ManyToOne ExpenseHead (LAZY, join `expense_head_id`, NN)`
  - `amount: BigDecimal (NN, precision=19, scale=2)`
  - `paymentMode: ExpensePaymentMode enum (NN, string)`
  - `description: String (NULL, length=500)`
  - `referenceNumber: String (NULL, length=100)`
  - `sessionId: Long (NN)`
  - `createdBy: Long (NN)`
  - `active: boolean (NN, D=true)`
  - Inherited `TenantEntity` + `BaseEntity` fields

#### Finance domain

##### FinanceAccountTransfer
- Class: `FinanceAccountTransfer`
- Table: `finance_account_transfers`
- PK: `id (Long, identity)`
- Unique: none declared
- Indexes:
  - `idx_fin_transfer_school_session_date (school_id, session_id, transfer_date)`
- Soft delete flags: none
- Fields:
  - `id: Long (PK, NN)`
  - `sessionId: Long (NN)`
  - `transferDate: LocalDate (NN)`
  - `amount: BigDecimal (NN, precision=19, scale=2)`
  - `fromAccount: String (NN, length=20)` (service currently enforces `CASH`)
  - `toAccount: String (NN, length=20)` (service currently enforces `BANK`)
  - `referenceNumber: String (NULL, length=100)`
  - `remarks: String (NULL, length=500)`
  - Inherited `TenantEntity` + `BaseEntity` fields

##### DayClosing
- Class: `DayClosing`
- Table: `day_closing`
- PK: `id (Long, identity)`
- Unique:
  - `uk_day_closing_school_date (school_id, date)`
- Indexes: none declared
- Soft delete flags: none
- Fields:
  - `id: Long (PK, NN)`
  - `date: LocalDate (NN)`
  - `sessionId: Long (NN)`
  - `openingCash: BigDecimal (NN, precision=19, scale=2)`
  - `openingBank: BigDecimal (NN, precision=19, scale=2)`
  - `cashRevenue: BigDecimal (NN, precision=19, scale=2)`
  - `bankRevenue: BigDecimal (NN, precision=19, scale=2)`
  - `cashExpense: BigDecimal (NN, precision=19, scale=2)`
  - `bankExpense: BigDecimal (NN, precision=19, scale=2)`
  - `transferOut: BigDecimal (NN, precision=19, scale=2)`
  - `transferIn: BigDecimal (NN, precision=19, scale=2)`
  - `closingCash: BigDecimal (NN, precision=19, scale=2)`
  - `closingBank: BigDecimal (NN, precision=19, scale=2)`
  - `overrideAllowed: boolean (NN, D=false)`
  - `closedBy: Long (NULL)`
  - `closedAt: LocalDateTime (NULL)`
  - Inherited `TenantEntity` + `BaseEntity` fields

#### Transport domain

##### TransportRoute
- Class: `TransportRoute`
- Table: `transport_routes`
- PK: `id (Long, identity)`
- Indexes/unique: none declared
- Soft delete flags:
  - `active` (`boolean`, `D=true`)
- Fields:
  - `id: Long (PK, NN)`
  - `name: String (NN)`
  - `description: String (NULL)`
  - `capacity: Integer (NN, D=30)`
  - `currentStrength: Integer (NN, D=0)`
  - `active: boolean (NN, D=true)`
  - `school: ManyToOne School (LAZY, join `school_id`, read-only)`
  - Inherited `TenantEntity` + `BaseEntity` fields

##### PickupPoint
- Class: `PickupPoint`
- Table: `pickup_points`
- PK: `id (Long, identity)`
- Indexes/unique: none declared
- Soft delete flags: none
- Fields:
  - `id: Long (PK, NN)`
  - `name: String (NN)`
  - `amount: BigDecimal (NN, precision=15, scale=2)`
  - `frequency: FeeFrequency enum (NN)`
  - `route: ManyToOne TransportRoute (LAZY, join `route_id`, NN)`
  - Inherited `TenantEntity` + `BaseEntity` fields

##### TransportEnrollment
- Class: `TransportEnrollment`
- Table: `transport_enrollments`
- PK: `id (Long, identity)`
- Unique:
  - `(student_id, session_id)`
- Indexes:
  - `idx_transport_enrollment_student (student_id, session_id)`
- Soft delete flags:
  - `active` (`boolean`, `D=true`)
- Fields:
  - `id: Long (PK, NN)`
  - `studentId: Long (NN)`
  - `pickupPoint: ManyToOne PickupPoint (LAZY, join `pickup_point_id`, NN)`
  - `sessionId: Long (NN)`
  - `active: boolean (NN, D=true)`
  - `school: ManyToOne School (LAZY, join `school_id`, read-only)`
  - Inherited `TenantEntity` + `BaseEntity` fields

#### Test management domain

##### Exam
- Class: `Exam`
- Table: `exams`
- PK: `id (Long, identity)`
- Indexes:
  - `idx_exam_class_session (class_id, session_id)`
- Unique: none declared
- Soft delete flags:
  - `active` (`boolean`, `D=true`)
- Fields:
  - `id: Long (PK, NN)`
  - `classId: Long (NN)`
  - `sessionId: Long (NN)`
  - `name: String (NN)`
  - `examType: String (NULL)`
  - `startDate: LocalDate (NULL)`
  - `endDate: LocalDate (NULL)`
  - `active: boolean (NN, D=true)`
  - `status: ExamStatus enum (NULL, D=DRAFT)`
  - Inherited `TenantEntity` + `BaseEntity` fields

##### ExamSubject
- Class: `ExamSubject`
- Table: `exam_subjects`
- PK: `id (Long, identity)`
- Unique:
  - `(exam_id, subject_id)`
- Indexes: none declared
- Soft delete flags:
  - `active` (`boolean`, `D=true`)
- Fields:
  - `id: Long (PK, NN)`
  - `examId: Long (NN)`
  - `subjectId: Long (NN)`
  - `maxMarks: Integer (NN)`
  - `active: boolean (NN, D=true)`
  - Inherited `TenantEntity` + `BaseEntity` fields

##### StudentMark
- Class: `StudentMark`
- Table: `student_marks`
- PK: `id (Long, identity)`
- Unique:
  - `(exam_id, student_id, exam_subject_id)`
- Indexes: none declared
- Soft delete flags: none
- Fields:
  - `id: Long (PK, NN)`
  - `examId: Long (NN)`
  - `examSubjectId: Long (NN)`
  - `studentId: Long (NN)`
  - `marksObtained: Integer (NN)`
  - `remarks: String (NULL)`
  - Inherited `TenantEntity` + `BaseEntity` fields

##### GradePolicy
- Class: `GradePolicy`
- Table: `grade_policies`
- PK: `id (Long, identity)`
- Indexes:
  - `idx_grade_school (school_id)`
- Unique: none declared
- Soft delete flags: none
- Fields:
  - `id: Long (PK, NN)`
  - `minPercent: Double (NN)`
  - `maxPercent: Double (NN)`
  - `grade: String (NN)`
  - Inherited `TenantEntity` + `BaseEntity` fields

---

## 2) Relationships and Associations

No entity defines `@OneToMany`, `@ManyToMany`, `mappedBy`, `cascade`, or `orphanRemoval`.
All relationships are owning-side references.

| Source field | Relation | Target | Owning side | mappedBy | Fetch | Cascade | Orphan removal | Join column |
|---|---|---|---|---|---|---|---|---|
| `StudentAttendance.student` | `ManyToOne` | `Student` | yes | n/a | `LAZY` | none | n/a | `student_id` (read-only) |
| `ClassSubject.schoolClass` | `ManyToOne` | `SchoolClass` | yes | n/a | `LAZY` | none | n/a | `class_id` |
| `ClassSubject.subject` | `ManyToOne` | `Subject` | yes | n/a | `LAZY` | none | n/a | `subject_id` |
| `ClassSubject.teacher` | `ManyToOne` | `Teacher` | yes | n/a | `LAZY` | none | n/a | `teacher_id` |
| `ClassSubject.school` | `ManyToOne` | `School` | yes | n/a | `LAZY` | none | n/a | `school_id` (read-only) |
| `SchoolClass.classTeacher` | `ManyToOne` | `Teacher` | yes | n/a | `LAZY` | none | n/a | `class_teacher_id` |
| `SchoolClass.school` | `ManyToOne` | `School` | yes | n/a | `LAZY` | none | n/a | `school_id` (read-only) |
| `Guardian.school` | `ManyToOne` | `School` | yes | n/a | `LAZY` | none | n/a | `school_id` (read-only) |
| `Student.currentClass` | `ManyToOne` | `SchoolClass` | yes | n/a | `LAZY` | none | n/a | `current_class_id` |
| `Student.school` | `ManyToOne` | `School` | yes | n/a | `LAZY` | none | n/a | `school_id` (read-only) |
| `Teacher.user` | `OneToOne` | `User` | yes | n/a | `LAZY` | none | n/a | `user_id` |
| `FeeStructure.feeType` | `ManyToOne` | `FeeType` | yes | n/a | `LAZY` | none | n/a | `fee_type_id` |
| `FeePaymentAllocation.feeType` | `ManyToOne` | `FeeType` | yes | n/a | `LAZY` | none | n/a | `fee_type_id` |
| `LateFeePolicy.feeStructure` | `OneToOne` | `FeeStructure` | yes | n/a | `LAZY` | none | n/a | `fee_structure_id` |
| `ExpenseVoucher.expenseHead` | `ManyToOne` | `ExpenseHead` | yes | n/a | `LAZY` | none | n/a | `expense_head_id` |
| `Subscription.school` | `ManyToOne` | `School` | yes | n/a | `LAZY` | none | n/a | `school_id` (read-only) |
| `SubscriptionPayment.subscription` | `ManyToOne` | `Subscription` | yes | n/a | `LAZY` | none | n/a | `subscription_id` |
| `PickupPoint.route` | `ManyToOne` | `TransportRoute` | yes | n/a | `LAZY` | none | n/a | `route_id` |
| `TransportEnrollment.pickupPoint` | `ManyToOne` | `PickupPoint` | yes | n/a | `LAZY` | none | n/a | `pickup_point_id` |
| `TransportEnrollment.school` | `ManyToOne` | `School` | yes | n/a | `LAZY` | none | n/a | `school_id` (read-only) |
| `TransportRoute.school` | `ManyToOne` | `School` | yes | n/a | `LAZY` | none | n/a | `school_id` (read-only) |
| `User.school` | `ManyToOne` | `School` | yes | n/a | `LAZY` | none | n/a | `school_id` |

---

## 3) Tenancy Model

### 3.1 Which entities contain `schoolId`?

- Directly via `TenantEntity`: all entities except `School` and `User`
  - Includes all fee, student, class, attendance, exam, transport, subscription models.
- `User` does not have scalar `schoolId`; tenancy comes from `users.school_id` via `@ManyToOne School`.
- `School` is global root entity and intentionally non-tenant-scoped.

### 3.2 Which entities use Hibernate filter?

- `TenantEntity` declares:
  - `@FilterDef(name="tenantFilter", parameter schoolId)`
  - `@Filter(name="tenantFilter", condition="school_id = :schoolId")`
- `User` has explicit `@Filter(name="tenantFilter", condition="school_id = :schoolId")`.

### 3.3 Where is `TenantContext` applied?

- Set from JWT claims in `JwtAuthFilter`.
- Optional override from `X-School-Id` for `SUPER_ADMIN`/`PLATFORM_ADMIN`.
- Cleared in `TenantContextCleanupFilter`.
- Filter enabled in `TenantFilterAspect` before service methods and transactional boundaries.
- Explicitly consumed in many services and mappers (`EnrollmentService`, `FeePaymentService`, `StudentService`, `FeeTypeService`, `TransportEnrollmentService`, etc.).

### 3.4 Entities missing `schoolId` that may be concerning

Expected non-tenant entities:
- `School`: root.
- `User`: uses `school` relation + filter.

Potential concern is not entity shape, but call path:
- Controller-level direct repository access can bypass intended service-level ownership checks.

Notable example:
- `/Users/sundaramtiwari/Documents/work/school-management/backend/src/main/java/com/school/backend/fee/controller/StudentFundingArrangementController.java:57`
  - Direct `fundingRepository.findById(id)` in controller before soft deactivate.

---

## 4) Session Model

### 4.1 Session-scoped entities

- `SchoolClass` (`sessionId`)
- `StudentEnrollment` (`sessionId`)
- `StudentAttendance` (`sessionId`)
- `PromotionRecord` (`sourceSessionId`, `targetSessionId`)
- `Exam` (`sessionId`)
- `TransportEnrollment` (`sessionId`)
- `FeeStructure` (`sessionId`)
- `StudentFeeAssignment` (`sessionId`)
- `StudentFundingArrangement` (`sessionId`)
- `FeePayment` (`sessionId`, nullable)
- `FeePaymentAllocation` (`sessionId`)
- `ExpenseVoucher` (`sessionId`)
- `FinanceAccountTransfer` (`sessionId`)
- `DayClosing` (`sessionId`)
- `School.currentSessionId` (selected current session pointer)

### 4.2 Session context plumbing

- `SessionContext` holds current request session id.
- `JwtAuthFilter` reads `X-Session-Id`, validates via `SessionResolver.validateForCurrentSchool()`, stores in `SessionContext`.
- `SessionResolver.resolveForCurrentSchool()` falls back to `School.currentSessionId`.

### 4.3 Session isolation risks

- `FeeSummaryService#getDefaultersPage` computes `effectiveSessionId` but calls repository with raw `sessionId` parameter:
  - `/Users/sundaramtiwari/Documents/work/school-management/backend/src/main/java/com/school/backend/fee/service/FeeSummaryService.java:491-494`
- Multiple repositories expose methods not forcing session scoping; correctness depends on service/controller discipline.

---

## 5) Snapshot Patterns

### 5.1 Snapshotting entities and fields

#### StudentFeeAssignment snapshots template state
- Snapshot fields copied at assignment creation:
  - `amount`
  - `dueDate`
  - `lateFeeType`
  - `lateFeeValue`
  - `lateFeeGraceDays`
  - `lateFeeCapType`
  - `lateFeeCapValue`
  - `sponsorCoveredAmount` (funding snapshot at creation/recalc points)
  - `totalDiscountAmount` (running persisted total)

Source:
- `StudentFeeAssignmentService.assign()` and `FeeStructureService.assignFeeToStudent()`

#### FeeAdjustment snapshots discount definition
- `discountNameSnapshot`
- `discountTypeSnapshot`
- `discountValueSnapshot`

Source:
- `FeeDiscountService.applyDiscount()`

### 5.2 What is being snapshot and why?

- Snapshotting freezes financial meaning at mutation time, so later master-data changes do not retroactively alter historic dues/payments.
- This aligns with financial-record integrity expectations for ledgers and receipts.

### 5.3 Fields that must never be retroactively changed

- On `StudentFeeAssignment`: all snapshot and persisted aggregate fields used for due/pending computation.
- On `FeePayment`: recorded principal/late fee amounts and payment date.
- On `FeeAdjustment`: adjustment amount and discount snapshots.

---

## 6) Mutation Rules (Inferred)

### 6.1 Entities effectively immutable after creation

- `FeePayment` (append-only pattern, no update endpoint)
- `FeePaymentAllocation` (append-only per payment write)
- `FeeAdjustment` (append-only adjustment records)
- `LateFeeLog` (append-only)
- `PromotionRecord` (append-only)
- `FinanceAccountTransfer` (append-only internal movement records)
- `AcademicSession.startDate/endDate` are intentionally immutable after creation (`AcademicSessionService`)
- `DayClosing` is persisted snapshot-per-day; record can be replaced only when override is enabled.

### 6.2 Entities with active flag / toggles

Active flag present in many entities:
- `School`, `AcademicSession`, `User`, `Student`, `StudentEnrollment`, `Guardian`, `Subject`, `SchoolClass`, `ClassSubject`, `FeeType`, `FeeStructure`, `LateFeePolicy`, `StudentFeeAssignment`, `DiscountDefinition`, `StudentFundingArrangement`, `TransportRoute`, `TransportEnrollment`, `Exam`, `ExamSubject`, `Subscription`.
- `ExpenseHead`, `ExpenseVoucher`

Explicit toggle endpoints/services:
- `FeeStructure` (`PATCH /api/fees/structures/{id}/toggle`)
- `FeeType` (`PATCH /api/fees/types/{id}/toggle`)
- `DiscountDefinition` (`PATCH /api/fees/discount-definitions/{id}/toggle`)
- `ExpenseHead` (`PATCH /api/expenses/heads/{id}/toggle-active`)
- `ExpenseVoucher` (`PATCH /api/expenses/{id}/toggle-active`)

### 6.3 Delete semantics currently mixed

Soft-delete style:
- `StudentService.delete()` -> `student.active=false`
- `TransportRouteService.deleteRoute()` -> `route.active=false`
- Funding delete endpoint deactivates arrangement (`active=false`)

Physical delete still present:
- `UserService.deleteUser()` -> repository delete
- `SubjectService.delete()`, `SchoolClassService.delete()`, `ClassSubjectService.delete()`
- `StudentDocumentService.delete()`

Implication:
- Deletion policy is inconsistent across modules.

---

## 7) Critical Business Invariants

### 7.1 Payment allocation and head-wise posting

In current `FeePaymentService.pay()` (head-wise API):
- Request requires explicit assignment allocations (`FeePaymentAllocationRequest` list).
- Each target assignment is locked (`findByIdWithLock`).
- Duplicate assignment rows in same payment are rejected.
- Late fee is accrued at payment time from snapshot policy.
- For each assignment allocation:
  1. Full pending late fee must be cleared first if any is due.
  2. Remaining amount is applied to principal.
- Per-assignment overpayment is rejected.
- `FeePaymentAllocation` rows are persisted and validated to exactly match stored payment principal/late totals.

### 7.2 Discount guard logic

In `FeeDiscountService.applyDiscount()`:
- Discount definition must be active.
- Principal due must be positive.
- Percentage discount capped by principal due.
- Flat discount cannot exceed principal due.
- Post-discount principal cannot go below zero.
- Creates immutable adjustment snapshot row.

### 7.3 Funding snapshot formula

In `FeeCalculationService.calculateFundingSnapshot()`:
- `netAfterDiscount = baseAmount - discountAmount`
- If funding inactive/missing -> `0`
- Validity window enforcement:
  - before `validFrom` -> `0`
  - after `validTo` -> `0`
- `FULL` -> full net coverage
- `PARTIAL`:
  - fixed amount OR percentage of net
  - capped to net

### 7.4 Late fee computation strategy

In `LateFeeCalculator.calculateLateFee()`:
- Handles `NONE`, `FLAT`, `PERCENTAGE`, `DAILY_PERCENTAGE`
- Uses grace days and due date.
- `FLAT`/`PERCENTAGE` are one-time using `lateFeeApplied`.
- Cap applied by `LateFeeCapType`:
  - `FIXED`
  - `PERCENTAGE` of unpaid principal

### 7.5 Withdrawal deactivation rules

In `StudentWithdrawalService.withdrawStudent()`:
- Enrollment row fetched with `PESSIMISTIC_WRITE` (`findByStudentIdAndSessionIdAndSchoolIdForUpdate`)
- On withdrawal:
  - enrollment `active=false`, `endDate` set
  - student `reasonForLeaving`, `dateOfLeaving`, `currentStatus` set
- Future fee assignments only:
  - deactivate if no principal/late-fee paid
  - skip if any payment exists
- Transport unenrollment triggered if active transport enrollment exists.

### 7.6 Additional invariants

- Exactly one active enrollment per student/session:
  - service check + unique constraint `(student_id, session_id, active)`
- One active funding arrangement per student/session:
  - service-level count guard
- Single active session per school:
  - `deactivateOtherActiveSessions`
- Promotion requires all source class/session exams locked:
  - `PromotionService` check using `ExamRepository.countNonLockedBySessionIdAndClassId(...)`
- Transport capacity invariant managed atomically:
  - increment/decrement update queries
- Day-closing lock invariant:
  - fee payments, expense vouchers, and internal transfers are blocked on closed dates unless override is enabled.

---

## 8) Aggregation Logic

### 8.1 Canonical pending formula (appears in multiple places)

```
pending =
    amount
  + lateFeeAccrued
  - totalDiscountAmount
  - sponsorCoveredAmount
  - lateFeeWaived
  - principalPaid
  - lateFeePaid
```

Used in:
- `StudentFeeAssignmentService.calculatePendingFromPersistedValues()`
- `FeeSummaryService` dashboard/top-defaulters/all-defaulters

### 8.2 Key aggregation providers

- `StudentFeeAssignmentRepository`:
  - `sumFinancialTotalsBySchoolAndSession`
  - `sumAssignedGroupedByStudent`
  - `sumFinancialSummaryByStudentGroupedBySession`
- `FeePaymentRepository`:
  - grouped sums by student/session
  - last payment dates
- `FeePaymentAllocationRepository`:
  - fee-head daily summaries
- `ExpenseVoucherRepository`:
  - session/month/day totals and head breakdowns
- `FinanceAccountTransferRepository`:
  - transfer ranges used in net cash/bank adjustments
- Service aggregators:
  - `DashboardStatsService` (`/api/dashboard/daily-cash`)
  - `FinanceReportingService` (`/api/finance/monthly-pl`, `/api/finance/session-pl`)

### 8.3 Aggregation drift and duplication risks

#### Defaulter formula inconsistency (paged vs full)

- Paged defaulters path omits discount and sponsor:
  - `/Users/sundaramtiwari/Documents/work/school-management/backend/src/main/java/com/school/backend/fee/service/FeeSummaryService.java:522-524`
- Full defaulters path includes discount and sponsor:
  - `/Users/sundaramtiwari/Documents/work/school-management/backend/src/main/java/com/school/backend/fee/service/FeeSummaryService.java:642-647`

#### Session parameter bug in paged defaulters

- `effectiveSessionId` is computed, but query call uses raw `sessionId`:
  - `/Users/sundaramtiwari/Documents/work/school-management/backend/src/main/java/com/school/backend/fee/service/FeeSummaryService.java:491-494`

#### Ledger summary formula mismatch

- `StudentLedgerService` pending excludes `lateFeeWaived`:
  - `/Users/sundaramtiwari/Documents/work/school-management/backend/src/main/java/com/school/backend/core/student/service/StudentLedgerService.java:54-58`

#### Repository defaulter SQL formula mismatch

- `StudentRepository` defaulter formula excludes discount/sponsor:
  - `/Users/sundaramtiwari/Documents/work/school-management/backend/src/main/java/com/school/backend/core/student/repository/StudentRepository.java:62-65`

---

## 9) Role and Permission Surface

### 9.1 Security architecture

- Global web security:
  - `/api/auth/**`, swagger docs are open
  - all other routes require authentication (`anyRequest().authenticated()`)
  - method-level authorization with `@PreAuthorize` enabled

### 9.2 `@PreAuthorize` usage summary

High-level pattern:
- Read/report endpoints: often `SCHOOL_ADMIN`, `ACCOUNTANT`, `SUPER_ADMIN`, `PLATFORM_ADMIN`
- Mutations: usually restricted to `SCHOOL_ADMIN` + admin roles
- Teacher-scoped mutations require both role and service-level assignment checks
- Finance module follows stricter split:
  - mutation: school-admin focused (`/api/finance/transfers`, day-closing override super-admin only)
  - reporting/export: `SCHOOL_ADMIN`, `ACCOUNTANT`, `SUPER_ADMIN`

### 9.3 Financial mutation endpoints

- `POST /api/fees/payments`
- `POST /api/fees/assignments`
- `POST /api/fees/assignments/{assignmentId}/discount`
- `POST /api/fees/assignments/{assignmentId}/waive-late-fee`
- `POST /api/fees/structures`
- `PATCH /api/fees/structures/{id}`
- `PATCH /api/fees/structures/{id}/toggle`
- `POST /api/fees/types`
- `PATCH /api/fees/types/{id}`
- `PATCH /api/fees/types/{id}/toggle`
- `POST /api/fees/discount-definitions`
- `PATCH /api/fees/discount-definitions/{id}/toggle`
- `POST /api/fees/funding`
- `DELETE /api/fees/funding/{id}` (soft deactivation behavior)
- `POST /api/expenses`
- `PATCH /api/expenses/{id}/toggle-active`
- `POST /api/expenses/heads`
- `PATCH /api/expenses/heads/{id}/toggle-active`
- `POST /api/finance/transfers`
- `POST /api/finance/day-closing`
- `PATCH /api/finance/day-closing/{date}/override`

Financial reporting/export endpoints (read-only):
- `GET /api/dashboard/daily-cash`
- `GET /api/fees/payments/head-summary`
- `GET /api/finance/monthly-pl`
- `GET /api/finance/session-pl`
- `GET /api/finance/export/daily-cash`
- `GET /api/finance/export/monthly-pl`
- `GET /api/finance/export/session-pl`
- `GET /api/finance/export/expenses`

### 9.4 Endpoints missing explicit method-level role annotation

These still require authentication due global config, but role-specific guard is absent:

- `StudentDocumentController`
  - `POST /api/students/{studentId}/documents`
  - `GET /api/students/{studentId}/documents`
  - `DELETE /api/students/{studentId}/documents/{documentId}`
- `MarksheetController`
  - `GET /api/marksheets/exam/{examId}/student/{studentId}`
  - `GET /api/marksheets/exam/{examId}/student/{studentId}/pdf`
- `TransportRouteController`
  - `GET /api/transport/routes`
- `TransportPointController`
  - `GET /api/transport/pickup-points/route/{routeId}`
- `ClassSubjectController`
  - `GET /api/class-subjects/{id}`
  - `GET /api/class-subjects/by-class/{classId}`
  - `DELETE /api/class-subjects/{id}`

---

## 10) Risk and Design Observations

### 10.1 Architectural strengths

- Strong multi-tenant baseline (`TenantEntity` + filter + context lifecycle).
- Financial model uses persisted snapshots and append-only payment/adjustment logs.
- Concurrency controls where critical:
  - payment allocation locks assignments
  - withdrawal/promotion lock enrollments
  - transport capacity uses atomic updates
- Business guards are explicit in service layer for many sensitive flows.

### 10.2 Potential invariant drift

- Pending/defaulter formulas duplicated and inconsistent across:
  - repository queries
  - `FeeSummaryService`
  - `StudentLedgerService`
- Paged defaulters session mismatch can return incorrect data scope.

### 10.3 Missing active/session filters

- Some list methods rely on caller discipline and may return active+inactive unless explicitly filtered.
- Some controller reads with no role annotation may broaden access among authenticated users.

### 10.4 Ownership/tenant check gaps

- Controller-level direct repository mutation:
  - funding deactivation by `id` in controller without explicit school ownership check.
- Several services rely on filter behavior rather than explicit ownership checks on all operations.

### 10.5 Dynamic calculations that should remain persisted

- Fee challan generation currently references active fee structures and frequencies for display computation.
- If challan needs historical accuracy per generated period, assignment snapshot usage should be preferred over current template values.

### 10.6 Cross-entity coupling risks

- Student withdrawal touches:
  - `StudentEnrollment`, `Student`, `StudentFeeAssignment`, transport enrollment.
- Transport enrollment couples transport seat counts with fee assignment creation/reactivation.
- Deletion strategy is mixed (hard delete vs soft deactivate), increasing historical consistency risk.

---

## Diagram (Text)

```text
School
  ├── AcademicSession (active, currentSessionId pointer on School)
  ├── User (role-based access)
  ├── SchoolClass (session scoped)
  │     └── ClassSubject (Subject + optional Teacher assignment)
  ├── Student
  │     ├── StudentEnrollment (session scoped, active lifecycle)
  │     ├── StudentAttendance (session/date scoped)
  │     ├── PromotionRecord (source/target session,class)
  │     └── StudentDocument
  ├── FeeType
  │     └── FeeStructure (session/class template, active)
  │           └── LateFeePolicy (template policy)
  ├── StudentFeeAssignment (snapshot financial record per student/session)
  │     ├── FeeAdjustment (discount/waiver snapshots)
  │     └── LateFeeLog
  ├── FeePayment (append-only financial transactions)
  │     └── FeePaymentAllocation (per-head principal/late split)
  ├── StudentFundingArrangement (session scoped coverage rules)
  ├── ExpenseHead
  │     └── ExpenseVoucher (session/date scoped expense entries)
  ├── FinanceAccountTransfer (CASH→BANK movement, session/date scoped)
  ├── DayClosing (daily financial snapshot/lock with override)
  ├── TransportRoute
  │     └── PickupPoint
  │           └── TransportEnrollment (session scoped, active lifecycle)
  └── Exam / ExamSubject / StudentMark / GradePolicy
```

---

## Appendix: Enumerations that materially impact calculations

- `FeeFrequency`: `MONTHLY`, `QUARTERLY`, `HALF_YEARLY`, `ANNUALLY`, `ONE_TIME`
- `LateFeeType`: `NONE`, `FLAT`, `PERCENTAGE`, `DAILY_PERCENTAGE`
- `LateFeeCapType`: `NONE`, `FIXED`, `PERCENTAGE`
- `FundingCoverageType`: `FULL`, `PARTIAL`
- `FundingCoverageMode`: `FIXED_AMOUNT`, `PERCENTAGE`
- `ExpensePaymentMode`: `CASH`, `BANK`, `UPI`
- `StudentStatus`: `ENROLLED`, `PASSED_OUT`, `LEFT`, `SUSPENDED`, `WITHDRAWN`

---

## Audit-focused recommendations (non-invasive)

1. Centralize pending/defaulter formula in one shared component and consume from all summary/defaulter paths.
2. Fix `FeeSummaryService#getDefaultersPage` to pass `effectiveSessionId` consistently.
3. Add explicit `@PreAuthorize` on currently authenticated-only sensitive endpoints.
4. Move controller-level repository mutation (funding deactivate) into service with explicit tenant ownership guard.
5. Standardize delete policy for master data linked to historical records (prefer soft delete).
