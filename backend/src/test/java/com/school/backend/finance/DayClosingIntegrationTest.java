package com.school.backend.finance;

import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.common.enums.FeeFrequency;
import com.school.backend.common.enums.Gender;
import com.school.backend.core.guardian.dto.GuardianCreateRequest;
import com.school.backend.core.student.dto.StudentCreateRequest;
import com.school.backend.core.student.dto.StudentDto;
import com.school.backend.fee.dto.FeePaymentRequest;
import com.school.backend.fee.dto.FeeStructureCreateRequest;
import com.school.backend.fee.dto.FeeStructureDto;
import com.school.backend.fee.dto.StudentFeeAssignRequest;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.finance.dto.FinanceAccountTransferRequest;
import com.school.backend.school.entity.School;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DayClosingIntegrationTest extends BaseAuthenticatedIntegrationTest {

    @Test
    void day_closing_should_lock_date_until_override_enabled() {
        ResponseEntity<School> schoolResp = restTemplate.exchange(
                "/api/schools",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "name", "Day Close School",
                        "displayName", "DCS",
                        "board", "CBSE",
                        "schoolCode", "DCS-001"), headers),
                School.class);
        Long schoolId = Objects.requireNonNull(schoolResp.getBody()).getId();
        loginAsSchoolAdmin(schoolId);

        Long sessionId = setupSession(schoolId, sessionRepository, schoolRepository);
        setSessionHeader(sessionId);

        ResponseEntity<Map> classResp = restTemplate.exchange(
                "/api/classes",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "6", "sessionId", sessionId, "schoolId", schoolId), headers),
                Map.class);
        Long classId = Long.valueOf(Objects.requireNonNull(classResp.getBody()).get("id").toString());

        StudentCreateRequest sreq = new StudentCreateRequest();
        sreq.setAdmissionNumber("ADM-DC-1");
        sreq.setFirstName("Day");
        sreq.setGender(Gender.MALE);
        sreq.setGuardians(List.of(GuardianCreateRequest.builder()
                .name("Guardian")
                .contactNumber("8888888888")
                .relation("FATHER")
                .primaryGuardian(true)
                .build()));
        Long studentId = Objects.requireNonNull(restTemplate
                .exchange("/api/students", HttpMethod.POST, new HttpEntity<>(sreq, headers), StudentDto.class)
                .getBody()).getId();

        FeeType type = new FeeType();
        type.setName("DayCloseTuition");
        Long feeTypeId = Objects.requireNonNull(restTemplate
                .exchange("/api/fees/types", HttpMethod.POST, new HttpEntity<>(type, headers), FeeType.class)
                .getBody()).getId();

        FeeStructureCreateRequest fsReq = new FeeStructureCreateRequest();
        fsReq.setClassId(classId);
        fsReq.setSessionId(sessionId);
        fsReq.setFeeTypeId(feeTypeId);
        fsReq.setAmount(new BigDecimal("1000.00"));
        fsReq.setFrequency(FeeFrequency.ONE_TIME);
        Long feeStructureId = Objects.requireNonNull(restTemplate.exchange(
                "/api/fees/structures",
                HttpMethod.POST,
                new HttpEntity<>(fsReq, headers),
                FeeStructureDto.class).getBody()).getId();

        StudentFeeAssignRequest assignReq = new StudentFeeAssignRequest();
        assignReq.setStudentId(studentId);
        assignReq.setFeeStructureId(feeStructureId);
        assignReq.setSessionId(sessionId);
        assignReq.setDueDate(LocalDate.of(2026, 2, 10));
        restTemplate.exchange("/api/fees/assignments", HttpMethod.POST, new HttpEntity<>(assignReq, headers), Object.class);

        LocalDate date = LocalDate.of(2026, 2, 20);

        FeePaymentRequest pay1 = new FeePaymentRequest();
        pay1.setStudentId(studentId);
        pay1.setSessionId(sessionId);
        pay1.setPaymentDate(date);
        pay1.setMode("CASH");
        pay1.setAllocations(List.of(
                com.school.backend.fee.dto.FeePaymentAllocationRequest.builder()
                        .assignmentId(findAssignmentId(studentId, sessionId))
                        .principalAmount(new BigDecimal("300.00"))
                        .build()));
        ResponseEntity<Object> pay1Resp = restTemplate.exchange(
                "/api/fees/payments",
                HttpMethod.POST,
                new HttpEntity<>(pay1, headers),
                Object.class);
        Assertions.assertThat(pay1Resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        FinanceAccountTransferRequest transferReq = new FinanceAccountTransferRequest();
        transferReq.setTransferDate(date);
        transferReq.setAmount(new BigDecimal("100.00"));
        transferReq.setReferenceNumber("DC-TRF-1");
        transferReq.setRemarks("before close");
        ResponseEntity<Object> transferResp = restTemplate.exchange(
                "/api/finance/transfers",
                HttpMethod.POST,
                new HttpEntity<>(transferReq, headers),
                Object.class);
        Assertions.assertThat(transferResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Object> closeResp = restTemplate.exchange(
                "/api/finance/day-closing?date=" + date,
                HttpMethod.POST,
                new HttpEntity<>(headers),
                Object.class);
        Assertions.assertThat(closeResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        FeePaymentRequest pay2 = new FeePaymentRequest();
        pay2.setStudentId(studentId);
        pay2.setSessionId(sessionId);
        pay2.setPaymentDate(date);
        pay2.setMode("CASH");
        pay2.setAllocations(List.of(
                com.school.backend.fee.dto.FeePaymentAllocationRequest.builder()
                        .assignmentId(findAssignmentId(studentId, sessionId))
                        .principalAmount(new BigDecimal("100.00"))
                        .build()));
        ResponseEntity<String> blockedResp = restTemplate.exchange(
                "/api/fees/payments",
                HttpMethod.POST,
                new HttpEntity<>(pay2, headers),
                String.class);
        Assertions.assertThat(blockedResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(Objects.requireNonNull(blockedResp.getBody())).contains("Date already closed");

        loginAsSuperAdmin();
        headers.set("X-School-Id", String.valueOf(schoolId));
        setSessionHeader(sessionId);
        ResponseEntity<Object> overrideResp = restTemplate.exchange(
                "/api/finance/day-closing/" + date + "/override",
                HttpMethod.PATCH,
                new HttpEntity<>(headers),
                Object.class);
        Assertions.assertThat(overrideResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        loginAsSchoolAdmin(schoolId);
        setSessionHeader(sessionId);
        ResponseEntity<Object> pay2RetryResp = restTemplate.exchange(
                "/api/fees/payments",
                HttpMethod.POST,
                new HttpEntity<>(pay2, headers),
                Object.class);
        Assertions.assertThat(pay2RetryResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private Long findAssignmentId(Long studentId, Long sessionId) {
        return assignmentRepository.findByStudentIdAndSessionId(studentId, sessionId).stream()
                .findFirst()
                .orElseThrow()
                .getId();
    }

    @AfterEach
    void cleanup() {
        fullCleanup();
    }
}
