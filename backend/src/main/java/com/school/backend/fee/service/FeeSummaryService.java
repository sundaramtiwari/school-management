package com.school.backend.fee.service;

import com.school.backend.common.exception.ResourceNotFoundException;
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
import com.school.backend.school.entity.School;
import com.school.backend.school.repository.AcademicSessionRepository;
import com.school.backend.school.repository.SchoolRepository;
import com.school.backend.user.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeeSummaryService {

    private static final int INT_ZERO = 0;
    private static final int SCALE_2 = 2;
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final RoundingMode ROUNDING_MODE_HALF_UP = RoundingMode.HALF_UP;

    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final StudentFeeAssignmentRepository assignmentRepository;
    private final FeePaymentRepository paymentRepository;
    private final AcademicSessionRepository sessionRepository;
    private final SchoolRepository schoolRepository;
    private final LateFeeCalculator lateFeeCalculator;

    // ---------------------------------------------------
    // DASHBOARD STATS
    // ---------------------------------------------------
    @Transactional(readOnly = true)
    public FeeStatsDto getDashboardStats(Long sessionId) {

        Long schoolId = SecurityUtil.schoolId();

        if (!sessionRepository.existsByIdAndSchoolId(sessionId, schoolId)) {
            throw new ResourceNotFoundException("Session not found: " + sessionId);
        }

        // 1. Today's Collection
        BigDecimal todayPaid = paymentRepository
                .sumTotalPaidBySchoolIdAndPaymentDate(schoolId, LocalDate.now());

        BigDecimal todayCollection = todayPaid != null ? todayPaid : ZERO;

        // 2. Total Students (SESSION AWARE)
        long totalStudents = enrollmentRepository
                .countBySchoolIdAndSessionIdAndActiveTrue(schoolId, sessionId);

        // 3. Optimized Pending Calculation (NO N+1)
        BigDecimal totalAssigned = assignmentRepository
                .sumTotalAssignedBySchoolAndSession(schoolId, sessionId);

        BigDecimal totalPaid = paymentRepository
                .sumTotalPaidBySchoolAndSession(schoolId, sessionId);

        BigDecimal totalPending = (totalAssigned != null ? totalAssigned : ZERO)
                .subtract(totalPaid != null ? totalPaid : ZERO);

        if (totalPending.compareTo(ZERO) < INT_ZERO) {
            totalPending = ZERO;
        }

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
    public FeeSummaryDto getStudentFeeSummary(Long studentId, Long sessionId) {

        Long schoolId = SecurityUtil.schoolId();

        Student student = studentRepository
                .findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));

        AcademicSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        List<StudentFeeAssignment> assignments = assignmentRepository.findByStudentIdAndSessionId(studentId,
                sessionId);

        BigDecimal totalFeeAccrued = assignments.stream()
                .map(a -> nz(a.getAmount()))
                .reduce(ZERO, java.math.BigDecimal::add);

        BigDecimal totalLateFeeAccrued = assignments.stream()
                .map(sfa -> {
                    BigDecimal unpaid = nz(sfa.getAmount())
                            .subtract(nz(sfa.getPrincipalPaid()))
                            .subtract(nz(sfa.getTotalDiscountAmount()))
                            .subtract(nz(sfa.getSponsorCoveredAmount()));
                    BigDecimal incremental = lateFeeCalculator.calculateLateFee(sfa,
                            unpaid, LocalDate.now());
                    return nz(sfa.getLateFeeAccrued()).add(incremental);
                })
                .reduce(ZERO, BigDecimal::add);

        BigDecimal totalLateFeePaid = assignments.stream()
                .map(a -> nz(a.getLateFeePaid()))
                .reduce(ZERO, BigDecimal::add);

        BigDecimal totalPrincipalPaid = assignments.stream()
                .map(a -> nz(a.getPrincipalPaid()))
                .reduce(ZERO, BigDecimal::add);

        BigDecimal totalPaid = totalPrincipalPaid.add(totalLateFeePaid);

        BigDecimal pending = totalFeeAccrued.add(totalLateFeeAccrued)
                .subtract(totalPaid);

        if (pending.compareTo(ZERO) < INT_ZERO) {
            pending = ZERO;
        }

        FeeSummaryDto dto = new FeeSummaryDto();
        dto.setStudentId(studentId);
        dto.setStudentName(student.getFirstName() + " " +
                (student.getLastName() != null ? student.getLastName() : ""));
        dto.setSession(session.getName());
        dto.setTotalFee(totalFeeAccrued.setScale(SCALE_2, ROUNDING_MODE_HALF_UP));
        dto.setTotalPaid(totalPaid.setScale(SCALE_2, ROUNDING_MODE_HALF_UP));
        dto.setTotalLateFeeAccrued(totalLateFeeAccrued.setScale(SCALE_2, ROUNDING_MODE_HALF_UP));
        dto.setTotalLateFeePaid(totalLateFeePaid.setScale(SCALE_2, ROUNDING_MODE_HALF_UP));
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
            Long sessionId = (stats[INT_ZERO] instanceof Number) ? ((Number) stats[INT_ZERO]).longValue() : null;
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

            // Total Paid = Principal Paid + Late Fee Paid
            BigDecimal sessionTotalPaid = totalPrincipalPaid.add(totalLateFeePaid);
            summary.setTotalPaid(sessionTotalPaid.setScale(SCALE_2, ROUNDING_MODE_HALF_UP));

            summary.setTotalLateFeeAccrued(totalLateFeeAccrued.setScale(SCALE_2, ROUNDING_MODE_HALF_UP));
            summary.setTotalLateFeePaid(totalLateFeePaid.setScale(SCALE_2, ROUNDING_MODE_HALF_UP));

            // Pending = (Gross Fee - Discount - Sponsor) + Accrued Late Fee - (Waived Late
            // Fee + Paid Principal + Paid Late Fee)
            // Actually: Pending = (Gross - Discount - Sponsor - PrincipalPaid) + (Accrued -
            // Waived - PaidLateFee)
            BigDecimal netPrincipal = totalFee.subtract(totalDiscount).subtract(totalSponsor);
            BigDecimal netLateFee = totalLateFeeAccrued.subtract(totalLateFeeWaived);

            BigDecimal pending = netPrincipal.subtract(totalPrincipalPaid)
                    .add(netLateFee.subtract(totalLateFeePaid));

            if (pending.compareTo(ZERO) < INT_ZERO) {
                pending = ZERO;
            }

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
        if (val instanceof BigDecimal)
            return (BigDecimal) val;
        return new BigDecimal(val.toString());
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : ZERO;
    }

    // ---------------------------------------------------
    // TOP DEFAULTERS
    // ---------------------------------------------------
    @Transactional(readOnly = true)
    public List<FeeSummaryDto> getTopDefaulters(Long sessionId, int limit) {

        Long schoolId = SecurityUtil.schoolId();

        if (!sessionRepository.existsByIdAndSchoolId(sessionId, schoolId)) {
            throw new ResourceNotFoundException("Session not found: " + sessionId);
        }

        // 1. Load students (session aware)
        List<Student> students = studentRepository
                .findBySchoolIdAndSessionId(schoolId, sessionId, Pageable.unpaged())
                .getContent();

        if (students.isEmpty())
            return List.of();

        // 2. Aggregated Assigned
        var assignedRaw = assignmentRepository
                .sumAssignedGroupedByStudent(schoolId, sessionId);

        var assignedMap = assignedRaw.stream()
                .collect(Collectors.toMap(
                        r -> (Long) r[INT_ZERO],
                        r -> (java.math.BigDecimal) r[1]));

        // 3. Aggregated Paid
        var paidRaw = paymentRepository
                .sumPaidGroupedByStudent(schoolId, sessionId);

        var paidMap = paidRaw.stream()
                .collect(Collectors.toMap(
                        r -> (Long) r[INT_ZERO],
                        r -> (java.math.BigDecimal) r[1]));

        String sessionName = sessionRepository.findById(sessionId)
                .map(AcademicSession::getName)
                .orElse("");

        List<FeeSummaryDto> results = new ArrayList<>();

        for (Student student : students) {

            Long studentId = student.getId();

            BigDecimal totalAssigned = assignedMap.getOrDefault(studentId,
                    ZERO);
            BigDecimal totalPaid = paidMap.getOrDefault(studentId, ZERO);

            BigDecimal pending = totalAssigned.subtract(totalPaid);
            if (pending.compareTo(ZERO) < INT_ZERO) {
                pending = ZERO;
            }

            if (pending.compareTo(ZERO) <= INT_ZERO)
                continue;

            FeeSummaryDto dto = new FeeSummaryDto();
            dto.setStudentId(studentId);
            dto.setStudentName(student.getFirstName() + " " +
                    (student.getLastName() != null ? student.getLastName() : ""));
            dto.setSession(sessionName);
            dto.setTotalFee(totalAssigned);
            dto.setTotalPaid(totalPaid);
            dto.setPendingFee(pending);
            dto.setFeePending(true);

            results.add(dto);
        }

        return results.stream()
                .sorted((a, b) -> b.getPendingFee().compareTo(a.getPendingFee()))
                .limit(limit)
                .toList();
    }

    // ---------------------------------------------------
    // ALL DEFAULTERS
    // ---------------------------------------------------
    @Transactional(readOnly = true)
    public List<DefaulterDto> getAllDefaulters() {

        Long schoolId = SecurityUtil.schoolId();

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        if (school.getCurrentSessionId() == null) {
            return List.of();
        }

        Long sessionId = school.getCurrentSessionId();

        AcademicSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        LocalDate sessionStart;
        try {
            int startYear = Integer.parseInt(session.getName().split("-")[INT_ZERO]);
            sessionStart = LocalDate.of(startYear, Month.APRIL, 1);
        } catch (Exception e) {
            sessionStart = LocalDate.now().minusMonths(6);
        }

        LocalDate today = LocalDate.now();

        // 1. Load students (session aware)
        List<Student> students = studentRepository
                .findBySchoolIdAndSessionId(schoolId, sessionId, Pageable.unpaged())
                .getContent();

        if (students.isEmpty())
            return List.of();

        // 2. Aggregated Assigned Amounts
        var assignedRaw = assignmentRepository
                .sumAssignedGroupedByStudent(schoolId, sessionId);

        var assignedMap = assignedRaw.stream()
                .collect(Collectors.toMap(
                        r -> (Long) r[INT_ZERO],
                        r -> (java.math.BigDecimal) r[1]));

        var lateFeeAccruedMap = assignedRaw.stream()
                .collect(Collectors.toMap(
                        r -> (Long) r[INT_ZERO],
                        r -> (java.math.BigDecimal) r[SCALE_2]));

        // 3. Aggregated Paid Amounts
        var paidRaw = paymentRepository
                .sumPaidGroupedByStudent(schoolId, sessionId);

        var paidMap = paidRaw.stream()
                .collect(Collectors.toMap(
                        r -> (Long) r[INT_ZERO],
                        r -> (java.math.BigDecimal) r[1]));

        // 4. Last Payment Dates
        var lastPaymentRaw = paymentRepository
                .findLastPaymentDateGroupedByStudent(schoolId, sessionId);

        var lastPaymentMap = lastPaymentRaw.stream()
                .collect(Collectors.toMap(
                        r -> (Long) r[INT_ZERO],
                        r -> (LocalDate) r[1]));

        List<DefaulterDto> defaulters = new ArrayList<>();

        for (Student student : students) {

            Long studentId = student.getId();

            BigDecimal totalAssigned = assignedMap.getOrDefault(studentId,
                    ZERO);
            BigDecimal totalLateFeeAccrued = lateFeeAccruedMap.getOrDefault(studentId,
                    ZERO);
            BigDecimal totalPaid = paidMap.getOrDefault(studentId, ZERO);

            BigDecimal pending = totalAssigned.add(totalLateFeeAccrued).subtract(totalPaid);
            if (pending.compareTo(ZERO) < INT_ZERO) {
                pending = ZERO;
            }

            if (pending.compareTo(ZERO) <= INT_ZERO)
                continue;

            LocalDate lastPaymentDate = lastPaymentMap.get(studentId);

            LocalDate overdueFrom = lastPaymentDate != null
                    ? lastPaymentDate
                    : sessionStart;

            long daysOverdue = Math.max(
                    ChronoUnit.DAYS.between(overdueFrom, today), INT_ZERO);

            String className = "";
            String classSection = "";

            if (student.getCurrentClass() != null) {
                className = student.getCurrentClass().getName();
                classSection = student.getCurrentClass().getSection();
            }

            defaulters.add(DefaulterDto.builder()
                    .studentId(studentId)
                    .studentName(student.getFirstName() + " " +
                            (student.getLastName() != null ? student.getLastName() : ""))
                    .admissionNumber(student.getAdmissionNumber())
                    .className(className)
                    .classSection(classSection != null ? classSection : "")
                    .amountDue(pending)
                    .lateFeeAccrued(totalLateFeeAccrued)
                    .lastPaymentDate(lastPaymentDate)
                    .daysOverdue(daysOverdue)
                    .parentContact(student.getContactNumber() != null
                            ? student.getContactNumber()
                            : "")
                    .build());
        }

        defaulters.sort((a, b) -> b.getAmountDue().compareTo(a.getAmountDue()));

        return defaulters;
    }

}
