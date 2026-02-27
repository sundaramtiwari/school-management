package com.school.backend.fee.service;

import com.school.backend.common.enums.DiscountType;
import com.school.backend.common.exception.BusinessException;
import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.fee.dto.FeeDiscountApplyResponse;
import com.school.backend.fee.entity.DiscountDefinition;
import com.school.backend.fee.entity.FeeAdjustment;
import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.fee.repository.DiscountDefinitionRepository;
import com.school.backend.fee.repository.FeeAdjustmentRepository;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class FeeDiscountService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final StudentFeeAssignmentRepository assignmentRepository;
    private final DiscountDefinitionRepository discountDefinitionRepository;
    private final FeeAdjustmentRepository feeAdjustmentRepository;
    private final FundingSnapshotService fundingSnapshotService;

    @Transactional
    public FeeDiscountApplyResponse applyDiscount(
            Long assignmentId,
            Long discountDefinitionId,
            Long schoolId,
            String remarks,
            Long staffId) {

        StudentFeeAssignment assignment = assignmentRepository.findByIdAndSchoolId(assignmentId, schoolId)
                .orElseThrow(() -> resolveAssignmentNotFound(assignmentId));

        DiscountDefinition discountDefinition = discountDefinitionRepository.findByIdAndSchoolId(discountDefinitionId,
                schoolId)
                .orElseThrow(() -> resolveDiscountNotFound(discountDefinitionId));

        if (!discountDefinition.isActive()) {
            throw new BusinessException("Discount definition is not active.");
        }

        if (feeAdjustmentRepository.existsByAssignmentIdAndDiscountDefinitionIdAndType(
                assignmentId, discountDefinitionId, FeeAdjustment.AdjustmentType.DISCOUNT)) {
            throw new IllegalStateException("This discount has already been applied to this fee.");
        }

        BigDecimal remainingPrincipal = calculateRemainingPrincipal(assignment);
        if (remainingPrincipal.compareTo(ZERO) <= 0) {
            throw new IllegalStateException("No principal available for discount.");
        }

        BigDecimal requestedDiscount = calculateDiscountAmount(discountDefinition, assignment.getAmount());
        if (requestedDiscount.compareTo(ZERO) <= 0) {
            throw new BusinessException("Calculated discount must be greater than zero.");
        }
        BigDecimal appliedDiscount = requestedDiscount.min(remainingPrincipal);
        boolean capped = requestedDiscount.compareTo(appliedDiscount) > 0;

        BigDecimal updatedTotalDiscount = nz(assignment.getTotalDiscountAmount()).add(appliedDiscount);
        assignment.setTotalDiscountAmount(updatedTotalDiscount);
        fundingSnapshotService.recalculateAndUpdateFundingSnapshot(assignment);
        BigDecimal updatedSponsorCoveredAmount = nz(assignment.getSponsorCoveredAmount());

        BigDecimal principalAfterDiscount = nz(assignment.getAmount())
                .subtract(nz(assignment.getPrincipalPaid()))
                .subtract(updatedTotalDiscount)
                .subtract(updatedSponsorCoveredAmount);
        if (principalAfterDiscount.compareTo(ZERO) < 0) {
            throw new BusinessException("Discount cannot reduce principal below zero.");
        }

        feeAdjustmentRepository.save(FeeAdjustment.builder()
                .assignmentId(assignment.getId())
                .type(FeeAdjustment.AdjustmentType.DISCOUNT)
                .amount(appliedDiscount)
                .discountDefinitionId(discountDefinition.getId())
                .discountNameSnapshot(discountDefinition.getName())
                .discountTypeSnapshot(discountDefinition.getType())
                .discountValueSnapshot(discountDefinition.getAmountValue())
                .reason(isBlank(remarks) ? discountDefinition.getName() : remarks.trim())
                .createdByStaff(staffId != null ? String.valueOf(staffId) : null)
                .schoolId(schoolId)
                .build());

        assignment.setSponsorCoveredAmount(updatedSponsorCoveredAmount);
        assignmentRepository.save(assignment);
        return FeeDiscountApplyResponse.builder()
                .appliedAmount(appliedDiscount)
                .capped(capped)
                .message(capped ? "Discount capped to remaining principal." : "Discount applied successfully.")
                .build();
    }

    private ResourceNotFoundException resolveAssignmentNotFound(Long assignmentId) {
        if (assignmentRepository.existsAnyById(assignmentId)) {
            throw new BusinessException("Assignment belongs to another tenant.");
        }
        return new ResourceNotFoundException("Student fee assignment not found: " + assignmentId);
    }

    private ResourceNotFoundException resolveDiscountNotFound(Long discountDefinitionId) {
        if (discountDefinitionRepository.existsAnyById(discountDefinitionId)) {
            throw new BusinessException("Discount definition belongs to another tenant.");
        }
        return new ResourceNotFoundException("Discount definition not found: " + discountDefinitionId);
    }

    private BigDecimal calculateRemainingPrincipal(StudentFeeAssignment assignment) {
        return nz(assignment.getAmount())
                .subtract(nz(assignment.getTotalDiscountAmount()))
                .subtract(nz(assignment.getSponsorCoveredAmount()))
                .subtract(nz(assignment.getPrincipalPaid()));
    }

    private BigDecimal calculateDiscountAmount(DiscountDefinition definition, BigDecimal assignmentAmount) {
        BigDecimal raw;
        if (definition.getType() == DiscountType.PERCENTAGE) {
            raw = nz(assignmentAmount).multiply(nz(definition.getAmountValue())).divide(HUNDRED, 6, RoundingMode.HALF_UP);
        } else if (definition.getType() == DiscountType.FLAT) {
            raw = nz(definition.getAmountValue());
        } else {
            throw new BusinessException("Unsupported discount type: " + definition.getType());
        }
        return raw.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : ZERO;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
