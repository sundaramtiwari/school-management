package com.school.backend.fee.service;

import com.school.backend.common.exception.BusinessException;
import com.school.backend.common.exception.InvalidOperationException;
import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.common.tenant.SessionContext;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.core.student.entity.Student;
import com.school.backend.core.student.repository.StudentEnrollmentRepository;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.fee.dto.DefaulterDto;
import com.school.backend.fee.dto.FeeStatsDto;
import com.school.backend.fee.dto.FeeSummaryDto;
import com.school.backend.fee.dto.StudentLedgerDto;
import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.fee.repository.FeePaymentRepository;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.repository.AcademicSessionRepository;
import com.school.backend.user.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeeSummaryService {

    private static final int INT_ZERO = 0;
    private static final int SCALE_2 = 2;
    private static final int DASHBOARD_COMPONENT_COUNT = 7;
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final RoundingMode ROUNDING_MODE_HALF_UP = RoundingMode.HALF_UP;

    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final StudentFeeAssignmentRepository assignmentRepository;
    private final FeePaymentRepository paymentRepository;
    private final AcademicSessionRepository sessionRepository;

    // ---------------------------------------------------
    // DASHBOARD STATS
    // ---------------------------------------------------
    @Transactional(readOnly = true)
    public FeeStatsDto getDashboardStats() {
        Long effectiveSessionId = validateAndGetSessionId();
        Long schoolId = TenantContext.getSchoolId();

        // 1. Today's Collection
        BigDecimal todayPaid = paymentRepository
                .sumTotalPaidBySchoolIdAndPaymentDate(schoolId, LocalDate.now());

        BigDecimal todayCollection = todayPaid != null ? todayPaid : ZERO;

        // 2. Total Students (SESSION AWARE)
        long totalStudents = enrollmentRepository
                .countBySchoolIdAndSessionIdAndActiveTrue(schoolId, effectiveSessionId);

        // 3. Optimized Pending Calculation (NO N+1)
        Object[] pendingComponentsRaw = assignmentRepository
                .sumFinancialTotalsBySchoolAndSession(schoolId, effectiveSessionId);
        Object[] pendingComponents = normalizeAggregationRow(pendingComponentsRaw, DASHBOARD_COMPONENT_COUNT);
        BigDecimal totalAssigned = toBigDecimal(pendingComponents[0]);
        BigDecimal totalLateFeeAccrued = toBigDecimal(pendingComponents[1]);
        BigDecimal totalDiscountAmount = toBigDecimal(pendingComponents[2]);
        BigDecimal totalSponsorCoveredAmount = toBigDecimal(pendingComponents[3]);
        BigDecimal totalLateFeeWaived = toBigDecimal(pendingComponents[4]);
        BigDecimal totalPrincipalPaid = toBigDecimal(pendingComponents[5]);
        BigDecimal totalLateFeePaid = toBigDecimal(pendingComponents[6]);

        BigDecimal totalPending = computePendingTotals(
                totalAssigned,
                totalLateFeeAccrued,
                totalDiscountAmount,
                totalSponsorCoveredAmount,
                totalLateFeeWaived,
                totalPrincipalPaid,
                totalLateFeePaid);

        return FeeStatsDto.builder()
                .todayCollection(todayCollection)
                .totalStudents(totalStudents)
                .pendingDues(totalPending)
                .build();
    }

    // ---------------------------------------------------
    // STUDENT SUMMARY
    // ---------------------------------------------------
    @Transactional(readOnly = true)
    public FeeSummaryDto getStudentFeeSummary(Long studentId) {

        Long schoolId = SecurityUtil.schoolId();

        Student student = studentRepository
                .findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));

        Long effectiveSessionId = validateAndGetSessionId();

        AcademicSession session = sessionRepository.findById(effectiveSessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Session not found: " + effectiveSessionId));

        List<StudentFeeAssignment> assignments = assignmentRepository.findByStudentIdAndSessionId(studentId,
                effectiveSessionId);

        BigDecimal totalFeeAccrued = assignments.stream()
                .map(a -> nz(a.getAmount()))
                .reduce(ZERO, java.math.BigDecimal::add);

        BigDecimal totalDiscountAmount = assignments.stream()
                .map(a -> nz(a.getTotalDiscountAmount()))
                .reduce(ZERO, BigDecimal::add);

        BigDecimal totalSponsorCoveredAmount = assignments.stream()
                .map(a -> nz(a.getSponsorCoveredAmount()))
                .reduce(ZERO, BigDecimal::add);

        BigDecimal totalLateFeeWaived = assignments.stream()
                .map(a -> nz(a.getLateFeeWaived()))
                .reduce(ZERO, BigDecimal::add);

        BigDecimal totalLateFeeAccrued = assignments.stream()
                .map(sfa -> nz(sfa.getLateFeeAccrued()))
                .reduce(ZERO, BigDecimal::add);

        BigDecimal totalLateFeePaid = assignments.stream()
                .map(a -> nz(a.getLateFeePaid()))
                .reduce(ZERO, BigDecimal::add);

        BigDecimal totalPrincipalPaid = assignments.stream()
                .map(a -> nz(a.getPrincipalPaid()))
                .reduce(ZERO, BigDecimal::add);

        BigDecimal totalPaid = totalPrincipalPaid.add(totalLateFeePaid);

        BigDecimal pending = computePendingTotals(
                totalFeeAccrued,
                totalLateFeeAccrued,
                totalDiscountAmount,
                totalSponsorCoveredAmount,
                totalLateFeeWaived,
                totalPrincipalPaid,
                totalLateFeePaid);

        FeeSummaryDto dto = new FeeSummaryDto();
        dto.setStudentId(studentId);
        dto.setStudentName(student.getFirstName() + " " +
                (student.getLastName() != null ? student.getLastName() : ""));
        dto.setSession(session.getName());
        dto.setTotalFee(totalFeeAccrued.setScale(SCALE_2, ROUNDING_MODE_HALF_UP));
        dto.setTotalDiscount(totalDiscountAmount.setScale(SCALE_2, ROUNDING_MODE_HALF_UP));
        dto.setTotalFunding(totalSponsorCoveredAmount.setScale(SCALE_2, ROUNDING_MODE_HALF_UP));
        dto.setTotalPaid(totalPaid.setScale(SCALE_2, ROUNDING_MODE_HALF_UP));
        dto.setTotalLateFeeAccrued(totalLateFeeAccrued.setScale(SCALE_2, ROUNDING_MODE_HALF_UP));
        dto.setTotalLateFeePaid(totalLateFeePaid.setScale(SCALE_2, ROUNDING_MODE_HALF_UP));
        dto.setTotalLateFeeWaived(totalLateFeeWaived.setScale(SCALE_2, ROUNDING_MODE_HALF_UP));
        dto.setPendingFee(pending.setScale(SCALE_2, ROUNDING_MODE_HALF_UP));
        dto.setFeePending(pending.compareTo(ZERO) > INT_ZERO);

        return dto;
    }

    @Transactional(readOnly = true)
    public StudentLedgerDto getStudentFullLedger(Long studentId) {
        Long schoolId = SecurityUtil.schoolId();
        Student student = studentRepository.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));

        // Single aggregation query for all sessions
        List<Object[]> sessionStats = assignmentRepository
                .sumFinancialSummaryByStudentGroupedBySession(studentId);

        // Fetch session names for mapping
        java.util.Map<Long, String> sessionNames = sessionRepository.findBySchoolId(schoolId).stream()
                .collect(java.util.stream.Collectors.toMap(AcademicSession::getId,
                        AcademicSession::getName));

        List<FeeSummaryDto> sessionSummaries = new ArrayList<>();

        for (Object[] stats : sessionStats) {
            Long sessionId = (stats[INT_ZERO] instanceof Number) ? ((Number) stats[INT_ZERO]).longValue()
                    : null;
            if (sessionId == null)
                continue;

            BigDecimal totalFee = toBigDecimal(stats[1]);
            BigDecimal totalDiscount = toBigDecimal(stats[SCALE_2]);
            BigDecimal totalSponsor = toBigDecimal(stats[3]);
            BigDecimal totalLateFeeAccrued = toBigDecimal(stats[4]);
            BigDecimal totalLateFeePaid = toBigDecimal(stats[5]);
            BigDecimal totalLateFeeWaived = toBigDecimal(stats[6]);
            BigDecimal totalPrincipalPaid = toBigDecimal(stats[7]);

            String sessionName = sessionNames.getOrDefault(sessionId, "Unknown Session " + sessionId);

            FeeSummaryDto summary = new FeeSummaryDto();
            summary.setStudentId(studentId);
            summary.setStudentName(student.getFirstName() + " " +
                    (student.getLastName() != null ? student.getLastName() : ""));
            summary.setSession(sessionName);
            summary.setTotalFee(totalFee.setScale(SCALE_2, ROUNDING_MODE_HALF_UP));
            summary.setTotalDiscount(totalDiscount.setScale(SCALE_2, ROUNDING_MODE_HALF_UP));
            summary.setTotalFunding(totalSponsor.setScale(SCALE_2, ROUNDING_MODE_HALF_UP));

            // Total Paid = Principal Paid + Late Fee Paid
            BigDecimal sessionTotalPaid = totalPrincipalPaid.add(totalLateFeePaid);
            summary.setTotalPaid(sessionTotalPaid.setScale(SCALE_2, ROUNDING_MODE_HALF_UP));

            summary.setTotalLateFeeAccrued(totalLateFeeAccrued.setScale(SCALE_2, ROUNDING_MODE_HALF_UP));
            summary.setTotalLateFeePaid(totalLateFeePaid.setScale(SCALE_2, ROUNDING_MODE_HALF_UP));

            BigDecimal pending = computePendingTotals(
                    totalFee,
                    totalLateFeeAccrued,
                    totalDiscount,
                    totalSponsor,
                    totalLateFeeWaived,
                    totalPrincipalPaid,
                    totalLateFeePaid);

            summary.setPendingFee(pending.setScale(SCALE_2, ROUNDING_MODE_HALF_UP));
            summary.setFeePending(pending.compareTo(ZERO) > INT_ZERO);

            sessionSummaries.add(summary);
        }

        BigDecimal grandTotalFee = sessionSummaries.stream()
                .map(FeeSummaryDto::getTotalFee)
                .reduce(ZERO, BigDecimal::add);
        BigDecimal grandTotalPaid = sessionSummaries.stream()
                .map(FeeSummaryDto::getTotalPaid)
                .reduce(ZERO, BigDecimal::add);
        BigDecimal grandTotalPending = sessionSummaries.stream()
                .map(FeeSummaryDto::getPendingFee)
                .reduce(ZERO, BigDecimal::add);

        return StudentLedgerDto.builder()
                .studentId(studentId)
                .studentName(student.getFirstName() + " " +
                        (student.getLastName() != null ? student.getLastName() : ""))
                .sessionSummaries(sessionSummaries)
                .grandTotalFee(grandTotalFee.setScale(SCALE_2, ROUNDING_MODE_HALF_UP))
                .grandTotalPaid(grandTotalPaid.setScale(SCALE_2, ROUNDING_MODE_HALF_UP))
                .grandTotalPending(grandTotalPending.setScale(SCALE_2, ROUNDING_MODE_HALF_UP))
                .build();
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null)
            return ZERO;
        if (val instanceof Object[] nested && nested.length > 0) {
            return toBigDecimal(nested[0]);
        }
        if (val instanceof BigDecimal)
            return (BigDecimal) val;
        return new BigDecimal(val.toString());
    }

    private Object[] normalizeAggregationRow(Object[] raw, int expectedColumns) {
        if (raw == null) {
            return new Object[expectedColumns];
        }
        if (raw.length == 1 && raw[0] instanceof Object[] nested) {
            return nested;
        }
        return raw;
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : ZERO;
    }

    // ---------------------------------------------------
    // DEFAULTERS PAGINATION & EXPORT
    // ---------------------------------------------------
    @Transactional(readOnly = true)
    public Page<DefaulterDto> getPaginatedDefaulters(String search, Long classId,
                                                     BigDecimal minAmountDue,
                                                     Integer minDaysOverdue, Pageable pageable) {
        return getDefaultersPage(search, classId, minAmountDue, minDaysOverdue, pageable);
    }

    @Transactional(readOnly = true)
    public long countDefaulters() {
        Long schoolId = SecurityUtil.schoolId();
        Long sessionId = validateAndGetSessionId();

        AcademicSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        LocalDate sessionStart;
        try {
            int startYear = Integer.parseInt(session.getName().split("-")[INT_ZERO]);
            sessionStart = LocalDate.of(startYear, Month.APRIL, 1);
        } catch (Exception e) {
            sessionStart = LocalDate.now().minusMonths(6);
        }

        BigDecimal effectiveMinAmountDue = new BigDecimal("0.01");

        return studentRepository.countDefaulters(
                schoolId,
                sessionId,
                null,
                null,
                effectiveMinAmountDue,
                null,
                sessionStart);
    }

    @Transactional(readOnly = true)
    public List<DefaulterDto> exportDefaulters(String search, Long classId, BigDecimal minAmountDue,
                                               Integer minDaysOverdue) {
        Page<DefaulterDto> page = getDefaultersPage(search, classId, minAmountDue, minDaysOverdue,
                PageRequest.of(0, 10001));
        if (page.getTotalElements() > 10000) {
            throw new BusinessException(
                    "Export exceeds maximum limit of 10,000 rows. Please apply filters.");
        }
        return page.getContent();
    }

    private Page<DefaulterDto> getDefaultersPage(String search, Long classId,
                                                 BigDecimal minAmountDue,
                                                 Integer minDaysOverdue, Pageable pageable) {

        Long schoolId = SecurityUtil.schoolId();
        Long effectiveSessionId = validateAndGetSessionId();

        AcademicSession session = sessionRepository.findById(effectiveSessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        LocalDate sessionStart;
        try {
            int startYear = Integer.parseInt(session.getName().split("-")[INT_ZERO]);
            sessionStart = LocalDate.of(startYear, Month.APRIL, 1);
        } catch (Exception e) {
            sessionStart = LocalDate.now().minusMonths(6);
        }

        LocalDate maxPaymentDate = null;
        if (minDaysOverdue != null && minDaysOverdue > 0) {
            maxPaymentDate = LocalDate.now().minusDays(minDaysOverdue);
        }

        BigDecimal effectiveMinAmountDue = (minAmountDue != null && minAmountDue.compareTo(ZERO) > 0)
                ? minAmountDue
                : new BigDecimal("0.01");

        Page<Object[]> rawPage = studentRepository.findDefaulterDetails(
                schoolId,
                effectiveSessionId,
                classId,
                (search != null && search.trim().isEmpty()) ? null : search,
                effectiveMinAmountDue,
                maxPaymentDate,
                sessionStart,
                pageable);

        LocalDate today = LocalDate.now();

        final LocalDate finalSessionStart = sessionStart;

        return rawPage.map(row -> {
            Long studentId = ((Number) row[0]).longValue();
            String firstName = (String) row[1];
            String lastName = (String) row[2];
            String admissionNumber = (String) row[3];
            String contactNumber = (String) row[4];
            String className = (String) row[5];
            String classSection = (String) row[6];

            BigDecimal totalAssigned = toBigDecimal(row[7]);
            BigDecimal totalLateFeeAccrued = toBigDecimal(row[8]);
            BigDecimal totalDiscountAmount = toBigDecimal(row[9]);
            BigDecimal totalSponsorCoveredAmount = toBigDecimal(row[10]);
            BigDecimal totalLateFeeWaived = toBigDecimal(row[11]);
            BigDecimal totalPrincipalPaid = toBigDecimal(row[12]);
            BigDecimal totalLateFeePaid = toBigDecimal(row[13]);
            LocalDate lastPaymentDate = row[14] instanceof java.sql.Date
                    ? ((java.sql.Date) row[14]).toLocalDate()
                    : (LocalDate) row[14];

            BigDecimal pending = computePendingTotals(
                    totalAssigned,
                    totalLateFeeAccrued,
                    totalDiscountAmount,
                    totalSponsorCoveredAmount,
                    totalLateFeeWaived,
                    totalPrincipalPaid,
                    totalLateFeePaid);

            LocalDate overdueFrom = lastPaymentDate != null ? lastPaymentDate : finalSessionStart;
            long daysOverdue = Math.max(ChronoUnit.DAYS.between(overdueFrom, today), 0);

            return DefaulterDto.builder()
                    .studentId(studentId)
                    .studentName(firstName + " " + (lastName != null ? lastName : ""))
                    .admissionNumber(admissionNumber)
                    .className(className != null ? className : "")
                    .classSection(classSection != null ? classSection : "")
                    .amountDue(pending)
                    .lateFeeAccrued(totalLateFeeAccrued)
                    .lastPaymentDate(lastPaymentDate)
                    .daysOverdue(daysOverdue)
                    .parentContact(contactNumber != null ? contactNumber : "")
                    .build();
        });
    }

    private Long validateAndGetSessionId() {
        Long sessionId = SessionContext.getSessionId();

        if (sessionId == null) {
            throw new InvalidOperationException("Session context is missing in request");
        }

        if (!sessionRepository.existsByIdAndSchoolId(sessionId, TenantContext.getSchoolId())) {
            throw new ResourceNotFoundException("Session not found or access denied: " + sessionId);
        }

        return sessionId;
    }

    private BigDecimal computePendingTotals(
            BigDecimal amount,
            BigDecimal lateFeeAccrued,
            BigDecimal totalDiscountAmount,
            BigDecimal sponsorCoveredAmount,
            BigDecimal lateFeeWaived,
            BigDecimal principalPaid,
            BigDecimal lateFeePaid) {
        return FeeMath.computePending(StudentFeeAssignment.builder()
                .amount(amount)
                .lateFeeAccrued(lateFeeAccrued)
                .totalDiscountAmount(totalDiscountAmount)
                .sponsorCoveredAmount(sponsorCoveredAmount)
                .lateFeeWaived(lateFeeWaived)
                .principalPaid(principalPaid)
                .lateFeePaid(lateFeePaid)
                .build());
    }

}
