package com.school.backend.devtools.seeder;

import com.school.backend.common.enums.DiscountType;
import com.school.backend.core.classsubject.entity.SchoolClass;
import com.school.backend.core.student.entity.Student;
import com.school.backend.core.student.entity.StudentEnrollment;
import com.school.backend.fee.entity.DiscountDefinition;
import com.school.backend.fee.entity.FeeAdjustment;
import com.school.backend.fee.entity.FeePayment;
import com.school.backend.fee.entity.FeeStructure;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.fee.entity.LateFeeLog;
import com.school.backend.fee.entity.LateFeePolicy;
import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.common.enums.FeeFrequency;
import com.school.backend.common.enums.LateFeeCapType;
import com.school.backend.common.enums.LateFeeType;
import com.school.backend.fee.repository.FeePaymentRepository;
import com.school.backend.fee.repository.FeeStructureRepository;
import com.school.backend.fee.repository.FeeTypeRepository;
import com.school.backend.fee.repository.DiscountDefinitionRepository;
import com.school.backend.fee.repository.FeeAdjustmentRepository;
import com.school.backend.fee.repository.LateFeeLogRepository;
import com.school.backend.fee.repository.LateFeePolicyRepository;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class FeeSeeder {

    private final FeeTypeRepository feeTypeRepository;
    private final FeeStructureRepository feeStructureRepository;
    private final LateFeePolicyRepository lateFeePolicyRepository;
    private final StudentFeeAssignmentRepository studentFeeAssignmentRepository;
    private final DiscountDefinitionRepository discountDefinitionRepository;
    private final FeeAdjustmentRepository feeAdjustmentRepository;
    private final FeePaymentRepository feePaymentRepository;
    private final LateFeeLogRepository lateFeeLogRepository;

    @Transactional
    public void seed(
            Random random,
            SessionSeeder.Result sessionResult,
            ClassSubjectSeeder.Result classSubjectResult,
            StudentSeeder.Result studentResult,
            TransportSeeder.Result transportResult) {
        List<FeeType> feeTypesToSave = new ArrayList<>();
        for (Long schoolId : classSubjectResult.classesBySchool().keySet()) {
            feeTypesToSave.add(feeType(schoolId, "Tuition"));
            feeTypesToSave.add(feeType(schoolId, "Exam"));
            feeTypesToSave.add(feeType(schoolId, "Transport"));
        }
        List<FeeType> savedTypes = feeTypeRepository.saveAll(feeTypesToSave);

        Map<Long, Map<String, FeeType>> feeTypeBySchoolAndName = new LinkedHashMap<>();
        for (FeeType feeType : savedTypes) {
            feeTypeBySchoolAndName
                    .computeIfAbsent(feeType.getSchoolId(), k -> new LinkedHashMap<>())
                    .put(feeType.getName(), feeType);
        }

        List<FeeStructure> feeStructuresToSave = new ArrayList<>();
        Map<String, FeeStructure> structureByClassSessionType = new LinkedHashMap<>();

        for (Map.Entry<Long, List<SchoolClass>> entry : classSubjectResult.classesBySchool().entrySet()) {
            Long schoolId = entry.getKey();
            Map<String, FeeType> typeMap = feeTypeBySchoolAndName.get(schoolId);
            for (SchoolClass schoolClass : entry.getValue()) {
                FeeStructure tuition = FeeStructure.builder()
                        .schoolId(schoolId)
                        .classId(schoolClass.getId())
                        .sessionId(schoolClass.getSessionId())
                        .feeType(typeMap.get("Tuition"))
                        .amount(tuitionAmount(schoolClass.getName()))
                        .frequency(FeeFrequency.MONTHLY)
                        .dueDayOfMonth(10)
                        .active(true)
                        .build();
                feeStructuresToSave.add(tuition);
                structureByClassSessionType.put(key(schoolClass.getId(), schoolClass.getSessionId(), "Tuition"),
                        tuition);

                FeeStructure exam = FeeStructure.builder()
                        .schoolId(schoolId)
                        .classId(schoolClass.getId())
                        .sessionId(schoolClass.getSessionId())
                        .feeType(typeMap.get("Exam"))
                        .amount(examAmount(schoolClass.getName()))
                        .frequency(FeeFrequency.ONE_TIME)
                        .dueDayOfMonth(10)
                        .active(true)
                        .build();
                feeStructuresToSave.add(exam);
                structureByClassSessionType.put(key(schoolClass.getId(), schoolClass.getSessionId(), "Exam"), exam);
            }
        }

        List<FeeStructure> savedStructures = feeStructureRepository.saveAll(feeStructuresToSave);

        List<LateFeePolicy> policies = new ArrayList<>(savedStructures.size());
        for (FeeStructure structure : savedStructures) {
            policies.add(
                    LateFeePolicy.builder()
                            .schoolId(structure.getSchoolId())
                            .feeStructure(structure)
                            .type(LateFeeType.PERCENTAGE)
                            .amountValue(BigDecimal.valueOf(2).setScale(2, RoundingMode.HALF_UP))
                            .graceDays(10)
                            .capType(LateFeeCapType.PERCENTAGE)
                            .capValue(BigDecimal.valueOf(20).setScale(2, RoundingMode.HALF_UP))
                            .active(true)
                            .build());
        }
        lateFeePolicyRepository.saveAll(policies);

        List<StudentFeeAssignment> assignmentsToSave = new ArrayList<>();
        List<FeePayment> paymentsToSave = new ArrayList<>();

        Map<Long, SessionSeeder.SessionTriplet> sessionsBySchool = sessionResult.sessionsBySchool();
        Map<Long, Map<Long, List<StudentEnrollment>>> enrollmentsBySchoolAndSession = studentResult
                .enrollmentsBySchoolAndSession();

        for (Map.Entry<Long, List<Student>> entry : studentResult.studentsBySchool().entrySet()) {
            Long schoolId = entry.getKey();
            List<Student> students = entry.getValue();
            SessionSeeder.SessionTriplet sessions = sessionsBySchool.get(schoolId);

            Map<String, StudentBucket> bucketByStudentId = buildBuckets(students);
            for (StudentEnrollment enrollment : findEnrollmentsForSchool(enrollmentsBySchoolAndSession, schoolId)) {
                StudentBucket bucket = bucketByStudentId.getOrDefault(String.valueOf(enrollment.getStudentId()),
                        StudentBucket.FULLY_PAID);
                String classSessionPrefix = key(enrollment.getClassId(), enrollment.getSessionId(), "");

                FeeStructure tuition = structureByClassSessionType.get(classSessionPrefix + "Tuition");
                FeeStructure exam = structureByClassSessionType.get(classSessionPrefix + "Exam");
                if (tuition != null) {
                    assignmentsToSave.add(buildAssignment(tuition, enrollment, sessions, bucket));
                    maybeAddPayment(random, enrollment, assignmentsToSave.get(assignmentsToSave.size() - 1),
                            paymentsToSave);
                }
                if (exam != null) {
                    assignmentsToSave.add(buildAssignment(exam, enrollment, sessions, bucket));
                    maybeAddPayment(random, enrollment, assignmentsToSave.get(assignmentsToSave.size() - 1),
                            paymentsToSave);
                }

            }
        }

        BatchSaveUtil.saveInBatches(assignmentsToSave, 1_000, studentFeeAssignmentRepository::saveAll);
        seedDiscountExamples(assignmentsToSave, classSubjectResult.classesBySchool().keySet());
        List<LateFeeLog> lateFeeLogsToSave = buildLateFeeLogs(assignmentsToSave);
        BatchSaveUtil.saveInBatches(paymentsToSave, 1_000, feePaymentRepository::saveAll);
        BatchSaveUtil.saveInBatches(lateFeeLogsToSave, 1_000, lateFeeLogRepository::saveAll);
    }

    private void seedDiscountExamples(List<StudentFeeAssignment> assignmentsToSave, java.util.Set<Long> schoolIds) {
        List<DiscountDefinition> definitionsToSave = new ArrayList<>();
        Map<Long, DiscountDefinition> percentageBySchool = new LinkedHashMap<>();
        Map<Long, DiscountDefinition> flatBySchool = new LinkedHashMap<>();

        for (Long schoolId : schoolIds) {
            DiscountDefinition percentage = DiscountDefinition.builder()
                    .schoolId(schoolId)
                    .name("Merit Scholarship 10%")
                    .type(DiscountType.PERCENTAGE)
                    .amountValue(new BigDecimal("10.00"))
                    .active(true)
                    .build();
            DiscountDefinition flat = DiscountDefinition.builder()
                    .schoolId(schoolId)
                    .name("Need Support 1500")
                    .type(DiscountType.FLAT)
                    .amountValue(new BigDecimal("1500.00"))
                    .active(true)
                    .build();
            definitionsToSave.add(percentage);
            definitionsToSave.add(flat);
            percentageBySchool.put(schoolId, percentage);
            flatBySchool.put(schoolId, flat);
        }
        BatchSaveUtil.saveInBatches(definitionsToSave, 1_000, discountDefinitionRepository::saveAll);

        Map<Long, Integer> discountedCountBySchool = new LinkedHashMap<>();
        List<StudentFeeAssignment> assignmentsToUpdate = new ArrayList<>();
        List<FeeAdjustment> adjustmentsToSave = new ArrayList<>();

        for (StudentFeeAssignment assignment : assignmentsToSave) {
            Long schoolId = assignment.getSchoolId();
            int schoolCount = discountedCountBySchool.getOrDefault(schoolId, 0);
            if (schoolCount >= 40) {
                continue;
            }

            BigDecimal principalDue = assignment.getAmount()
                    .subtract(assignment.getPrincipalPaid())
                    .subtract(assignment.getTotalDiscountAmount());
            if (principalDue.compareTo(BigDecimal.valueOf(100)) <= 0) {
                continue;
            }

            DiscountDefinition definition;
            BigDecimal discountAmount;
            if (schoolCount % 2 == 0) {
                definition = percentageBySchool.get(schoolId);
                discountAmount = principalDue.multiply(new BigDecimal("10"))
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            } else {
                definition = flatBySchool.get(schoolId);
                discountAmount = principalDue.min(new BigDecimal("1500.00")).setScale(2, RoundingMode.HALF_UP);
            }

            if (definition == null || discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            assignment.setTotalDiscountAmount(assignment.getTotalDiscountAmount().add(discountAmount));
            assignmentsToUpdate.add(assignment);

            adjustmentsToSave.add(FeeAdjustment.builder()
                    .schoolId(schoolId)
                    .assignmentId(assignment.getId())
                    .type(FeeAdjustment.AdjustmentType.DISCOUNT)
                    .amount(discountAmount)
                    .discountDefinitionId(definition.getId())
                    .discountNameSnapshot(definition.getName())
                    .discountTypeSnapshot(definition.getType())
                    .discountValueSnapshot(definition.getAmountValue())
                    .reason("Seeded discount for UI validation")
                    .createdByStaff(null)
                    .build());

            discountedCountBySchool.put(schoolId, schoolCount + 1);
        }

        if (!assignmentsToUpdate.isEmpty()) {
            BatchSaveUtil.saveInBatches(assignmentsToUpdate, 1_000, studentFeeAssignmentRepository::saveAll);
        }
        if (!adjustmentsToSave.isEmpty()) {
            BatchSaveUtil.saveInBatches(adjustmentsToSave, 1_000, feeAdjustmentRepository::saveAll);
        }
    }

    private FeeType feeType(Long schoolId, String name) {
        return FeeType.builder()
                .schoolId(schoolId)
                .name(name)
                .description(name + " Fee")
                .active(true)
                .build();
    }

    private BigDecimal tuitionAmount(String className) {
        BigDecimal base;
        if ("9".equals(className) || "10".equals(className) || "11".equals(className) || "12".equals(className)) {
            base = BigDecimal.valueOf(42_000);
        } else if ("6".equals(className) || "7".equals(className) || "8".equals(className)) {
            base = BigDecimal.valueOf(32_000);
        } else {
            base = BigDecimal.valueOf(24_000);
        }
        return base.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal examAmount(String className) {
        BigDecimal amount = ("11".equals(className) || "12".equals(className))
                ? BigDecimal.valueOf(4_500)
                : BigDecimal.valueOf(3_000);
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, StudentBucket> buildBuckets(List<Student> students) {
        Map<String, StudentBucket> buckets = new LinkedHashMap<>();
        int total = students.size();
        int fullPaidCutoff = (int) Math.round(total * 0.40);
        int partialPaidCutoff = fullPaidCutoff + (int) Math.round(total * 0.25);
        int overdueCutoff = partialPaidCutoff + (int) Math.round(total * 0.15);

        for (int i = 0; i < students.size(); i++) {
            StudentBucket bucket;
            if (i < fullPaidCutoff) {
                bucket = StudentBucket.FULLY_PAID;
            } else if (i < partialPaidCutoff) {
                bucket = StudentBucket.PARTIAL_PAID;
            } else if (i < overdueCutoff) {
                bucket = StudentBucket.OVERDUE_WITH_LATE_FEE;
            } else {
                bucket = StudentBucket.MULTI_SESSION_DEFAULTER;
            }
            buckets.put(String.valueOf(students.get(i).getId()), bucket);
        }
        return buckets;
    }

    private List<StudentEnrollment> findEnrollmentsForSchool(
            Map<Long, Map<Long, List<StudentEnrollment>>> enrollmentsBySchoolAndSession,
            Long schoolId) {
        List<StudentEnrollment> result = new ArrayList<>();
        Map<Long, List<StudentEnrollment>> bySession = enrollmentsBySchoolAndSession.getOrDefault(schoolId, Map.of());
        for (List<StudentEnrollment> sessionEnrollments : bySession.values()) {
            result.addAll(sessionEnrollments);
        }
        return result;
    }

    private StudentFeeAssignment buildAssignment(
            FeeStructure structure,
            StudentEnrollment enrollment,
            SessionSeeder.SessionTriplet sessions,
            StudentBucket bucket) {
        BigDecimal amount = structure.getAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal principalPaid = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal lateFeeAccrued = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        boolean lateFeeApplied = false;

        BigDecimal payable = amount.setScale(2, RoundingMode.HALF_UP);

        switch (bucket) {
            case FULLY_PAID -> principalPaid = payable;
            case PARTIAL_PAID ->
                principalPaid = payable.multiply(BigDecimal.valueOf(0.50)).setScale(2, RoundingMode.HALF_UP);
            case OVERDUE_WITH_LATE_FEE -> {
                lateFeeApplied = true;
                lateFeeAccrued = amount.multiply(BigDecimal.valueOf(0.06)).setScale(2, RoundingMode.HALF_UP);
            }
            case MULTI_SESSION_DEFAULTER -> {
                lateFeeApplied = true;
                lateFeeAccrued = amount.multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_UP);
            }
        }

        LocalDate dueDate = enrollment.getSessionId().equals(sessions.completed().getId())
                ? LocalDate.of(2023, 7, 10)
                : LocalDate.of(2024, 7, 10);
        if (bucket == StudentBucket.MULTI_SESSION_DEFAULTER || bucket == StudentBucket.OVERDUE_WITH_LATE_FEE) {
            dueDate = dueDate.minusDays(45);
        }

        return StudentFeeAssignment.builder()
                .schoolId(enrollment.getSchoolId())
                .studentId(enrollment.getStudentId())
                .feeStructureId(structure.getId())
                .sessionId(enrollment.getSessionId())
                .amount(amount)
                .dueDate(dueDate)
                .lateFeeType(LateFeeType.PERCENTAGE)
                .lateFeeValue(BigDecimal.valueOf(2).setScale(2, RoundingMode.HALF_UP))
                .lateFeeGraceDays(10)
                .lateFeeCapType(LateFeeCapType.PERCENTAGE)
                .lateFeeCapValue(BigDecimal.valueOf(20).setScale(2, RoundingMode.HALF_UP))
                .lateFeeApplied(lateFeeApplied)
                .lateFeeAccrued(lateFeeAccrued)
                .principalPaid(principalPaid)
                .lateFeePaid(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .lateFeeWaived(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .totalDiscountAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .active(true)
                .build();
    }

    private void maybeAddPayment(
            Random random,
            StudentEnrollment enrollment,
            StudentFeeAssignment assignment,
            List<FeePayment> paymentsToSave) {
        if (assignment.getPrincipalPaid().compareTo(BigDecimal.ZERO) > 0) {
            paymentsToSave.add(
                    FeePayment.builder()
                            .schoolId(enrollment.getSchoolId())
                            .studentId(enrollment.getStudentId())
                            .sessionId(enrollment.getSessionId())
                            .principalPaid(assignment.getPrincipalPaid())
                            .lateFeePaid(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                            .paymentDate(assignment.getDueDate().minusDays(2 + random.nextInt(4)))
                            .transactionReference("TXN-" + enrollment.getStudentId() + "-" + enrollment.getClassId()
                                    + "-" + random.nextInt(10_000))
                            .mode("UPI")
                            .remarks("Seeded payment")
                            .build());
        }
    }

    private List<LateFeeLog> buildLateFeeLogs(List<StudentFeeAssignment> assignments) {
        List<LateFeeLog> logs = new ArrayList<>();
        for (StudentFeeAssignment assignment : assignments) {
            if (assignment.getLateFeeAccrued().compareTo(BigDecimal.ZERO) > 0) {
                logs.add(
                        LateFeeLog.builder()
                                .schoolId(assignment.getSchoolId())
                                .assignmentId(assignment.getId())
                                .computedAmount(assignment.getLateFeeAccrued())
                                .appliedDate(assignment.getDueDate().plusDays(20))
                                .reason("Seeded late fee accrual")
                                .build());
            }
        }
        return logs;
    }

    private String key(Long classId, Long sessionId, String type) {
        return classId + "::" + sessionId + "::" + type;
    }

    private enum StudentBucket {
        FULLY_PAID,
        PARTIAL_PAID,
        OVERDUE_WITH_LATE_FEE,
        MULTI_SESSION_DEFAULTER
    }
}
