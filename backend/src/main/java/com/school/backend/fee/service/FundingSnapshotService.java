package com.school.backend.fee.service;

import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import com.school.backend.fee.repository.StudentFundingArrangementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class FundingSnapshotService {

    private final StudentFundingArrangementRepository fundingRepository;
    private final StudentFeeAssignmentRepository assignmentRepository;
    private final FeeCalculationService feeCalculationService;

    @Transactional
    public void recalculateAndUpdateFundingSnapshot(StudentFeeAssignment assignment) {
        BigDecimal updatedSponsorCoveredAmount = fundingRepository
                .findByStudentIdAndSessionIdAndSchoolIdAndActiveTrue(
                        assignment.getStudentId(),
                        assignment.getSessionId(),
                        assignment.getSchoolId())
                .map(funding -> feeCalculationService.calculateFundingSnapshot(
                        assignment.getAmount(),
                        nz(assignment.getTotalDiscountAmount()),
                        funding))
                .orElse(nz(assignment.getSponsorCoveredAmount()));

        assignment.setSponsorCoveredAmount(updatedSponsorCoveredAmount);
        assignmentRepository.save(assignment);
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
