package com.school.backend.devtools.seeder;

import com.school.backend.core.classsubject.entity.SchoolClass;
import com.school.backend.core.student.entity.Student;
import com.school.backend.core.student.entity.StudentEnrollment;
import com.school.backend.fee.entity.FeePayment;
import com.school.backend.fee.entity.FeeStructure;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.fee.entity.LateFeeLog;
import com.school.backend.fee.entity.LateFeePolicy;
import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.fee.entity.StudentFundingArrangement;
import com.school.backend.fee.enums.FeeFrequency;
import com.school.backend.fee.enums.FundingCoverageMode;
import com.school.backend.fee.enums.FundingCoverageType;
import com.school.backend.fee.enums.LateFeeCapType;
import com.school.backend.fee.enums.LateFeeType;
import com.school.backend.fee.repository.FeePaymentRepository;
import com.school.backend.fee.repository.FeeStructureRepository;
import com.school.backend.fee.repository.FeeTypeRepository;
import com.school.backend.fee.repository.LateFeeLogRepository;
import com.school.backend.fee.repository.LateFeePolicyRepository;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import com.school.backend.fee.repository.StudentFundingArrangementRepository;
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
    private final StudentFundingArrangementRepository studentFundingArrangementRepository;
    private final FeePaymentRepository feePaymentRepository;
    private final LateFeeLogRepository lateFeeLogRepository;

    @Transactional
    public void seed(
            Random random,
            SessionSeeder.Result sessionResult,
            ClassSubjectSeeder.Result classSubjectResult,
            StudentSeeder.Result studentResult,
            TransportSeeder.Result transportResult
    ) {
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
                        .frequency(FeeFrequency.ANNUALLY)
                        .dueDayOfMonth(10)
                        .active(true)
                        .build();
                feeStructuresToSave.add(tuition);
                structureByClassSessionType.put(key(schoolClass.getId(), schoolClass.getSessionId(), "Tuition"), tuition);

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

                if (shouldIncludeTransport(schoolClass.getName())) {
                    FeeStructure transport = FeeStructure.builder()
                            .schoolId(schoolId)
                            .classId(schoolClass.getId())
                            .sessionId(schoolClass.getSessionId())
                            .feeType(typeMap.get("Transport"))
                            .amount(BigDecimal.valueOf(8_400).setScale(2, RoundingMode.HALF_UP))
                            .frequency(FeeFrequency.ANNUALLY)
                            .dueDayOfMonth(10)
                            .active(true)
                            .build();
                    feeStructuresToSave.add(transport);
                    structureByClassSessionType.put(key(schoolClass.getId(), schoolClass.getSessionId(), "Transport"), transport);
                }
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
                            .build()
            );
        }
        lateFeePolicyRepository.saveAll(policies);

        List<StudentFundingArrangement> fundingArrangementsToSave = new ArrayList<>();
        List<StudentFeeAssignment> assignmentsToSave = new ArrayList<>();
        List<FeePayment> paymentsToSave = new ArrayList<>();

        Map<Long, SessionSeeder.SessionTriplet> sessionsBySchool = sessionResult.sessionsBySchool();
        Map<Long, Map<Long, List<StudentEnrollment>>> enrollmentsBySchoolAndSession = studentResult.enrollmentsBySchoolAndSession();

        for (Map.Entry<Long, List<Student>> entry : studentResult.studentsBySchool().entrySet()) {
            Long schoolId = entry.getKey();
            List<Student> students = entry.getValue();
            SessionSeeder.SessionTriplet sessions = sessionsBySchool.get(schoolId);

            Map<String, StudentBucket> bucketByStudentId = buildBuckets(students);
            Map<String, StudentFundingArrangement> fundingArrangementByStudentSession = new LinkedHashMap<>();

            for (StudentEnrollment enrollment : findEnrollmentsForSchool(enrollmentsBySchoolAndSession, schoolId)) {
                StudentBucket bucket = bucketByStudentId.getOrDefault(String.valueOf(enrollment.getStudentId()), StudentBucket.FULLY_PAID);
                String classSessionPrefix = key(enrollment.getClassId(), enrollment.getSessionId(), "");

                FeeStructure tuition = structureByClassSessionType.get(classSessionPrefix + "Tuition");
                FeeStructure exam = structureByClassSessionType.get(classSessionPrefix + "Exam");
                if (tuition != null) {
                    assignmentsToSave.add(buildAssignment(tuition, enrollment, sessions, bucket, fundingArrangementByStudentSession, fundingArrangementsToSave));
                    maybeAddPayment(random, enrollment, assignmentsToSave.get(assignmentsToSave.size() - 1), paymentsToSave);
                }
                if (exam != null) {
                    assignmentsToSave.add(buildAssignment(exam, enrollment, sessions, bucket, fundingArrangementByStudentSession, fundingArrangementsToSave));
                    maybeAddPayment(random, enrollment, assignmentsToSave.get(assignmentsToSave.size() - 1), paymentsToSave);
                }

                Long transportSession = transportResult.transportSessionByStudentId().get(enrollment.getStudentId());
                FeeStructure transport = structureByClassSessionType.get(classSessionPrefix + "Transport");
                if (transport != null && transportSession != null && transportSession.equals(enrollment.getSessionId())) {
                    assignmentsToSave.add(buildAssignment(transport, enrollment, sessions, bucket, fundingArrangementByStudentSession, fundingArrangementsToSave));
                    maybeAddPayment(random, enrollment, assignmentsToSave.get(assignmentsToSave.size() - 1), paymentsToSave);
                }
            }
        }

        BatchSaveUtil.saveInBatches(fundingArrangementsToSave, 1_000, studentFundingArrangementRepository::saveAll);
        BatchSaveUtil.saveInBatches(assignmentsToSave, 1_000, studentFeeAssignmentRepository::saveAll);
        List<LateFeeLog> lateFeeLogsToSave = buildLateFeeLogs(assignmentsToSave);
        BatchSaveUtil.saveInBatches(paymentsToSave, 1_000, feePaymentRepository::saveAll);
        BatchSaveUtil.saveInBatches(lateFeeLogsToSave, 1_000, lateFeeLogRepository::saveAll);
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

    private boolean shouldIncludeTransport(String className) {
        return switch (className) {
            case "Nursery", "LKG", "UKG", "1", "2", "3", "4", "5", "6", "7" -> true;
            default -> false;
        };
    }

    private Map<String, StudentBucket> buildBuckets(List<Student> students) {
        Map<String, StudentBucket> buckets = new LinkedHashMap<>();
        int total = students.size();
        int fullPaidCutoff = (int) Math.round(total * 0.40);
        int partialPaidCutoff = fullPaidCutoff + (int) Math.round(total * 0.25);
        int overdueCutoff = partialPaidCutoff + (int) Math.round(total * 0.15);
        int partialSponsorshipCutoff = overdueCutoff + (int) Math.round(total * 0.10);
        int fullSponsorshipCutoff = partialSponsorshipCutoff + (int) Math.round(total * 0.05);

        for (int i = 0; i < students.size(); i++) {
            StudentBucket bucket;
            if (i < fullPaidCutoff) {
                bucket = StudentBucket.FULLY_PAID;
            } else if (i < partialPaidCutoff) {
                bucket = StudentBucket.PARTIAL_PAID;
            } else if (i < overdueCutoff) {
                bucket = StudentBucket.OVERDUE_WITH_LATE_FEE;
            } else if (i < partialSponsorshipCutoff) {
                bucket = StudentBucket.PARTIAL_SPONSORSHIP;
            } else if (i < fullSponsorshipCutoff) {
                bucket = StudentBucket.FULL_SPONSORSHIP;
            } else {
                bucket = StudentBucket.MULTI_SESSION_DEFAULTER;
            }
            buckets.put(String.valueOf(students.get(i).getId()), bucket);
        }
        return buckets;
    }

    private List<StudentEnrollment> findEnrollmentsForSchool(
            Map<Long, Map<Long, List<StudentEnrollment>>> enrollmentsBySchoolAndSession,
            Long schoolId
    ) {
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
            StudentBucket bucket,
            Map<String, StudentFundingArrangement> fundingArrangementByStudentSession,
            List<StudentFundingArrangement> fundingArrangementsToSave
    ) {
        BigDecimal amount = structure.getAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal sponsorCovered = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal principalPaid = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal lateFeeAccrued = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        boolean lateFeeApplied = false;

        if (bucket == StudentBucket.PARTIAL_SPONSORSHIP || bucket == StudentBucket.FULL_SPONSORSHIP) {
            BigDecimal coverage = bucket == StudentBucket.FULL_SPONSORSHIP
                    ? BigDecimal.valueOf(100)
                    : BigDecimal.valueOf(50);
            sponsorCovered = amount.multiply(coverage).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            String fundingKey = enrollment.getStudentId() + "::" + enrollment.getSessionId();
            if (!fundingArrangementByStudentSession.containsKey(fundingKey)) {
                StudentFundingArrangement arrangement = StudentFundingArrangement.builder()
                        .schoolId(enrollment.getSchoolId())
                        .studentId(enrollment.getStudentId())
                        .sessionId(enrollment.getSessionId())
                        .coverageType(bucket == StudentBucket.FULL_SPONSORSHIP ? FundingCoverageType.FULL : FundingCoverageType.PARTIAL)
                        .coverageMode(FundingCoverageMode.PERCENTAGE)
                        .coverageValue(coverage.setScale(2, RoundingMode.HALF_UP))
                        .validFrom(LocalDate.of(2024, 4, 1))
                        .validTo(LocalDate.of(2025, 3, 31))
                        .active(true)
                        .build();
                fundingArrangementByStudentSession.put(fundingKey, arrangement);
                fundingArrangementsToSave.add(arrangement);
            }
        }

        BigDecimal payable = amount.subtract(sponsorCovered).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        switch (bucket) {
            case FULLY_PAID -> principalPaid = payable;
            case PARTIAL_PAID -> principalPaid = payable.multiply(BigDecimal.valueOf(0.50)).setScale(2, RoundingMode.HALF_UP);
            case PARTIAL_SPONSORSHIP -> principalPaid = payable;
            case OVERDUE_WITH_LATE_FEE -> {
                lateFeeApplied = true;
                lateFeeAccrued = amount.multiply(BigDecimal.valueOf(0.06)).setScale(2, RoundingMode.HALF_UP);
            }
            case MULTI_SESSION_DEFAULTER -> {
                lateFeeApplied = true;
                lateFeeAccrued = amount.multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_UP);
            }
            case FULL_SPONSORSHIP -> principalPaid = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
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
                .sponsorCoveredAmount(sponsorCovered)
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
            List<FeePayment> paymentsToSave
    ) {
        if (assignment.getPrincipalPaid().compareTo(BigDecimal.ZERO) > 0) {
            paymentsToSave.add(
                    FeePayment.builder()
                            .schoolId(enrollment.getSchoolId())
                            .studentId(enrollment.getStudentId())
                            .sessionId(enrollment.getSessionId())
                            .principalPaid(assignment.getPrincipalPaid())
                            .lateFeePaid(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                            .paymentDate(assignment.getDueDate().minusDays(2 + random.nextInt(4)))
                            .transactionReference("TXN-" + enrollment.getStudentId() + "-" + enrollment.getClassId() + "-" + random.nextInt(10_000))
                            .mode("UPI")
                            .remarks("Seeded payment")
                            .build()
            );
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
                                .build()
                );
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
        PARTIAL_SPONSORSHIP,
        FULL_SPONSORSHIP,
        MULTI_SESSION_DEFAULTER
    }
}
