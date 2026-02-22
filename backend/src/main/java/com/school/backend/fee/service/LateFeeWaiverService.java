package com.school.backend.fee.service;

import com.school.backend.common.exception.BusinessException;
import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.fee.entity.FeeAdjustment;
import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.fee.repository.FeeAdjustmentRepository;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class LateFeeWaiverService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final StudentFeeAssignmentRepository assignmentRepository;
    private final FeeAdjustmentRepository feeAdjustmentRepository;

    @Transactional
    public StudentFeeAssignment waiveLateFee(
            Long assignmentId,
            BigDecimal waiverAmount,
            String remarks,
            Long schoolId,
            Long staffId) {

        StudentFeeAssignment assignment = assignmentRepository.findByIdAndSchoolId(assignmentId, schoolId)
                .orElseThrow(() -> resolveAssignmentNotFound(assignmentId));

        if (waiverAmount == null || waiverAmount.compareTo(ZERO) <= 0) {
            throw new BusinessException("Waiver amount must be greater than zero.");
        }

        BigDecimal outstandingLateFee = nz(assignment.getLateFeeAccrued())
                .subtract(nz(assignment.getLateFeePaid()))
                .subtract(nz(assignment.getLateFeeWaived()));

        if (outstandingLateFee.compareTo(ZERO) <= 0) {
            throw new BusinessException("No late fee is pending to waive.");
        }

        BigDecimal normalizedWaiverAmount = waiverAmount.setScale(2, java.math.RoundingMode.HALF_UP);
        if (normalizedWaiverAmount.compareTo(outstandingLateFee) > 0) {
            throw new BusinessException("Waiver amount exceeds waivable late fee.");
        }

        assignment.setLateFeeWaived(nz(assignment.getLateFeeWaived()).add(normalizedWaiverAmount));
        StudentFeeAssignment updatedAssignment = assignmentRepository.save(assignment);

        feeAdjustmentRepository.save(FeeAdjustment.builder()
                .assignmentId(updatedAssignment.getId())
                .type(FeeAdjustment.AdjustmentType.LATE_FEE_WAIVER)
                .amount(normalizedWaiverAmount)
                .reason(remarks != null ? remarks.trim() : null)
                .createdByStaff(staffId != null ? String.valueOf(staffId) : null)
                .schoolId(schoolId)
                .build());

        return updatedAssignment;
    }

    private ResourceNotFoundException resolveAssignmentNotFound(Long assignmentId) {
        if (assignmentRepository.existsAnyById(assignmentId)) {
            throw new BusinessException("Assignment belongs to another tenant.");
        }
        return new ResourceNotFoundException("Student fee assignment not found: " + assignmentId);
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : ZERO;
    }
}
