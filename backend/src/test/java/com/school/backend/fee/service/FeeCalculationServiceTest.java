package com.school.backend.fee.service;

import com.school.backend.fee.entity.StudentFundingArrangement;
import com.school.backend.fee.enums.FundingCoverageMode;
import com.school.backend.fee.enums.FundingCoverageType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FeeCalculationServiceTest {

    private final FeeCalculationService feeCalculationService = new FeeCalculationService();

    @Test
    @DisplayName("Net Principal calculation should subtract discount and funding and cap at zero")
    void testCalculateNetPrincipal() {
        BigDecimal amount = new BigDecimal("1000.00");
        BigDecimal discount = new BigDecimal("100.00");
        BigDecimal funding = new BigDecimal("200.00");

        BigDecimal result = feeCalculationService.calculateNetPrincipal(amount, discount, funding);
        assertEquals(new BigDecimal("700.00"), result);

        // Test capping at zero
        BigDecimal highDiscount = new BigDecimal("1200.00");
        BigDecimal resultCap = feeCalculationService.calculateNetPrincipal(amount, highDiscount, funding);
        assertEquals(new BigDecimal("0.00"), resultCap);
    }

    @Test
    @DisplayName("Funding snapshot: FULL coverage returns net after discount")
    void testCalculateFundingFull() {
        StudentFundingArrangement funding = new StudentFundingArrangement();
        funding.setActive(true);
        funding.setCoverageType(FundingCoverageType.FULL);

        BigDecimal base = new BigDecimal("1000.00");
        BigDecimal discount = new BigDecimal("150.00");

        BigDecimal result = feeCalculationService.calculateFundingSnapshot(base, discount, funding);
        assertEquals(new BigDecimal("850.00"), result);
    }

    @Test
    @DisplayName("Funding snapshot: PARTIAL FIXED_AMOUNT coverage")
    void testCalculateFundingFixed() {
        StudentFundingArrangement funding = new StudentFundingArrangement();
        funding.setActive(true);
        funding.setCoverageType(FundingCoverageType.PARTIAL);
        funding.setCoverageMode(FundingCoverageMode.FIXED_AMOUNT);
        funding.setCoverageValue(new BigDecimal("300.00"));

        BigDecimal base = new BigDecimal("1000.00");
        BigDecimal discount = new BigDecimal("100.00");

        BigDecimal result = feeCalculationService.calculateFundingSnapshot(base, discount, funding);
        assertEquals(new BigDecimal("300.00"), result);

        // Test capping
        funding.setCoverageValue(new BigDecimal("1200.00"));
        BigDecimal resultCap = feeCalculationService.calculateFundingSnapshot(base, discount, funding);
        assertEquals(new BigDecimal("900.00"), resultCap);
    }

    @Test
    @DisplayName("Funding snapshot: PARTIAL PERCENTAGE coverage")
    void testCalculateFundingPercentage() {
        StudentFundingArrangement funding = new StudentFundingArrangement();
        funding.setActive(true);
        funding.setCoverageType(FundingCoverageType.PARTIAL);
        funding.setCoverageMode(FundingCoverageMode.PERCENTAGE);
        funding.setCoverageValue(new BigDecimal("25.00")); // 25%

        BigDecimal base = new BigDecimal("1000.00");
        BigDecimal discount = new BigDecimal("200.00"); // Net = 800

        BigDecimal result = feeCalculationService.calculateFundingSnapshot(base, discount, funding);
        assertEquals(new BigDecimal("200.00"), result); // 25% of 800 = 200
    }

    @Test
    @DisplayName("Funding snapshot: Inactive funding returns zero")
    void testCalculateFundingInactive() {
        StudentFundingArrangement funding = new StudentFundingArrangement();
        funding.setActive(false);

        BigDecimal base = new BigDecimal("1000.00");
        BigDecimal result = feeCalculationService.calculateFundingSnapshot(base, BigDecimal.ZERO, funding);
        assertEquals(new BigDecimal("0.00"), result);
    }
}
