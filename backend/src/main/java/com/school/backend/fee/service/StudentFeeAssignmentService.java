package com.school.backend.fee.service;

import com.school.backend.common.enums.LateFeeCapType;
import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.fee.dto.StudentFeeAssignRequest;
import com.school.backend.fee.dto.StudentFeeAssignmentDto;
import com.school.backend.fee.entity.FeeStructure;
import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.common.enums.FeeFrequency;
import com.school.backend.fee.repository.FeeStructureRepository;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.repository.AcademicSessionRepository;
import com.school.backend.fee.repository.StudentFundingArrangementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.school.backend.fee.entity.LateFeePolicy;
import com.school.backend.fee.repository.LateFeePolicyRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
    private final StudentFundingArrangementRepository fundingRepository;
    private final FeeCalculationService feeCalculationService;

    // ---------------- ASSIGN ----------------
    @Transactional
    public StudentFeeAssignmentDto assign(StudentFeeAssignRequest req) {

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

        BigDecimal calculatedAmount = fs.getAmount();
        if (fs.getFrequency() == FeeFrequency.MONTHLY) {
            calculatedAmount = calculatedAmount.multiply(new BigDecimal(12));
        }
        final BigDecimal finalAmount = calculatedAmount;

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
    @Transactional(readOnly = true)
    public List<StudentFeeAssignmentDto> listByStudent(Long studentId, Long sessionId) {

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

        // Populate Fee Type Name
        feeStructureRepository.findById(sfa.getFeeStructureId()).ifPresent(fs -> {
            if (fs.getFeeType() != null) {
                dto.setFeeTypeName(fs.getFeeType().getName());
            }
        });

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

        BigDecimal pending = calculatePendingFromPersistedValues(sfa);

        dto.setStatus(pending.compareTo(BigDecimal.ZERO) <= 0 ? "PAID" : "PENDING");
        BigDecimal remainingPrincipal = nz(sfa.getAmount())
                .subtract(nz(sfa.getPrincipalPaid()))
                .subtract(nz(sfa.getTotalDiscountAmount()))
                .subtract(nz(sfa.getSponsorCoveredAmount()));
        if (remainingPrincipal.compareTo(ZERO) < 0) {
            remainingPrincipal = ZERO;
        }
        dto.setRemainingPrincipal(remainingPrincipal.setScale(2, RoundingMode.HALF_UP));

        dto.setActive(sfa.isActive());

        return dto;
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : ZERO;
    }

    private BigDecimal calculatePendingFromPersistedValues(StudentFeeAssignment sfa) {
        return nz(sfa.getAmount())
                .add(nz(sfa.getLateFeeAccrued()))
                .subtract(nz(sfa.getTotalDiscountAmount()))
                .subtract(nz(sfa.getSponsorCoveredAmount()))
                .subtract(nz(sfa.getPrincipalPaid()))
                .subtract(nz(sfa.getLateFeePaid()))
                .subtract(nz(sfa.getLateFeeWaived()));
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
}
