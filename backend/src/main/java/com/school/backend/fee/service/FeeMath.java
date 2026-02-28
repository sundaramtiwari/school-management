package com.school.backend.fee.service;

import com.school.backend.fee.entity.StudentFeeAssignment;

import java.math.BigDecimal;

public final class FeeMath {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private FeeMath() {
    }

    public static BigDecimal computePending(StudentFeeAssignment assignment) {
        if (assignment == null) {
            return ZERO;
        }

        BigDecimal pending = nz(assignment.getAmount())
                .add(nz(assignment.getLateFeeAccrued()))
                .subtract(nz(assignment.getTotalDiscountAmount()))
                .subtract(nz(assignment.getLateFeeWaived()))
                .subtract(nz(assignment.getPrincipalPaid()))
                .subtract(nz(assignment.getLateFeePaid()));

        return pending.compareTo(ZERO) < 0 ? ZERO : pending;
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : ZERO;
    }
}
