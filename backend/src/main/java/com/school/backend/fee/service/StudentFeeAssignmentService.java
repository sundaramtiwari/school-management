package com.school.backend.fee.service;

import com.school.backend.common.enums.LateFeeCapType;
import com.school.backend.common.exception.InvalidOperationException;
import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.common.enums.FeeFrequency;
import com.school.backend.common.tenant.SessionContext;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.core.student.entity.StudentEnrollment;
import com.school.backend.core.student.repository.StudentEnrollmentRepository;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.fee.dto.StudentFeeAssignRequest;
import com.school.backend.fee.dto.StudentFeeAssignmentDto;
import com.school.backend.fee.entity.FeeStructure;
import com.school.backend.fee.entity.LateFeePolicy;
import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.fee.repository.FeeStructureRepository;
import com.school.backend.fee.repository.LateFeePolicyRepository;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import com.school.backend.fee.repository.StudentFundingArrangementRepository;
import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.repository.AcademicSessionRepository;
import com.school.backend.transport.entity.TransportEnrollment;
import com.school.backend.transport.repository.TransportEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentFeeAssignmentService {
    private static final BigDecimal ZERO = BigDecimal.ZERO;


    private final StudentFeeAssignmentRepository assignmentRepository;
    private final FeeStructureRepository feeStructureRepository;
    private final StudentRepository studentRepository;
    private final LateFeePolicyRepository lateFeePolicyRepository;
    private final AcademicSessionRepository academicSessionRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final TransportEnrollmentRepository transportEnrollmentRepository;
    private final StudentFundingArrangementRepository fundingRepository;
    private final FeeCalculationService feeCalculationService;

    // ---------------- ASSIGN ----------------
    @Transactional
    public StudentFeeAssignmentDto assign(StudentFeeAssignRequest req) {
        Long effectiveSessionId = requireSessionId();
        if (req.getSessionId() != null && !req.getSessionId().equals(effectiveSessionId)) {
            throw new InvalidOperationException("Session mismatch between request and context");
        }
        req.setSessionId(effectiveSessionId);

        if (!studentRepository.existsById(req.getStudentId())) {
            throw new ResourceNotFoundException("Student not found: " + req.getStudentId());
        }

        FeeStructure fs = feeStructureRepository.findById(req.getFeeStructureId())
                .orElseThrow(() -> new ResourceNotFoundException("FeeStructure not found: " + req.getFeeStructureId()));

        // Prevent duplicate assignment
        boolean exists = assignmentRepository
                .existsByStudentIdAndFeeStructureIdAndSessionId(
                        req.getStudentId(),
                        req.getFeeStructureId(),
                        req.getSessionId());

        if (exists) {
            throw new IllegalStateException("Fee already assigned to student for this session");
        }

        final BigDecimal finalAmount = feeCalculationService.calculateAssignableAmount(fs, req.getStudentId());

        // --- Derive Due Date from session start/end or use request override ---
        LocalDate dueDate = req.getDueDate();
        if (dueDate == null) {
            int dueDay = fs.getDueDayOfMonth() != null ? fs.getDueDayOfMonth() : 10;
            dueDate = resolveDerivedDueDate(req.getSessionId(), fs.getSchoolId(), dueDay);
        }

        // --- Snapshot Late Fee Policy ---
        LateFeePolicy policy = lateFeePolicyRepository.findByFeeStructureId(fs.getId()).orElse(null);

        StudentFeeAssignment assignment = StudentFeeAssignment.builder()
                .studentId(req.getStudentId())
                .feeStructureId(fs.getId())
                .sessionId(req.getSessionId())
                .amount(finalAmount)
                .dueDate(dueDate)
                .lateFeeType(policy != null ? policy.getType() : null)
                .lateFeeValue(policy != null ? policy.getAmountValue() : BigDecimal.ZERO)
                .lateFeeGraceDays(policy != null ? policy.getGraceDays() : 0)
                .lateFeeCapType(policy != null ? policy.getCapType() : LateFeeCapType.NONE)
                .lateFeeCapValue(policy != null ? policy.getCapValue() : BigDecimal.ZERO)
                .schoolId(TenantContext.getSchoolId())
                .active(true)
                .build();

        // --- Snapshot Funding ---
        BigDecimal discountSnapshot = nz(assignment.getTotalDiscountAmount());
        BigDecimal fundingSnapshot = fundingRepository
                .findActiveByStudentAndSession(req.getStudentId(), req.getSessionId())
                .map(f -> feeCalculationService.calculateFundingSnapshot(finalAmount, discountSnapshot, f))
                .orElse(BigDecimal.ZERO);
        assignment.setSponsorCoveredAmount(fundingSnapshot);

        return toDto(assignmentRepository.save(assignment));
    }

    // ---------------- LIST ----------------

    /**
     * Lists fee assignments for a student in the current session context.
     *
     * @param studentId ID of the student whose fee assignments are to be listed.
     * @return List of StudentFeeAssignmentDto for the specified student and current session.
     */
    @Transactional(readOnly = true)
    public List<StudentFeeAssignmentDto> listByStudent(Long studentId) {
        return listByStudent(studentId, requireSessionId());
    }

    /**
     * Lists fee assignments for a student within a specific session context.
     * This is used internally to support session override scenarios, but is not exposed directly via API.
     * The session context is required to ensure we are showing assignments relevant to the current academic session,
     * and to prevent accidental cross-session data exposure.
     *
     * @param studentId ID of the student whose fee assignments are to be listed.
     * @param sessionId Caller should ensure this is a valid session for the current school,
     *                  typically by using requireSessionId() or validating against SessionResolver.
     * @return List of StudentFeeAssignmentDto for the specified student and session.
     */
    @Transactional(readOnly = true)
    private List<StudentFeeAssignmentDto> listByStudent(Long studentId, Long sessionId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found: " + studentId);
        }

        return assignmentRepository
                .findByStudentIdAndSessionId(studentId, sessionId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    // ---------------- MAPPER ----------------
    public StudentFeeAssignmentDto toDto(StudentFeeAssignment sfa) {

        StudentFeeAssignmentDto dto = new StudentFeeAssignmentDto();

        dto.setId(sfa.getId());
        dto.setStudentId(sfa.getStudentId());
        dto.setFeeStructureId(sfa.getFeeStructureId());
        dto.setSessionId(sfa.getSessionId());
        dto.setAmount(sfa.getAmount());
        dto.setAnnualAmount(nz(sfa.getAmount()).setScale(2, RoundingMode.HALF_UP));

        // Populate Fee Type Name
        FeeFrequency frequency = FeeFrequency.ONE_TIME;
        int periodsPerYear = FeeFrequency.ONE_TIME.getPeriodsPerYear();
        feeStructureRepository.findById(sfa.getFeeStructureId()).ifPresent(fs -> {
            if (fs.getFeeType() != null) {
                dto.setFeeTypeName(fs.getFeeType().getName());
            }
            dto.setFrequency(fs.getFrequency());
            dto.setPeriodsPerYear(fs.getFrequency() != null ? fs.getFrequency().getPeriodsPerYear() : 1);
        });
        if (dto.getFrequency() != null) {
            frequency = dto.getFrequency();
            periodsPerYear = Math.max(1, dto.getPeriodsPerYear());
        } else {
            dto.setFrequency(frequency);
            dto.setPeriodsPerYear(periodsPerYear);
        }

        dto.setDueDate(sfa.getDueDate());
        dto.setLateFeeType(sfa.getLateFeeType());
        dto.setLateFeeValue(sfa.getLateFeeValue());
        dto.setLateFeeGraceDays(sfa.getLateFeeGraceDays());
        dto.setLateFeeCapType(sfa.getLateFeeCapType());
        dto.setLateFeeCapValue(sfa.getLateFeeCapValue());

        dto.setLateFeeApplied(sfa.isLateFeeApplied());
        dto.setLateFeeAccrued(sfa.getLateFeeAccrued());
        dto.setLateFeePaid(sfa.getLateFeePaid());
        dto.setLateFeeWaived(sfa.getLateFeeWaived());
        dto.setTotalDiscountAmount(sfa.getTotalDiscountAmount());
        dto.setSponsorCoveredAmount(sfa.getSponsorCoveredAmount());
        dto.setPrincipalPaid(sfa.getPrincipalPaid());

        BigDecimal pending = FeeMath.computePending(sfa);
        BigDecimal annualAmount = nz(sfa.getAmount()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal principalPaid = nz(sfa.getPrincipalPaid());
        BigDecimal totalDiscountAmount = nz(sfa.getTotalDiscountAmount());
        BigDecimal sponsorCoveredAmount = nz(sfa.getSponsorCoveredAmount());
        BigDecimal lateFeeAccrued = nz(sfa.getLateFeeAccrued());
        BigDecimal lateFeePaid = nz(sfa.getLateFeePaid());
        BigDecimal lateFeeWaived = nz(sfa.getLateFeeWaived());
        dto.setNextDueDate(null);

        if (frequency != FeeFrequency.ONE_TIME) {
            int safePeriodsPerYear = Math.max(1, periodsPerYear);
            int periodLengthMonths = Math.max(1, 12 / safePeriodsPerYear);
            dto.setPeriodsPerYear(safePeriodsPerYear);

            AcademicSession academicSession = academicSessionRepository.findById(sfa.getSessionId()).orElse(null);
            LocalDate sessionStartDate = academicSession != null ? academicSession.getStartDate() : null;
            LocalDate sessionEndDate = academicSession != null ? academicSession.getEndDate() : null;

            LocalDate baseDate;
            boolean transportBased = false;
            Long schoolId = TenantContext.getSchoolId();
            if (schoolId != null) {
                transportBased = feeStructureRepository.findById(sfa.getFeeStructureId())
                        .map(FeeStructure::getFeeType)
                        .map(ft -> ft != null && ft.isTransportBased())
                        .orElse(false);
            }

            if (transportBased && schoolId != null) {
                TransportEnrollment transportEnrollment = transportEnrollmentRepository
                        .findByStudentIdAndSessionIdAndSchoolIdAndActiveTrue(
                                sfa.getStudentId(), sfa.getSessionId(), schoolId)
                        .orElse(null);
                baseDate = transportEnrollment != null && transportEnrollment.getCreatedAt() != null
                        ? transportEnrollment.getCreatedAt().toLocalDate()
                        : null;
            } else {
                StudentEnrollment enrollment = studentEnrollmentRepository
                        .findFirstByStudentIdAndSessionIdAndActiveTrue(sfa.getStudentId(), sfa.getSessionId())
                        .orElse(null);
                LocalDate enrollmentDate = enrollment != null ? enrollment.getEnrollmentDate() : null;
                LocalDate enrollmentStartDate = enrollment != null ? enrollment.getStartDate() : null;
                baseDate = firstNonNullDate(enrollmentDate, enrollmentStartDate, sessionStartDate);
            }

            LocalDate today = LocalDate.now();
            int periodsElapsed;
            if (baseDate == null || today.isBefore(baseDate)) {
                periodsElapsed = 0;
            } else {
                long zeroBasedMonthsElapsed = ChronoUnit.MONTHS.between(YearMonth.from(baseDate), YearMonth.from(today));
                periodsElapsed = (int) (Math.floorDiv(zeroBasedMonthsElapsed, periodLengthMonths) + 1);
            }

            periodsElapsed = Math.max(0, Math.min(periodsElapsed, safePeriodsPerYear));
            dto.setPeriodsElapsed(periodsElapsed);

            LocalDate nextDueDate;
            if (baseDate == null) {
                nextDueDate = null;
            } else if (today.isBefore(baseDate)) {
                nextDueDate = baseDate;
            } else if (periodsElapsed < safePeriodsPerYear) {
                long monthsToAdd = (long) periodLengthMonths * periodsElapsed;
                nextDueDate = baseDate.plusMonths(monthsToAdd);
            } else {
                nextDueDate = null;
            }
            if (sessionEndDate != null && nextDueDate != null && nextDueDate.isAfter(sessionEndDate)) {
                nextDueDate = null;
            }
            dto.setNextDueDate(nextDueDate);

            BigDecimal amountPerPeriod = annualAmount
                    .divide(BigDecimal.valueOf(safePeriodsPerYear), 2, RoundingMode.HALF_UP);
            dto.setAmountPerPeriod(amountPerPeriod);

            BigDecimal dueTillDate = amountPerPeriod.multiply(BigDecimal.valueOf(periodsElapsed))
                    .setScale(2, RoundingMode.HALF_UP);
            dueTillDate = dueTillDate.min(annualAmount);
            if (dueTillDate.compareTo(ZERO) < 0) {
                dueTillDate = ZERO;
            }
            dto.setDueTillDate(dueTillDate);

            BigDecimal pendingTillDate = dueTillDate
                    .add(lateFeeAccrued)
                    .subtract(totalDiscountAmount)
                    .subtract(sponsorCoveredAmount)
                    .subtract(principalPaid)
                    .subtract(lateFeePaid)
                    .subtract(lateFeeWaived);
            if (pendingTillDate.compareTo(ZERO) < 0) {
                pendingTillDate = ZERO;
            }
            dto.setPendingTillDate(pendingTillDate.setScale(2, RoundingMode.HALF_UP));
        } else {
            dto.setPeriodsPerYear(1);
            dto.setPeriodsElapsed(1);
            dto.setAmountPerPeriod(annualAmount);

            BigDecimal dueTillDate = principalPaid.compareTo(annualAmount) >= 0 ? ZERO : annualAmount;
            dto.setDueTillDate(dueTillDate.setScale(2, RoundingMode.HALF_UP));
            BigDecimal pendingTillDate = dueTillDate
                    .add(lateFeeAccrued)
                    .subtract(totalDiscountAmount)
                    .subtract(sponsorCoveredAmount)
                    .subtract(principalPaid)
                    .subtract(lateFeePaid)
                    .subtract(lateFeeWaived);
            if (pendingTillDate.compareTo(ZERO) < 0) {
                pendingTillDate = ZERO;
            }
            dto.setPendingTillDate(pendingTillDate.setScale(2, RoundingMode.HALF_UP));
        }

        BigDecimal remainingForSession = annualAmount
                .subtract(principalPaid)
                .subtract(totalDiscountAmount)
                .subtract(sponsorCoveredAmount);
        if (remainingForSession.compareTo(ZERO) < 0) {
            remainingForSession = ZERO;
        }
        dto.setRemainingForSession(remainingForSession.setScale(2, RoundingMode.HALF_UP));

        dto.setStatus(pending.compareTo(BigDecimal.ZERO) <= 0 ? "PAID" : "PENDING");

        dto.setActive(sfa.isActive());

        return dto;
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : ZERO;
    }

    private LocalDate firstNonNullDate(LocalDate first, LocalDate second, LocalDate third) {
        if (first != null) {
            return first;
        }
        if (second != null) {
            return second;
        }
        return third;
    }

    private LocalDate resolveDerivedDueDate(Long sessionId, Long schoolId, Integer dueDayOfMonth) {
        AcademicSession session = academicSessionRepository.findById(sessionId)
                .filter(s -> schoolId.equals(s.getSchoolId()))
                .orElse(null);

        if (session == null || session.getStartDate() == null) {
            return LocalDate.now().plusDays(10);
        }

        LocalDate start = session.getStartDate();
        LocalDate end = session.getEndDate() != null ? session.getEndDate() : start.plusYears(1).minusDays(1);
        int targetDay = Math.max(1, Math.min(dueDayOfMonth != null ? dueDayOfMonth : 10, 31));

        LocalDate candidate = clampDay(start.withDayOfMonth(1), targetDay);
        if (candidate.isBefore(start)) {
            candidate = clampDay(start.plusMonths(1).withDayOfMonth(1), targetDay);
        }
        if (candidate.isAfter(end)) {
            return end;
        }
        return candidate;
    }

    private LocalDate clampDay(LocalDate baseMonth, int day) {
        int lastDay = baseMonth.lengthOfMonth();
        return baseMonth.withDayOfMonth(Math.min(day, lastDay));
    }

    private Long requireSessionId() {
        Long sessionId = SessionContext.getSessionId();
        if (sessionId == null) {
            throw new InvalidOperationException("Session context is missing in request");
        }
        return sessionId;
    }
}
