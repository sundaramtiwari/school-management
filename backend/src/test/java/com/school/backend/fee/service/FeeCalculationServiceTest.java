package com.school.backend.fee.service;

import com.school.backend.common.enums.FeeFrequency;
import com.school.backend.fee.entity.StudentFundingArrangement;
import com.school.backend.common.enums.FundingCoverageMode;
import com.school.backend.common.enums.FundingCoverageType;
import com.school.backend.fee.entity.FeeStructure;
import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.repository.AcademicSessionRepository;
import com.school.backend.core.student.entity.StudentEnrollment;
import com.school.backend.core.student.repository.StudentEnrollmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeeCalculationServiceTest {

    @Mock
    private AcademicSessionRepository academicSessionRepository;
    @Mock
    private StudentEnrollmentRepository enrollmentRepository;

    @InjectMocks
    private FeeCalculationService feeCalculationService;

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

    @Test
    @DisplayName("Assignable amount: monthly should prorate by months remaining for mid-session enrollment")
    void testCalculateAssignableAmountMonthlyMidSession() {
        FeeStructure feeStructure = FeeStructure.builder()
                .id(10L)
                .schoolId(1L)
                .sessionId(1L)
                .amount(new BigDecimal("1000.00"))
                .frequency(FeeFrequency.MONTHLY)
                .build();

        AcademicSession session = AcademicSession.builder()
                .id(1L)
                .schoolId(1L)
                .startDate(LocalDate.of(2024, 4, 1))
                .endDate(LocalDate.of(2025, 3, 31))
                .build();

        StudentEnrollment enrollment = StudentEnrollment.builder()
                .studentId(100L)
                .sessionId(1L)
                .enrollmentDate(LocalDate.of(2024, 10, 15))
                .active(true)
                .build();

        when(academicSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(enrollmentRepository.findFirstByStudentIdAndSessionIdAndActiveTrue(100L, 1L))
                .thenReturn(Optional.of(enrollment));

        BigDecimal result = feeCalculationService.calculateAssignableAmount(feeStructure, 100L);
        assertEquals(new BigDecimal("6000.00"), result);
    }

    @Test
    @DisplayName("Assignable amount: monthly should charge full periods when session dates are missing")
    void testCalculateAssignableAmountMonthlyFallback() {
        FeeStructure feeStructure = FeeStructure.builder()
                .id(10L)
                .schoolId(1L)
                .sessionId(1L)
                .amount(new BigDecimal("1000.00"))
                .frequency(FeeFrequency.MONTHLY)
                .build();

        when(academicSessionRepository.findById(1L)).thenReturn(Optional.empty());

        BigDecimal result = feeCalculationService.calculateAssignableAmount(feeStructure, 100L);
        assertEquals(new BigDecimal("12000.00"), result);
    }
}
