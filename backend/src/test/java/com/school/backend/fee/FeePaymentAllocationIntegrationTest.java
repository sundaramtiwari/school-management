package com.school.backend.fee;

import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.common.enums.FeeFrequency;
import com.school.backend.common.enums.Gender;
import com.school.backend.core.guardian.dto.GuardianCreateRequest;
import com.school.backend.core.student.dto.StudentCreateRequest;
import com.school.backend.core.student.dto.StudentDto;
import com.school.backend.fee.dto.*;
import com.school.backend.fee.entity.FeePaymentAllocation;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.fee.repository.FeePaymentAllocationRepository;
import com.school.backend.common.enums.LateFeeCapType;
import com.school.backend.common.enums.LateFeeType;
import com.school.backend.school.entity.School;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FeePaymentAllocationIntegrationTest extends BaseAuthenticatedIntegrationTest {

    @Autowired
    private FeePaymentAllocationRepository feePaymentAllocationRepository;

    private Long schoolId;
    private Long classId;
    private Long studentId;
    private Long feeTypeId;
    private Long feeStructureId;
    private Long sessionId;

    @Test
    void payment_should_allocate_to_late_fee_first_then_principal() {
        // 1. Setup Environment
        setupBaseData();

        // 2. Assign Fee (Amount: 1000)
        StudentFeeAssignRequest assignReq = new StudentFeeAssignRequest();
        assignReq.setStudentId(studentId);
        assignReq.setFeeStructureId(feeStructureId);
        assignReq.setSessionId(sessionId);
        assignReq.setDueDate(LocalDate.now().minusDays(10)); // Overdue

        ResponseEntity<StudentFeeAssignmentDto> assignResp = restTemplate.exchange(
                "/api/fees/assignments", HttpMethod.POST, new HttpEntity<>(assignReq, headers),
                StudentFeeAssignmentDto.class);
        Assertions.assertThat(assignResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 3. Verify late fee is not pre-accrued before payment-time processing
        String summaryUrl = "/api/fees/summary/students/" + studentId + "?sessionId=" + sessionId;
        ResponseEntity<FeeSummaryDto> summaryResp = restTemplate.exchange(
                summaryUrl, HttpMethod.GET, new HttpEntity<>(headers), FeeSummaryDto.class);

        BigDecimal lateFee = Objects.requireNonNull(summaryResp.getBody()).getTotalLateFeeAccrued();
        Assertions.assertThat(lateFee).isEqualByComparingTo(BigDecimal.ZERO);

        // 4. Make Partial Payment (Amount: 100)
        // Should pay 50 (Late Fee) and 50 (Principal)
        FeePaymentRequest payReq = new FeePaymentRequest();
        payReq.setStudentId(studentId);
        payReq.setSessionId(sessionId);
        payReq.setAmountPaid(new BigDecimal("100.00"));
        payReq.setMode("CASH");
        payReq.setPaymentDate(LocalDate.now());

        ResponseEntity<FeePaymentDto> payResp = restTemplate.exchange(
                "/api/fees/payments", HttpMethod.POST, new HttpEntity<>(payReq, headers), FeePaymentDto.class);
        Assertions.assertThat(payResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        FeePaymentDto payment = Objects.requireNonNull(payResp.getBody());
        Assertions.assertThat(payment.getLateFeePaid()).isEqualByComparingTo(new BigDecimal("50.00"));
        Assertions.assertThat(payment.getPrincipalPaid()).isEqualByComparingTo(new BigDecimal("50.00"));

        List<FeePaymentAllocation> allocations = feePaymentAllocationRepository.findAll().stream()
                .filter(a -> Objects.equals(a.getFeePaymentId(), payment.getId()))
                .toList();
        Assertions.assertThat(allocations).hasSize(1);
        Assertions.assertThat(allocations.get(0).getPrincipalAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        Assertions.assertThat(allocations.get(0).getLateFeeAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        Assertions.assertThat(allocations.get(0).getFeeType().getId()).isEqualTo(feeTypeId);

        BigDecimal totalPrincipalAllocated = allocations.stream()
                .map(FeePaymentAllocation::getPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalLateFeeAllocated = allocations.stream()
                .map(FeePaymentAllocation::getLateFeeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Assertions.assertThat(totalPrincipalAllocated).isEqualByComparingTo(payment.getPrincipalPaid());
        Assertions.assertThat(totalLateFeeAllocated).isEqualByComparingTo(payment.getLateFeePaid());

        // 5. Verify Final Dues
        // Principal: 1000 - 50 = 950
        // Late Fee: 50 - 50 = 0
        summaryResp = restTemplate.exchange(summaryUrl, HttpMethod.GET, new HttpEntity<>(headers), FeeSummaryDto.class);
        FeeSummaryDto summary = Objects.requireNonNull(summaryResp.getBody());
        Assertions.assertThat(summary.getPendingFee()).isEqualByComparingTo(new BigDecimal("950.00"));
        Assertions.assertThat(summary.getTotalLateFeeAccrued().subtract(summary.getTotalLateFeePaid()))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void head_summary_should_return_fee_type_totals_for_date() {
        setupBaseData();

        StudentFeeAssignRequest assignReq = new StudentFeeAssignRequest();
        assignReq.setStudentId(studentId);
        assignReq.setFeeStructureId(feeStructureId);
        assignReq.setSessionId(sessionId);
        assignReq.setDueDate(LocalDate.now().minusDays(7));

        ResponseEntity<StudentFeeAssignmentDto> assignResp = restTemplate.exchange(
                "/api/fees/assignments", HttpMethod.POST, new HttpEntity<>(assignReq, headers),
                StudentFeeAssignmentDto.class);
        Assertions.assertThat(assignResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        FeePaymentRequest payReq = new FeePaymentRequest();
        payReq.setStudentId(studentId);
        payReq.setSessionId(sessionId);
        payReq.setAmountPaid(new BigDecimal("120.00"));
        payReq.setMode("CASH");
        payReq.setPaymentDate(LocalDate.now());

        ResponseEntity<FeePaymentDto> payResp = restTemplate.exchange(
                "/api/fees/payments", HttpMethod.POST, new HttpEntity<>(payReq, headers), FeePaymentDto.class);
        Assertions.assertThat(payResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<FeeTypeHeadSummaryDto[]> summaryResp = restTemplate.exchange(
                "/api/fees/payments/head-summary?date=" + LocalDate.now(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                FeeTypeHeadSummaryDto[].class);

        Assertions.assertThat(summaryResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        FeeTypeHeadSummaryDto[] rows = Objects.requireNonNull(summaryResp.getBody());
        Assertions.assertThat(rows).hasSize(1);
        Assertions.assertThat(rows[0].getFeeTypeId()).isEqualTo(feeTypeId);
        Assertions.assertThat(rows[0].getFeeTypeName()).isEqualTo("GENERIC");
        Assertions.assertThat(rows[0].getTotalPrincipal()).isEqualByComparingTo(new BigDecimal("70.00"));
        Assertions.assertThat(rows[0].getTotalLateFee()).isEqualByComparingTo(new BigDecimal("50.00"));
        Assertions.assertThat(rows[0].getTotalCollected()).isEqualByComparingTo(new BigDecimal("120.00"));
    }

    private void setupBaseData() {
        // Create School
        Map<String, Object> schoolReq = Map.of("name", "Allocation Test School", "displayName", "ATS", "board", "CBSE",
                "schoolCode", "ATS-001");
        ResponseEntity<School> schoolResp = restTemplate.exchange("/api/schools", HttpMethod.POST,
                new HttpEntity<>(schoolReq, headers), School.class);
        schoolId = Objects.requireNonNull(schoolResp.getBody()).getId();
        loginAsSchoolAdmin(schoolId);

        // Session
        sessionId = setupSession(schoolId, sessionRepository, schoolRepository);
        setSessionHeader(sessionId);

        // Class
        Map<String, Object> classReq = Map.of("name", "10", "sessionId", sessionId, "schoolId", schoolId);
        ResponseEntity<Map> classResp = restTemplate.exchange("/api/classes", HttpMethod.POST,
                new HttpEntity<>(classReq, headers), Map.class);
        classId = Long.valueOf(Objects.requireNonNull(classResp.getBody()).get("id").toString());

        // Student
        StudentCreateRequest sreq = new StudentCreateRequest();
        sreq.setAdmissionNumber("ADM-ALLOC-1");
        sreq.setFirstName("Alloc");
        sreq.setGender(Gender.MALE);
        sreq.setGuardians(List.of(GuardianCreateRequest.builder().name("G").contactNumber("1").relation("F")
                .primaryGuardian(true).build()));
        studentId = Objects.requireNonNull(restTemplate
                .exchange("/api/students", HttpMethod.POST, new HttpEntity<>(sreq, headers), StudentDto.class)
                .getBody()).getId();

        // Fee Type
        FeeType typeReq = new FeeType();
        typeReq.setName("GENERIC");
        feeTypeId = Objects.requireNonNull(restTemplate
                .exchange("/api/fees/types", HttpMethod.POST, new HttpEntity<>(typeReq, headers), FeeType.class)
                .getBody()).getId();

        // Fee Structure (1000) WITH Late Fee Policy (Flat 50)
        FeeStructureCreateRequest fsReq = new FeeStructureCreateRequest();
        fsReq.setClassId(classId);
        fsReq.setSessionId(sessionId);
        fsReq.setFeeTypeId(feeTypeId);
        fsReq.setAmount(new BigDecimal("1000.00"));
        fsReq.setFrequency(FeeFrequency.ONE_TIME);

        // Late Fee Policy fields
        fsReq.setLateFeeType(LateFeeType.FLAT);
        fsReq.setLateFeeAmountValue(new BigDecimal("50.00"));
        fsReq.setLateFeeGraceDays(0);
        fsReq.setLateFeeCapType(LateFeeCapType.NONE);
        fsReq.setLateFeeCapValue(BigDecimal.ZERO);

        feeStructureId = Objects.requireNonNull(restTemplate.exchange("/api/fees/structures", HttpMethod.POST,
                new HttpEntity<>(fsReq, headers), FeeStructureDto.class).getBody()).getId();
    }

    @AfterEach
    void cleanup() {
        fullCleanup();
    }
}
