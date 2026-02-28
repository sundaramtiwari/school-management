package com.school.backend.devtools.seeder;

import com.school.backend.common.enums.ExpensePaymentMode;
import com.school.backend.common.enums.FeeFrequency;
import com.school.backend.common.enums.LateFeeCapType;
import com.school.backend.common.enums.LateFeeType;
import com.school.backend.common.enums.UserRole;
import com.school.backend.common.tenant.SessionContext;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.core.classsubject.entity.SchoolClass;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import com.school.backend.core.student.entity.StudentEnrollment;
import com.school.backend.core.student.repository.StudentEnrollmentRepository;
import com.school.backend.expense.dto.ExpenseHeadCreateRequest;
import com.school.backend.expense.entity.ExpenseHead;
import com.school.backend.expense.entity.ExpenseVoucher;
import com.school.backend.expense.repository.ExpenseHeadRepository;
import com.school.backend.expense.repository.ExpenseVoucherRepository;
import com.school.backend.expense.service.ExpenseService;
import com.school.backend.fee.dto.FeePaymentAllocationRequest;
import com.school.backend.fee.dto.FeePaymentRequest;
import com.school.backend.fee.dto.StudentFeeAssignRequest;
import com.school.backend.fee.entity.FeePayment;
import com.school.backend.fee.entity.FeeStructure;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.fee.entity.LateFeePolicy;
import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.fee.repository.FeePaymentRepository;
import com.school.backend.fee.repository.FeeStructureRepository;
import com.school.backend.fee.repository.FeeTypeRepository;
import com.school.backend.fee.repository.LateFeePolicyRepository;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import com.school.backend.fee.service.FeePaymentService;
import com.school.backend.fee.service.StudentFeeAssignmentService;
import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.entity.School;
import com.school.backend.school.repository.AcademicSessionRepository;
import com.school.backend.school.repository.SchoolRepository;
import com.school.backend.user.entity.User;
import com.school.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class FinanceVerificationSeeder {

    private static final String MARKER_PREFIX = "PHASE2-FINANCE";
    private static final String MODE_CASH = "CASH";
    private static final String MODE_BANK = "BANK";

    private final SchoolRepository schoolRepository;
    private final AcademicSessionRepository academicSessionRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final FeeTypeRepository feeTypeRepository;
    private final FeeStructureRepository feeStructureRepository;
    private final LateFeePolicyRepository lateFeePolicyRepository;
    private final StudentFeeAssignmentRepository studentFeeAssignmentRepository;
    private final FeePaymentRepository feePaymentRepository;
    private final StudentFeeAssignmentService studentFeeAssignmentService;
    private final FeePaymentService feePaymentService;
    private final ExpenseHeadRepository expenseHeadRepository;
    private final ExpenseVoucherRepository expenseVoucherRepository;
    private final ExpenseService expenseService;
    private final UserRepository userRepository;

    @Transactional
    public void seed() {
        List<School> schools = schoolRepository.findAll().stream()
                .sorted(Comparator.comparing(School::getId))
                .toList();
        for (School school : schools) {
            seedForSchool(school);
        }
    }

    private void seedForSchool(School school) {
        Long schoolId = school.getId();
        Optional<AcademicSession> activeSessionOpt = academicSessionRepository
                .findFirstBySchoolIdAndActiveTrueOrderByStartDateDesc(schoolId);
        if (activeSessionOpt.isEmpty()) {
            return;
        }

        AcademicSession activeSession = activeSessionOpt.get();
        Long sessionId = activeSession.getId();

        TenantContext.setSchoolId(schoolId);
        SessionContext.setSessionId(sessionId);
        try {
            List<SchoolClass> sessionClasses = schoolClassRepository
                    .findBySchoolIdAndSessionIdAndActiveTrue(schoolId, sessionId, PageRequest.of(0, 200))
                    .getContent();
            if (sessionClasses.size() < 2) {
                return;
            }

            Map<String, FeeType> feeTypes = ensureFeeTypes(schoolId);
            List<SchoolClass> baseClasses = sessionClasses.stream()
                    .sorted(Comparator.comparing(SchoolClass::getId))
                    .limit(2)
                    .toList();
            Optional<SchoolClass> nurseryClassOpt = sessionClasses.stream()
                    .filter(c -> "Nursery".equalsIgnoreCase(c.getName()))
                    .findFirst();

            List<SchoolClass> structureTargetClasses = new ArrayList<>(baseClasses);
            nurseryClassOpt.ifPresent(nurseryClass -> {
                boolean alreadyIncluded = structureTargetClasses.stream()
                        .anyMatch(c -> c.getId().equals(nurseryClass.getId()));
                if (!alreadyIncluded) {
                    structureTargetClasses.add(nurseryClass);
                }
            });

            Map<Long, Map<String, FeeStructure>> structureByClassId = ensureFeeStructuresAndPolicies(
                    schoolId, sessionId, feeTypes, structureTargetClasses);

            nurseryClassOpt.ifPresent(nurseryClass -> seedNurseryAssignmentsAndPayments(
                    schoolId, sessionId, nurseryClass, structureByClassId.get(nurseryClass.getId())));

            seedExpenseHeadsAndVouchers(schoolId, sessionId);
        } catch (Exception e) {
            log.warn("Finance verification seeding failed for school {}: {}", schoolId, e.getMessage());
        } finally {
            SessionContext.clear();
            TenantContext.clear();
        }
    }

    private Map<String, FeeType> ensureFeeTypes(Long schoolId) {
        Map<String, String> required = Map.of(
                "Tuition", "Tuition Fee",
                "Transport", "Transport Fee",
                "Exam Fee", "Exam Fee");

        Map<String, FeeType> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : required.entrySet()) {
            String name = entry.getKey();
            FeeType type = feeTypeRepository.findByNameAndSchoolId(name, schoolId)
                    .orElseGet(() -> feeTypeRepository.saveAndFlush(FeeType.builder()
                            .name(name)
                            .description(entry.getValue())
                            .active(true)
                            .schoolId(schoolId)
                            .build()));
            result.put(name, type);
        }
        return result;
    }

    private Map<Long, Map<String, FeeStructure>> ensureFeeStructuresAndPolicies(
            Long schoolId,
            Long sessionId,
            Map<String, FeeType> feeTypes,
            List<SchoolClass> classes) {
        Map<Long, Map<String, FeeStructure>> result = new LinkedHashMap<>();
        for (SchoolClass schoolClass : classes) {
            List<FeeStructure> existing = feeStructureRepository
                    .findByClassIdAndSessionIdAndSchoolId(schoolClass.getId(), sessionId, schoolId);

            Map<String, FeeStructure> byType = new LinkedHashMap<>();
            byType.put("Tuition",
                    ensureStructure(existing, schoolId, sessionId, schoolClass.getId(), feeTypes.get("Tuition"),
                            new BigDecimal("1200.00"), FeeFrequency.ONE_TIME));
            byType.put("Transport",
                    ensureStructure(existing, schoolId, sessionId, schoolClass.getId(), feeTypes.get("Transport"),
                            new BigDecimal("500.00"), FeeFrequency.ONE_TIME));
            byType.put("Exam Fee",
                    ensureStructure(existing, schoolId, sessionId, schoolClass.getId(), feeTypes.get("Exam Fee"),
                            new BigDecimal("300.00"), FeeFrequency.ONE_TIME));

            for (FeeStructure structure : byType.values()) {
                ensureLateFeePolicy(structure);
            }
            result.put(schoolClass.getId(), byType);
        }
        return result;
    }

    private FeeStructure ensureStructure(
            List<FeeStructure> existing,
            Long schoolId,
            Long sessionId,
            Long classId,
            FeeType feeType,
            BigDecimal amount,
            FeeFrequency frequency) {
        return existing.stream()
                .filter(fs -> fs.getFeeType() != null && fs.getFeeType().getId().equals(feeType.getId()))
                .filter(fs -> fs.getAmount() != null && fs.getAmount().compareTo(amount) == 0)
                .filter(fs -> fs.getFrequency() == frequency)
                .findFirst()
                .orElseGet(() -> feeStructureRepository.saveAndFlush(FeeStructure.builder()
                        .schoolId(schoolId)
                        .sessionId(sessionId)
                        .classId(classId)
                        .feeType(feeType)
                        .amount(amount)
                        .frequency(frequency)
                        .dueDayOfMonth(10)
                        .active(true)
                        .build()));
    }

    private void ensureLateFeePolicy(FeeStructure structure) {
        if (lateFeePolicyRepository.findByFeeStructureId(structure.getId()).isPresent()) {
            return;
        }
        lateFeePolicyRepository.saveAndFlush(LateFeePolicy.builder()
                .schoolId(structure.getSchoolId())
                .feeStructure(structure)
                .type(LateFeeType.FLAT)
                .amountValue(new BigDecimal("50.00"))
                .graceDays(0)
                .capType(LateFeeCapType.NONE)
                .capValue(BigDecimal.ZERO)
                .active(true)
                .build());
    }

    private void seedNurseryAssignmentsAndPayments(
            Long schoolId,
            Long sessionId,
            SchoolClass nurseryClass,
            Map<String, FeeStructure> nurseryStructures) {
        if (nurseryStructures == null || nurseryStructures.isEmpty()) {
            return;
        }
        List<StudentEnrollment> nurseryEnrollments = studentEnrollmentRepository
                .findByClassIdAndSessionId(nurseryClass.getId(), sessionId);
        if (nurseryEnrollments.isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate feesOnlyDate = today.minusDays(1);

        for (StudentEnrollment enrollment : nurseryEnrollments) {
            Long studentId = enrollment.getStudentId();
            ensureAssignment(studentId, sessionId, nurseryStructures.get("Tuition").getId(), today.minusDays(12));
            ensureAssignment(studentId, sessionId, nurseryStructures.get("Transport").getId(), today.plusDays(5));
            ensureAssignment(studentId, sessionId, nurseryStructures.get("Exam Fee").getId(), today.plusDays(15));
        }

        if (nurseryEnrollments.size() < 3) {
            return;
        }

        Long studentA = nurseryEnrollments.get(0).getStudentId();
        Long studentB = nurseryEnrollments.get(1).getStudentId();
        Long studentC = nurseryEnrollments.get(2).getStudentId();

        String caseARef = MARKER_PREFIX + "-CASE-A-" + schoolId + "-" + sessionId;
        if (!paymentExists(studentA, sessionId, schoolId, caseARef)) {
            BigDecimal amount = computeCaseAAmount(studentA, sessionId);
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                createPayment(studentA, sessionId, amount, MODE_CASH, today, caseARef,
                        "Case A multi-head cash [Heads: Tuition, Transport]");
            }
        }

        String caseBRef = MARKER_PREFIX + "-CASE-B-" + schoolId + "-" + sessionId;
        if (!paymentExists(studentB, sessionId, schoolId, caseBRef)) {
            BigDecimal amount = computeSmallHeadAmount(studentB, sessionId, new BigDecimal("120.00"));
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                createPayment(studentB, sessionId, amount, MODE_BANK, today, caseBRef,
                        "Case B single-head bank [Head: Exam Fee]");
            }
        }

        String caseCRef = MARKER_PREFIX + "-CASE-C-" + schoolId + "-" + sessionId;
        if (!paymentExists(studentC, sessionId, schoolId, caseCRef)) {
            BigDecimal amount = computeSmallHeadAmount(studentC, sessionId, new BigDecimal("80.00"));
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                createPayment(studentC, sessionId, amount, MODE_CASH, feesOnlyDate, caseCRef,
                        "Case C partial payment [Head: Tuition]");
            }
        }
    }

    private void ensureAssignment(Long studentId, Long sessionId, Long feeStructureId, LocalDate dueDate) {
        boolean exists = studentFeeAssignmentRepository.findByStudentIdAndFeeStructureIdAndSessionIdAndSchoolId(
                studentId, feeStructureId, sessionId, TenantContext.getSchoolId()).isPresent();
        if (exists) {
            return;
        }
        StudentFeeAssignRequest req = new StudentFeeAssignRequest();
        req.setStudentId(studentId);
        req.setSessionId(sessionId);
        req.setFeeStructureId(feeStructureId);
        req.setDueDate(dueDate);
        studentFeeAssignmentService.assign(req);
    }

    private BigDecimal computeCaseAAmount(Long studentId, Long sessionId) {
        List<StudentFeeAssignment> assignments = studentFeeAssignmentRepository
                .findByStudentIdAndSessionId(studentId, sessionId)
                .stream()
                .filter(StudentFeeAssignment::isActive)
                .sorted(Comparator.comparing(a -> a.getDueDate() != null ? a.getDueDate() : LocalDate.MAX))
                .toList();

        List<BigDecimal> pendingDues = new ArrayList<>();
        for (StudentFeeAssignment assignment : assignments) {
            BigDecimal principalDue = nz(assignment.getAmount())
                    .subtract(nz(assignment.getPrincipalPaid()))
                    .subtract(nz(assignment.getTotalDiscountAmount()));
            if (principalDue.compareTo(BigDecimal.ZERO) < 0) {
                principalDue = BigDecimal.ZERO;
            }
            BigDecimal lateDue = nz(assignment.getLateFeeAccrued())
                    .subtract(nz(assignment.getLateFeePaid()))
                    .subtract(nz(assignment.getLateFeeWaived()));
            if (lateDue.compareTo(BigDecimal.ZERO) < 0) {
                lateDue = BigDecimal.ZERO;
            }
            BigDecimal totalDue = principalDue.add(lateDue);
            if (totalDue.compareTo(BigDecimal.ZERO) > 0) {
                pendingDues.add(totalDue);
            }
            if (pendingDues.size() >= 2) {
                break;
            }
        }
        if (pendingDues.isEmpty()) {
            return BigDecimal.ZERO;
        }
        if (pendingDues.size() == 1) {
            return pendingDues.get(0).min(new BigDecimal("200.00"));
        }
        BigDecimal first = pendingDues.get(0);
        BigDecimal second = pendingDues.get(1);
        return first.add(second.min(new BigDecimal("150.00")));
    }

    private BigDecimal computeSmallHeadAmount(Long studentId, Long sessionId, BigDecimal cap) {
        List<StudentFeeAssignment> assignments = studentFeeAssignmentRepository
                .findByStudentIdAndSessionId(studentId, sessionId)
                .stream()
                .filter(StudentFeeAssignment::isActive)
                .sorted(Comparator.comparing(a -> a.getDueDate() != null ? a.getDueDate() : LocalDate.MAX))
                .toList();
        for (StudentFeeAssignment assignment : assignments) {
            BigDecimal principalDue = nz(assignment.getAmount())
                    .subtract(nz(assignment.getPrincipalPaid()))
                    .subtract(nz(assignment.getTotalDiscountAmount()));
            if (principalDue.compareTo(BigDecimal.ZERO) < 0) {
                principalDue = BigDecimal.ZERO;
            }
            BigDecimal lateDue = nz(assignment.getLateFeeAccrued())
                    .subtract(nz(assignment.getLateFeePaid()))
                    .subtract(nz(assignment.getLateFeeWaived()));
            if (lateDue.compareTo(BigDecimal.ZERO) < 0) {
                lateDue = BigDecimal.ZERO;
            }
            BigDecimal due = principalDue.add(lateDue);
            if (due.compareTo(BigDecimal.ZERO) > 0) {
                return due.min(cap);
            }
        }
        return BigDecimal.ZERO;
    }

    private void createPayment(
            Long studentId,
            Long sessionId,
            BigDecimal amount,
            String mode,
            LocalDate date,
            String txnRef,
            String remarks) {

        List<StudentFeeAssignment> assignments = studentFeeAssignmentRepository
                .findByStudentIdAndSessionId(studentId, sessionId)
                .stream()
                .filter(StudentFeeAssignment::isActive)
                .sorted(Comparator.comparing(a -> a.getDueDate() != null ? a.getDueDate() : LocalDate.MAX))
                .toList();

        BigDecimal remainingToAllocate = amount;
        List<FeePaymentAllocationRequest> allocations = new ArrayList<>();

        for (StudentFeeAssignment assignment : assignments) {
            if (remainingToAllocate.compareTo(BigDecimal.ZERO) <= 0)
                break;

            BigDecimal principalDue = nz(assignment.getAmount())
                    .subtract(nz(assignment.getPrincipalPaid()))
                    .subtract(nz(assignment.getTotalDiscountAmount()));

            BigDecimal lateDue = nz(assignment.getLateFeeAccrued())
                    .subtract(nz(assignment.getLateFeePaid()))
                    .subtract(nz(assignment.getLateFeeWaived()));

            BigDecimal totalDue = principalDue.add(lateDue);

            if (totalDue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal paymentForThis = remainingToAllocate.min(totalDue);
                allocations.add(FeePaymentAllocationRequest.builder()
                        .assignmentId(assignment.getId())
                        .principalAmount(paymentForThis) // Service will handle late fee first
                        .build());
                remainingToAllocate = remainingToAllocate.subtract(paymentForThis);
            }
        }

        if (allocations.isEmpty())
            return;

        FeePaymentRequest req = new FeePaymentRequest();
        req.setStudentId(studentId);
        req.setSessionId(sessionId);
        req.setAllocations(allocations);
        req.setMode(mode);
        req.setPaymentDate(date);
        req.setTransactionReference(txnRef);
        req.setRemarks(remarks + " [" + MARKER_PREFIX + "]");
        feePaymentService.pay(req);
    }

    private boolean paymentExists(Long studentId, Long sessionId, Long schoolId, String transactionReference) {
        List<FeePayment> payments = feePaymentRepository
                .findByStudentIdAndSessionIdAndSchoolIdOrderByPaymentDateDescIdDesc(studentId, sessionId, schoolId);
        return payments.stream()
                .anyMatch(p -> transactionReference.equals(p.getTransactionReference()));
    }

    private void seedExpenseHeadsAndVouchers(Long schoolId, Long sessionId) {
        Map<String, ExpenseHead> heads = ensureExpenseHeads(schoolId);

        LocalDate today = LocalDate.now();
        LocalDate expenseOnlyDate = today.minusDays(2);

        Long createdBy = resolveCreatedByUserId(schoolId);

        ensureVoucher(schoolId, sessionId, createdBy, heads.get("Stationery"), today,
                new BigDecimal("180.00"), ExpensePaymentMode.CASH, "Cash expense today",
                MARKER_PREFIX + "-EXP-TODAY-CASH-" + schoolId + "-" + sessionId);

        ensureVoucher(schoolId, sessionId, createdBy, heads.get("Maintenance"), expenseOnlyDate,
                new BigDecimal("220.00"), ExpensePaymentMode.CASH, "Cash expense historical",
                MARKER_PREFIX + "-EXP-OLD-CASH-" + schoolId + "-" + sessionId);

        ensureVoucher(schoolId, sessionId, createdBy, heads.get("Snacks"), expenseOnlyDate,
                new BigDecimal("140.00"), ExpensePaymentMode.BANK, "Bank expense historical",
                MARKER_PREFIX + "-EXP-OLD-BANK-" + schoolId + "-" + sessionId);
    }

    private Map<String, ExpenseHead> ensureExpenseHeads(Long schoolId) {
        String[] names = { "Stationery", "Maintenance", "Snacks" };
        for (String name : names) {
            String normalized = name.trim();
            if (!expenseHeadRepository.existsBySchoolIdAndNormalizedName(schoolId, normalized)) {
                ExpenseHeadCreateRequest req = new ExpenseHeadCreateRequest();
                req.setName(name);
                req.setDescription(MARKER_PREFIX + " head");
                expenseService.createHead(req);
            }
        }

        Map<String, ExpenseHead> result = new LinkedHashMap<>();
        for (ExpenseHead head : expenseHeadRepository.findBySchoolIdOrderByNameAsc(schoolId)) {
            result.put(head.getName(), head);
        }
        return result;
    }

    private void ensureVoucher(
            Long schoolId,
            Long sessionId,
            Long createdBy,
            ExpenseHead head,
            LocalDate expenseDate,
            BigDecimal amount,
            ExpensePaymentMode paymentMode,
            String description,
            String referenceNumber) {
        boolean exists = expenseVoucherRepository
                .findBySchoolIdAndSessionIdAndActiveTrueOrderByExpenseDateDescIdDesc(schoolId, sessionId)
                .stream()
                .anyMatch(v -> referenceNumber.equals(v.getReferenceNumber()));
        if (exists || head == null) {
            return;
        }

        String voucherNumber = nextVoucherNumber(schoolId, sessionId);
        expenseVoucherRepository.saveAndFlush(ExpenseVoucher.builder()
                .schoolId(schoolId)
                .sessionId(sessionId)
                .voucherNumber(voucherNumber)
                .expenseDate(expenseDate)
                .expenseHead(head)
                .amount(amount)
                .paymentMode(paymentMode)
                .description(description + " [" + MARKER_PREFIX + "]")
                .referenceNumber(referenceNumber)
                .createdBy(createdBy)
                .active(true)
                .build());
    }

    private String nextVoucherNumber(Long schoolId, Long sessionId) {
        int nextSeq = expenseVoucherRepository.findFirstBySchoolIdAndSessionIdOrderByIdDesc(schoolId, sessionId)
                .map(v -> extractSequence(v.getVoucherNumber()) + 1)
                .orElse(1);
        return "EXP-" + sessionId + "-" + String.format("%05d", nextSeq);
    }

    private int extractSequence(String voucherNumber) {
        if (voucherNumber == null || voucherNumber.isBlank()) {
            return 0;
        }
        String[] parts = voucherNumber.split("-");
        try {
            return Integer.parseInt(parts[parts.length - 1]);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private Long resolveCreatedByUserId(Long schoolId) {
        List<User> schoolAdmins = userRepository.findByRole(UserRole.SCHOOL_ADMIN, PageRequest.of(0, 500)).getContent();
        for (User user : schoolAdmins) {
            if (user.isActive() && user.getSchool() != null && schoolId.equals(user.getSchool().getId())) {
                return user.getId();
            }
        }
        return userRepository.findByRole(UserRole.SUPER_ADMIN, PageRequest.of(0, 1)).getContent().stream()
                .findFirst()
                .map(User::getId)
                .orElse(1L);
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
