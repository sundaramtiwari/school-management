package com.school.backend.fee.service;

import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.common.enums.LateFeeCapType;
import com.school.backend.common.enums.LateFeeType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class LateFeeCalculator {

    public BigDecimal calculateLateFee(
            StudentFeeAssignment assignment,
            BigDecimal unpaidAmount,
            LocalDate paymentDate) {
        LateFeeType type = assignment.getLateFeeType() != null ? assignment.getLateFeeType() : LateFeeType.NONE;
        if (type == LateFeeType.NONE) {
            return BigDecimal.ZERO;
        }

        if (assignment.getDueDate() == null || paymentDate == null) {
            return BigDecimal.ZERO;
        }

        int graceDays = assignment.getLateFeeGraceDays() != null ? Math.max(assignment.getLateFeeGraceDays(), 0) : 0;
        LocalDate lateStartDate = assignment.getDueDate().plusDays(graceDays);
        if (!paymentDate.isAfter(lateStartDate)) {
            return BigDecimal.ZERO;
        }

        BigDecimal principalUnpaid = nz(unpaidAmount);
        if (principalUnpaid.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal value = nz(assignment.getLateFeeValue());
        BigDecimal currentAccrued = nz(assignment.getLateFeeAccrued());
        BigDecimal computedIncrement;

        switch (type) {
            case FLAT -> {
                if (assignment.isLateFeeApplied()) {
                    return BigDecimal.ZERO;
                }
                computedIncrement = value;
            }
            case PERCENTAGE -> {
                if (assignment.isLateFeeApplied()) {
                    return BigDecimal.ZERO;
                }
                computedIncrement = principalUnpaid.multiply(value).divide(BigDecimal.valueOf(100), 8,
                        RoundingMode.HALF_UP);
            }
            case DAILY_PERCENTAGE -> {
                long daysLate = ChronoUnit.DAYS.between(lateStartDate, paymentDate);
                if (daysLate <= 0) {
                    return BigDecimal.ZERO;
                }
                BigDecimal totalAccruedAsOfPaymentDate = principalUnpaid
                        .multiply(value)
                        .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(daysLate));

                BigDecimal cappedTotal = applyCap(assignment, principalUnpaid, totalAccruedAsOfPaymentDate);
                computedIncrement = cappedTotal.subtract(currentAccrued);
            }
            default -> computedIncrement = BigDecimal.ZERO;
        }

        if (type == LateFeeType.FLAT || type == LateFeeType.PERCENTAGE) {
            computedIncrement = applyCap(assignment, principalUnpaid, computedIncrement);
        }

        if (computedIncrement.compareTo(BigDecimal.ZERO) < 0) {
            computedIncrement = BigDecimal.ZERO;
        }

        return computedIncrement.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal applyCap(StudentFeeAssignment assignment, BigDecimal unpaidAmount, BigDecimal computed) {
        LateFeeCapType capType = assignment.getLateFeeCapType() != null
                ? assignment.getLateFeeCapType()
                : LateFeeCapType.NONE;
        BigDecimal capValue = nz(assignment.getLateFeeCapValue());

        if (capType == LateFeeCapType.FIXED) {
            return computed.min(capValue);
        }
        if (capType == LateFeeCapType.PERCENTAGE) {
            BigDecimal capAmount = unpaidAmount.multiply(capValue)
                    .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
            return computed.min(capAmount);
        }
        return computed;
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
