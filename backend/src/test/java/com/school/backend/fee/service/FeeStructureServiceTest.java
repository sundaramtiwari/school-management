package com.school.backend.fee.service;

import com.school.backend.common.enums.FeeFrequency;
import com.school.backend.core.student.entity.StudentEnrollment;
import com.school.backend.core.student.repository.StudentEnrollmentRepository;
import com.school.backend.fee.entity.FeeStructure;
import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.fee.repository.LateFeePolicyRepository;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.repository.AcademicSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeeStructureServiceTest {

    @Mock
    private StudentEnrollmentRepository enrollmentRepository;
    @Mock
    private StudentFeeAssignmentRepository assignmentRepository;
    @Mock
    private AcademicSessionRepository academicSessionRepository;
    @Mock
    private LateFeePolicyRepository lateFeePolicyRepository;
    @Mock
    private FeeCalculationService feeCalculationService;

    @InjectMocks
    private FeeStructureService feeStructureService;

    private AcademicSession session;
    private FeeStructure monthlyFee;
    private FeeStructure annualFee;

    @BeforeEach
    void setUp() {
        session = AcademicSession.builder()
                .id(1L)
                .schoolId(1L)
                .startDate(LocalDate.of(2024, 4, 1))
                .endDate(LocalDate.of(2025, 3, 31))
                .build();

        monthlyFee = FeeStructure.builder()
                .id(10L)
                .schoolId(1L)
                .sessionId(1L)
                .amount(new BigDecimal("1000.00"))
                .frequency(FeeFrequency.MONTHLY)
                .build();

        annualFee = FeeStructure.builder()
                .id(11L)
                .schoolId(1L)
                .sessionId(1L)
                .amount(new BigDecimal("5000.00"))
                .frequency(FeeFrequency.ANNUALLY)
                .build();
    }

    @Test
    @DisplayName("Full Session: Enrollment at start should charge 12 months for Monthly fee")
    void testFullSessionMonthly() {
        setupMocks(LocalDate.of(2024, 4, 1));
        when(feeCalculationService.calculateAssignableAmount(monthlyFee, 100L)).thenReturn(new BigDecimal("12000.00"));

        feeStructureService.assignFeeToStudent(monthlyFee, 100L);

        ArgumentCaptor<StudentFeeAssignment> captor = ArgumentCaptor.forClass(StudentFeeAssignment.class);
        verify(assignmentRepository).save(captor.capture());

        // 1000 * 12 months = 12000
        assertEquals(new BigDecimal("12000.00"), captor.getValue().getAmount());
    }

    @Test
    @DisplayName("Mid-Session: Enrollment 6 months in should charge remaining 7 months (Oct to Mar)")
    void testMidSessionMonthly() {
        // Enrolling on Oct 15th
        setupMocks(LocalDate.of(2024, 10, 15));
        when(feeCalculationService.calculateAssignableAmount(monthlyFee, 100L)).thenReturn(new BigDecimal("6000.00"));

        feeStructureService.assignFeeToStudent(monthlyFee, 100L);

        ArgumentCaptor<StudentFeeAssignment> captor = ArgumentCaptor.forClass(StudentFeeAssignment.class);
        verify(assignmentRepository).save(captor.capture());

        // Oct, Nov, Dec, Jan, Feb, Mar = 6 months.
        // Logic: between(2024-10-01, 2025-04-01) = 6 months
        assertEquals(new BigDecimal("6000.00"), captor.getValue().getAmount());
    }

    @Test
    @DisplayName("End-Session: Enrollment in last month should charge 1 month")
    void testEndSessionMonthly() {
        setupMocks(LocalDate.of(2025, 3, 15));
        when(feeCalculationService.calculateAssignableAmount(monthlyFee, 100L)).thenReturn(new BigDecimal("1000.00"));

        feeStructureService.assignFeeToStudent(monthlyFee, 100L);

        ArgumentCaptor<StudentFeeAssignment> captor = ArgumentCaptor.forClass(StudentFeeAssignment.class);
        verify(assignmentRepository).save(captor.capture());

        assertEquals(new BigDecimal("1000.00"), captor.getValue().getAmount());
    }

    @Test
    @DisplayName("Annual Fixed: Enrollment mid-session should still charge 100% for Annual fee")
    void testMidSessionAnnual() {
        setupMocks(LocalDate.of(2024, 10, 1));
        when(feeCalculationService.calculateAssignableAmount(annualFee, 100L)).thenReturn(new BigDecimal("5000.00"));

        feeStructureService.assignFeeToStudent(annualFee, 100L);

        ArgumentCaptor<StudentFeeAssignment> captor = ArgumentCaptor.forClass(StudentFeeAssignment.class);
        verify(assignmentRepository).save(captor.capture());

        assertEquals(new BigDecimal("5000.00"), captor.getValue().getAmount());
    }

    private void setupMocks(LocalDate enrollmentDate) {
        lenient().when(academicSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        StudentEnrollment enrollment = StudentEnrollment.builder()
                .studentId(100L)
                .sessionId(1L)
                .enrollmentDate(enrollmentDate)
                .active(true)
                .build();

        lenient().when(enrollmentRepository.findFirstByStudentIdAndSessionIdAndActiveTrue(100L, 1L))
                .thenReturn(Optional.of(enrollment));

        lenient().when(lateFeePolicyRepository.findByFeeStructureId(any())).thenReturn(Optional.empty());
    }
}
