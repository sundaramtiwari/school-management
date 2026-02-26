package com.school.backend.fee.service;

import com.school.backend.fee.entity.StudentFeeAssignment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FeeMathTest {

    @Test
    @DisplayName("computePending should subtract discount only")
    void computePending_discountOnly() {
        StudentFeeAssignment assignment = StudentFeeAssignment.builder()
                .amount(new BigDecimal("1000.00"))
                .totalDiscountAmount(new BigDecimal("250.00"))
                .build();

        assertEquals(new BigDecimal("750.00"), FeeMath.computePending(assignment));
    }

    @Test
    @DisplayName("computePending should subtract funding only")
    void computePending_fundingOnly() {
        StudentFeeAssignment assignment = StudentFeeAssignment.builder()
                .amount(new BigDecimal("1000.00"))
                .sponsorCoveredAmount(new BigDecimal("400.00"))
                .build();

        assertEquals(new BigDecimal("600.00"), FeeMath.computePending(assignment));
    }

    @Test
    @DisplayName("computePending should subtract waived late fee")
    void computePending_waivedLateFee() {
        StudentFeeAssignment assignment = StudentFeeAssignment.builder()
                .amount(new BigDecimal("1000.00"))
                .lateFeeAccrued(new BigDecimal("100.00"))
                .lateFeeWaived(new BigDecimal("30.00"))
                .build();

        assertEquals(new BigDecimal("1070.00"), FeeMath.computePending(assignment));
    }

    @Test
    @DisplayName("computePending should subtract partial principal payments")
    void computePending_partialPrincipal() {
        StudentFeeAssignment assignment = StudentFeeAssignment.builder()
                .amount(new BigDecimal("1000.00"))
                .principalPaid(new BigDecimal("325.00"))
                .build();

        assertEquals(new BigDecimal("675.00"), FeeMath.computePending(assignment));
    }
}
