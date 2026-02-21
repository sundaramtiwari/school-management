package com.school.backend.fee.service;

import com.school.backend.common.enums.LateFeeType;
import com.school.backend.common.exception.BusinessException;
import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.fee.dto.FeePaymentDto;
import com.school.backend.fee.dto.FeePaymentRequest;
import com.school.backend.fee.entity.FeePayment;
import com.school.backend.fee.entity.LateFeeLog;
import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.fee.repository.FeePaymentRepository;
import com.school.backend.fee.repository.LateFeeLogRepository;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import com.school.backend.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class FeePaymentService {

    private final FeePaymentRepository paymentRepository;
    private final StudentRepository studentRepository;
    private final SchoolRepository schoolRepository;
    private final StudentFeeAssignmentRepository assignmentRepository;
    private final LateFeeLogRepository lateFeeLogRepository;
    private final LateFeeCalculator lateFeeCalculator;

    // ---------------- PAY ----------------
    @Transactional
    public FeePaymentDto pay(FeePaymentRequest req) {

        if (!studentRepository.existsById(req.getStudentId())) {
            throw new ResourceNotFoundException("Student not found: " + req.getStudentId());
        }

        if (req.getAmountPaid() == null || req.getAmountPaid().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Payment amount must be greater than zero.");
        }

        Long sessionId = req.getSessionId();
        if (sessionId == null) {
            sessionId = schoolRepository.findById(TenantContext.getSchoolId())
                    .map(com.school.backend.school.entity.School::getCurrentSessionId)
                    .orElse(null);
        }
        if (sessionId == null) {
            throw new BusinessException("Session is required for fee payment.");
        }

        // Fetch active assignments for this student and session with PESSIMISTIC lock
        List<StudentFeeAssignment> assignments = assignmentRepository
                .findByStudentIdAndSessionIdWithLock(req.getStudentId(), sessionId);
        if (assignments.isEmpty()) {
            throw new BusinessException("No fee assignments found for this student in current session.");
        }

        BigDecimal totalIncoming = req.getAmountPaid();
        BigDecimal principalToPay = BigDecimal.ZERO;
        BigDecimal lateFeeToPay = BigDecimal.ZERO;

        BigDecimal remainingIncoming = totalIncoming;
        LocalDate effectivePaymentDate = req.getPaymentDate() != null ? req.getPaymentDate() : LocalDate.now();

        // Sort by due date (oldest first), nulls last for legacy data safety
        assignments.sort(Comparator.comparing(a -> a.getDueDate() != null ? a.getDueDate() : LocalDate.MAX));

        for (StudentFeeAssignment assignment : assignments) {
            hydrateDefaults(assignment);
            if (remainingIncoming.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal principalDue = assignment.getAmount()
                    .subtract(assignment.getPrincipalPaid())
                    .subtract(assignment.getTotalDiscountAmount())
                    .subtract(assignment.getSponsorCoveredAmount());
            if (principalDue.compareTo(BigDecimal.ZERO) < 0) {
                principalDue = BigDecimal.ZERO;
            }

            // Calculate and accrue late fee from snapshot at payment time
            BigDecimal incrementalLateFee = lateFeeCalculator.calculateLateFee(assignment, principalDue,
                    effectivePaymentDate);
            if (incrementalLateFee.compareTo(BigDecimal.ZERO) > 0) {
                assignment.setLateFeeAccrued(assignment.getLateFeeAccrued().add(incrementalLateFee));

                // For one-time late fees, mark as applied
                if (assignment.getLateFeeType() == LateFeeType.FLAT ||
                        assignment.getLateFeeType() == LateFeeType.PERCENTAGE) {
                    assignment.setLateFeeApplied(true);
                }

                lateFeeLogRepository.save(LateFeeLog.builder()
                        .schoolId(TenantContext.getSchoolId())
                        .assignmentId(assignment.getId())
                        .computedAmount(incrementalLateFee)
                        .appliedDate(effectivePaymentDate)
                        .reason("Payment-time late fee accrual")
                        .build());
            }

            // Allocate to late fee first
            BigDecimal lateFeeDue = assignment.getLateFeeAccrued().subtract(assignment.getLateFeePaid())
                    .subtract(assignment.getLateFeeWaived());

            if (lateFeeDue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal lateFeeAllocation = remainingIncoming.min(lateFeeDue);
                assignment.setLateFeePaid(assignment.getLateFeePaid().add(lateFeeAllocation));
                lateFeeToPay = lateFeeToPay.add(lateFeeAllocation);
                remainingIncoming = remainingIncoming.subtract(lateFeeAllocation);
            }

            if (remainingIncoming.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            // Allocate to principal
            principalDue = assignment.getAmount()
                    .subtract(assignment.getPrincipalPaid())
                    .subtract(assignment.getTotalDiscountAmount())
                    .subtract(assignment.getSponsorCoveredAmount());
            if (principalDue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal principalAllocation = remainingIncoming.min(principalDue);
                assignment.setPrincipalPaid(assignment.getPrincipalPaid().add(principalAllocation));
                principalToPay = principalToPay.add(principalAllocation);
                remainingIncoming = remainingIncoming.subtract(principalAllocation);
            }
        }

        if (remainingIncoming.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("Payment exceeds outstanding dues by ₹ " + remainingIncoming);
        }

        // Save updated assignments
        assignmentRepository.saveAll(assignments);

        FeePayment payment = FeePayment.builder()
                .studentId(req.getStudentId())
                .sessionId(sessionId)
                .principalPaid(principalToPay)
                .lateFeePaid(lateFeeToPay)
                .paymentDate(effectivePaymentDate)
                .mode(req.getMode())
                .transactionReference(req.getTransactionReference())
                .remarks(req.getRemarks())
                .schoolId(TenantContext.getSchoolId())
                .build();

        return toDto(paymentRepository.save(payment));
    }

    // ---------------- HISTORY ----------------
    @Transactional(readOnly = true)
    public List<FeePaymentDto> getHistory(Long studentId) {

        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found: " + studentId);
        }

        return paymentRepository.findByStudentId(studentId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeePaymentDto> getRecentPayments(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return paymentRepository.findRecentPayments(TenantContext.getSchoolId(), pageable)
                .stream()
                .map(this::toDto)
                .toList();
    }

    // ---------------- MAPPER ----------------
    private FeePaymentDto toDto(FeePayment p) {

        FeePaymentDto dto = new FeePaymentDto();

        dto.setId(p.getId());
        dto.setStudentId(p.getStudentId());
        dto.setSessionId(p.getSessionId());
        dto.setPrincipalPaid(p.getPrincipalPaid());
        dto.setLateFeePaid(p.getLateFeePaid());
        dto.setAmountPaid(p.getPrincipalPaid().add(p.getLateFeePaid()));
        dto.setPaymentDate(p.getPaymentDate());
        dto.setMode(p.getMode());
        dto.setTransactionReference(p.getTransactionReference());
        dto.setRemarks(p.getRemarks());

        return dto;
    }

    private void hydrateDefaults(StudentFeeAssignment assignment) {
        if (assignment.getLateFeeAccrued() == null) {
            assignment.setLateFeeAccrued(BigDecimal.ZERO);
        }
        if (assignment.getLateFeePaid() == null) {
            assignment.setLateFeePaid(BigDecimal.ZERO);
        }
        if (assignment.getLateFeeWaived() == null) {
            assignment.setLateFeeWaived(BigDecimal.ZERO);
        }
        if (assignment.getPrincipalPaid() == null) {
            assignment.setPrincipalPaid(BigDecimal.ZERO);
        }
        if (assignment.getTotalDiscountAmount() == null) {
            assignment.setTotalDiscountAmount(BigDecimal.ZERO);
        }
        if (assignment.getAmount() == null) {
            assignment.setAmount(BigDecimal.ZERO);
        }
    }
}
