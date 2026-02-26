package com.school.backend.finance;

import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.common.enums.ExpensePaymentMode;
import com.school.backend.common.enums.FeeFrequency;
import com.school.backend.common.enums.Gender;
import com.school.backend.core.guardian.dto.GuardianCreateRequest;
import com.school.backend.core.student.dto.StudentCreateRequest;
import com.school.backend.core.student.dto.StudentDto;
import com.school.backend.expense.dto.ExpenseHeadCreateRequest;
import com.school.backend.expense.dto.ExpenseHeadDto;
import com.school.backend.expense.dto.ExpenseVoucherCreateRequest;
import com.school.backend.fee.dto.FeePaymentRequest;
import com.school.backend.fee.dto.FeeStructureCreateRequest;
import com.school.backend.fee.dto.FeeStructureDto;
import com.school.backend.fee.dto.StudentFeeAssignRequest;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.finance.dto.DayClosingDto;
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

        ExpenseHeadCreateRequest headReq = new ExpenseHeadCreateRequest();
        headReq.setName("Day Close Expense");
        ResponseEntity<ExpenseHeadDto> headResp = restTemplate.exchange(
                "/api/expenses/heads",
                HttpMethod.POST,
                new HttpEntity<>(headReq, headers),
                ExpenseHeadDto.class);
        Long headId = Objects.requireNonNull(headResp.getBody()).getId();

        ExpenseVoucherCreateRequest expenseReq = new ExpenseVoucherCreateRequest();
        expenseReq.setExpenseDate(date);
        expenseReq.setExpenseHeadId(headId);
        expenseReq.setAmount(new BigDecimal("50.00"));
        expenseReq.setPaymentMode(ExpensePaymentMode.CASH);
        expenseReq.setDescription("before close");
        ResponseEntity<Object> expenseResp = restTemplate.exchange(
                "/api/expenses",
                HttpMethod.POST,
                new HttpEntity<>(expenseReq, headers),
                Object.class);
        Assertions.assertThat(expenseResp.getStatusCode()).isEqualTo(HttpStatus.OK);

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

        ResponseEntity<DayClosingDto> closeResp = restTemplate.exchange(
                "/api/finance/day-closing?date=" + date,
                HttpMethod.POST,
                new HttpEntity<>(headers),
                DayClosingDto.class);
        Assertions.assertThat(closeResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        DayClosingDto closing = Objects.requireNonNull(closeResp.getBody());

        BigDecimal expectedClosingCash = nz(closing.getOpeningCash())
                .add(nz(closing.getCashRevenue()))
                .subtract(nz(closing.getCashExpense()))
                .subtract(nz(closing.getTransferOut()));
        BigDecimal expectedClosingBank = nz(closing.getOpeningBank())
                .add(nz(closing.getBankRevenue()))
                .subtract(nz(closing.getBankExpense()))
                .add(nz(closing.getTransferIn()));

        Assertions.assertThat(closing.getCashRevenue()).isEqualByComparingTo(new BigDecimal("300.00"));
        Assertions.assertThat(closing.getBankRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        Assertions.assertThat(closing.getCashExpense()).isEqualByComparingTo(new BigDecimal("50.00"));
        Assertions.assertThat(closing.getBankExpense()).isEqualByComparingTo(BigDecimal.ZERO);
        Assertions.assertThat(closing.getTransferOut()).isEqualByComparingTo(new BigDecimal("100.00"));
        Assertions.assertThat(closing.getTransferIn()).isEqualByComparingTo(new BigDecimal("100.00"));
        Assertions.assertThat(closing.getClosingCash()).isEqualByComparingTo(expectedClosingCash);
        Assertions.assertThat(closing.getClosingBank()).isEqualByComparingTo(expectedClosingBank);
        Assertions.assertThat(closing.getClosingCash()).isEqualByComparingTo(new BigDecimal("150.00"));
        Assertions.assertThat(closing.getClosingBank()).isEqualByComparingTo(new BigDecimal("100.00"));

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

        ExpenseVoucherCreateRequest blockedExpenseReq = new ExpenseVoucherCreateRequest();
        blockedExpenseReq.setExpenseDate(date);
        blockedExpenseReq.setExpenseHeadId(headId);
        blockedExpenseReq.setAmount(new BigDecimal("1.00"));
        blockedExpenseReq.setPaymentMode(ExpensePaymentMode.CASH);
        ResponseEntity<String> blockedExpenseResp = restTemplate.exchange(
                "/api/expenses",
                HttpMethod.POST,
                new HttpEntity<>(blockedExpenseReq, headers),
                String.class);
        Assertions.assertThat(blockedExpenseResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(Objects.requireNonNull(blockedExpenseResp.getBody())).contains("Date already closed");

        FinanceAccountTransferRequest blockedTransferReq = new FinanceAccountTransferRequest();
        blockedTransferReq.setTransferDate(date);
        blockedTransferReq.setAmount(new BigDecimal("1.00"));
        ResponseEntity<String> blockedTransferResp = restTemplate.exchange(
                "/api/finance/transfers",
                HttpMethod.POST,
                new HttpEntity<>(blockedTransferReq, headers),
                String.class);
        Assertions.assertThat(blockedTransferResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(Objects.requireNonNull(blockedTransferResp.getBody())).contains("Date already closed");

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

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    @AfterEach
    void cleanup() {
        fullCleanup();
    }
}
