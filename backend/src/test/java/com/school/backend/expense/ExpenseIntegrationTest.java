package com.school.backend.expense;

import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.common.enums.ExpensePaymentMode;
import com.school.backend.expense.dto.*;
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
import java.util.Map;
import java.util.Objects;

public class ExpenseIntegrationTest extends BaseAuthenticatedIntegrationTest {

    @Test
    void expense_module_should_create_list_and_summarize_in_session_scope() {
        ResponseEntity<School> schoolResp = restTemplate.exchange(
                "/api/schools",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "name", "Expense Test School",
                        "displayName", "ETS",
                        "board", "CBSE",
                        "schoolCode", "ETS-001"), headers),
                School.class);
        Long schoolId = Objects.requireNonNull(schoolResp.getBody()).getId();
        loginAsSchoolAdmin(schoolId);

        Long sessionId = setupSession(schoolId, sessionRepository, schoolRepository);
        setSessionHeader(sessionId);

        ExpenseHeadCreateRequest headReq = new ExpenseHeadCreateRequest();
        headReq.setName("  electricity bill ");
        headReq.setDescription("Power expense");

        ResponseEntity<ExpenseHeadDto> headResp = restTemplate.exchange(
                "/api/expenses/heads",
                HttpMethod.POST,
                new HttpEntity<>(headReq, headers),
                ExpenseHeadDto.class);
        Assertions.assertThat(headResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        ExpenseHeadDto head = Objects.requireNonNull(headResp.getBody());
        Assertions.assertThat(head.getName()).isEqualTo("Electricity Bill");

        ExpenseHeadCreateRequest duplicateReq = new ExpenseHeadCreateRequest();
        duplicateReq.setName("ELECTRICITY BILL");

        ResponseEntity<String> duplicateResp = restTemplate.exchange(
                "/api/expenses/heads",
                HttpMethod.POST,
                new HttpEntity<>(duplicateReq, headers),
                String.class);
        Assertions.assertThat(duplicateResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ExpenseVoucherCreateRequest voucherReq = new ExpenseVoucherCreateRequest();
        voucherReq.setExpenseDate(LocalDate.of(2026, 2, 10));
        voucherReq.setExpenseHeadId(head.getId());
        voucherReq.setAmount(new BigDecimal("2500.00"));
        voucherReq.setPaymentMode(ExpensePaymentMode.UPI);
        voucherReq.setDescription("Feb bill");
        voucherReq.setReferenceNumber("TXN-1001");

        ResponseEntity<ExpenseVoucherDto> voucherResp = restTemplate.exchange(
                "/api/expenses",
                HttpMethod.POST,
                new HttpEntity<>(voucherReq, headers),
                ExpenseVoucherDto.class);
        Assertions.assertThat(voucherResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        ExpenseVoucherDto voucher = Objects.requireNonNull(voucherResp.getBody());
        Assertions.assertThat(voucher.getVoucherNumber()).startsWith("EXP-" + sessionId + "-");
        Assertions.assertThat(voucher.getAmount()).isEqualByComparingTo(new BigDecimal("2500.00"));

        ResponseEntity<ExpenseVoucherDto[]> listByDateResp = restTemplate.exchange(
                "/api/expenses?date=2026-02-10",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ExpenseVoucherDto[].class);
        Assertions.assertThat(listByDateResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(Objects.requireNonNull(listByDateResp.getBody())).hasSize(1);

        ResponseEntity<ExpenseVoucherDto[]> monthlyResp = restTemplate.exchange(
                "/api/expenses/monthly?year=2026&month=2",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ExpenseVoucherDto[].class);
        Assertions.assertThat(monthlyResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(Objects.requireNonNull(monthlyResp.getBody())).hasSize(1);

        ResponseEntity<ExpenseSessionSummaryDto> summaryResp = restTemplate.exchange(
                "/api/expenses/session-summary",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ExpenseSessionSummaryDto.class);
        Assertions.assertThat(summaryResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        ExpenseSessionSummaryDto summary = Objects.requireNonNull(summaryResp.getBody());
        Assertions.assertThat(summary.getSessionId()).isEqualTo(sessionId);
        Assertions.assertThat(summary.getTotalVouchers()).isEqualTo(1);
        Assertions.assertThat(summary.getTotalExpense()).isEqualByComparingTo(new BigDecimal("2500.00"));
        Assertions.assertThat(summary.getHeadWiseTotals()).hasSize(1);
        Assertions.assertThat(summary.getHeadWiseTotals().get(0).getExpenseHeadName()).isEqualTo("Electricity Bill");
    }

    @AfterEach
    void cleanup() {
        fullCleanup();
    }
}
