package com.school.backend.fee.service;

import com.school.backend.common.exception.BusinessException;
import com.school.backend.common.tenant.SessionContext;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.fee.dto.FeePaymentAllocationRequest;
import com.school.backend.fee.dto.FeePaymentDto;
import com.school.backend.fee.dto.FeePaymentRequest;
import com.school.backend.fee.entity.FeePayment;
import com.school.backend.fee.entity.FeeStructure;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.fee.repository.FeePaymentAllocationRepository;
import com.school.backend.fee.repository.FeePaymentRepository;
import com.school.backend.fee.repository.FeeStructureRepository;
import com.school.backend.fee.repository.LateFeeLogRepository;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import com.school.backend.finance.repository.DayClosingRepository;
import com.school.backend.core.student.repository.StudentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeePaymentServiceTest {

    @Mock
    private FeePaymentRepository paymentRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private StudentFeeAssignmentRepository assignmentRepository;
    @Mock
    private LateFeeLogRepository lateFeeLogRepository;
    @Mock
    private LateFeeCalculator lateFeeCalculator;
    @Mock
    private FeeStructureRepository feeStructureRepository;
    @Mock
    private FeePaymentAllocationRepository feePaymentAllocationRepository;
    @Mock
    private DayClosingRepository dayClosingRepository;

    @InjectMocks
    private FeePaymentService feePaymentService;

    @BeforeEach
    void setUp() {
        TenantContext.setSchoolId(10L);
        SessionContext.setSessionId(20L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SessionContext.clear();
    }

    @Test
    @DisplayName("pay should reject overpayment using centralized pending formula")
    void pay_shouldRejectOverpayment() {
        FeePaymentRequest request = new FeePaymentRequest();
        request.setStudentId(100L);
        request.setSessionId(20L);
        request.setMode("CASH");
        request.setAllocations(List.of(FeePaymentAllocationRequest.builder()
                .assignmentId(500L)
                .principalAmount(new BigDecimal("701.00"))
                .build()));

        StudentFeeAssignment assignment = StudentFeeAssignment.builder()
                .id(500L)
                .studentId(100L)
                .schoolId(10L)
                .amount(new BigDecimal("1000.00"))
                .totalDiscountAmount(new BigDecimal("200.00"))
                .sponsorCoveredAmount(new BigDecimal("100.00"))
                .build();

        when(studentRepository.existsById(100L)).thenReturn(true);
        when(dayClosingRepository.existsBySchoolIdAndDateAndOverrideAllowedFalse(org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.any())).thenReturn(false);
        when(assignmentRepository.findByIdWithLock(500L)).thenReturn(Optional.of(assignment));
        when(lateFeeCalculator.calculateLateFee(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(BigDecimal.ZERO);

        assertThrows(BusinessException.class, () -> feePaymentService.pay(request));

        verify(paymentRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(assignmentRepository, never()).saveAll(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("pay should accept late-fee-only allocation when principal is zero")
    void pay_shouldAcceptLateFeeOnlyAllocation() {
        FeePaymentRequest request = new FeePaymentRequest();
        request.setStudentId(100L);
        request.setSessionId(20L);
        request.setMode("CASH");
        request.setPaymentDate(LocalDate.of(2026, 2, 26));
        request.setAllocations(List.of(FeePaymentAllocationRequest.builder()
                .assignmentId(500L)
                .principalAmount(BigDecimal.ZERO)
                .lateFeeAmount(new BigDecimal("50.00"))
                .build()));

        StudentFeeAssignment assignment = StudentFeeAssignment.builder()
                .id(500L)
                .studentId(100L)
                .schoolId(10L)
                .feeStructureId(700L)
                .amount(new BigDecimal("1000.00"))
                .principalPaid(new BigDecimal("1000.00"))
                .lateFeeAccrued(new BigDecimal("50.00"))
                .lateFeePaid(BigDecimal.ZERO)
                .lateFeeWaived(BigDecimal.ZERO)
                .totalDiscountAmount(BigDecimal.ZERO)
                .sponsorCoveredAmount(BigDecimal.ZERO)
                .build();

        FeeType feeType = FeeType.builder()
                .id(900L)
                .schoolId(10L)
                .name("TUITION")
                .build();
        FeeStructure feeStructure = FeeStructure.builder()
                .id(700L)
                .schoolId(10L)
                .sessionId(20L)
                .feeType(feeType)
                .amount(new BigDecimal("1000.00"))
                .build();

        when(studentRepository.existsById(100L)).thenReturn(true);
        when(dayClosingRepository.existsBySchoolIdAndDateAndOverrideAllowedFalse(org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.any())).thenReturn(false);
        when(assignmentRepository.findByIdWithLock(500L)).thenReturn(Optional.of(assignment));
        when(lateFeeCalculator.calculateLateFee(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(assignmentRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any())).thenAnswer(invocation -> {
            FeePayment payment = invocation.getArgument(0);
            payment.setId(111L);
            return payment;
        });
        when(feeStructureRepository.findByIdInAndSchoolId(List.of(700L), 10L)).thenReturn(List.of(feeStructure));

        FeePaymentDto result = feePaymentService.pay(request);

        assertNotNull(result);
        assertEquals(new BigDecimal("0"), result.getPrincipalPaid());
        assertEquals(new BigDecimal("50.00"), result.getLateFeePaid());
        verify(paymentRepository).save(any());
        verify(feePaymentAllocationRepository).saveAll(any());
    }
}
