package com.school.backend.finance;

import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.common.enums.ExpensePaymentMode;
import com.school.backend.expense.entity.ExpenseHead;
import com.school.backend.expense.entity.ExpenseVoucher;
import com.school.backend.fee.entity.FeePayment;
import com.school.backend.finance.dto.FinancialOverviewDto;
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

public class SessionPLIntegrationTest extends BaseAuthenticatedIntegrationTest {

    @Test
    void session_pl_should_include_only_entries_within_session_date_range() {
        ResponseEntity<School> schoolResp = restTemplate.exchange(
                "/api/schools",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "name", "Session PnL School",
                        "displayName", "SPS",
                        "board", "CBSE",
                        "schoolCode", "SPS-PL-001"), headers),
                School.class);
        Long schoolId = Objects.requireNonNull(schoolResp.getBody()).getId();
        loginAsSchoolAdmin(schoolId);

        Long sessionId = setupSession(schoolId, sessionRepository, schoolRepository); // 2025-04-01 to 2026-03-31
        setSessionHeader(sessionId);

        LocalDate inRange1 = LocalDate.of(2025, 7, 10);
        LocalDate inRange2 = LocalDate.of(2025, 12, 5);
        LocalDate outOfRangeBefore = LocalDate.of(2025, 3, 30);
        LocalDate outOfRangeAfter = LocalDate.of(2026, 4, 2);

        // Revenue: in-range (cash + bank) and out-of-range noise
        feePaymentRepository.save(FeePayment.builder()
                .schoolId(schoolId)
                .sessionId(sessionId)
                .studentId(2001L)
                .principalPaid(new BigDecimal("400.00"))
                .lateFeePaid(new BigDecimal("20.00"))
                .paymentDate(inRange1)
                .mode("CASH")
                .build());
        feePaymentRepository.save(FeePayment.builder()
                .schoolId(schoolId)
                .sessionId(sessionId)
                .studentId(2002L)
                .principalPaid(new BigDecimal("300.00"))
                .lateFeePaid(BigDecimal.ZERO)
                .paymentDate(inRange2)
                .mode("bank")
                .build());
        feePaymentRepository.save(FeePayment.builder()
                .schoolId(schoolId)
                .sessionId(sessionId)
                .studentId(2003L)
                .principalPaid(new BigDecimal("999.00"))
                .lateFeePaid(BigDecimal.ZERO)
                .paymentDate(outOfRangeBefore)
                .mode("CASH")
                .build());
        feePaymentRepository.save(FeePayment.builder()
                .schoolId(schoolId)
                .sessionId(sessionId)
                .studentId(2004L)
                .principalPaid(new BigDecimal("888.00"))
                .lateFeePaid(BigDecimal.ZERO)
                .paymentDate(outOfRangeAfter)
                .mode("UPI")
                .build());

        ExpenseHead head = expenseHeadRepository.save(ExpenseHead.builder()
                .schoolId(schoolId)
                .name("Operations")
                .normalizedName("Operations")
                .active(true)
                .build());

        // Expenses: in-range (cash + bank) and out-of-range noise
        expenseVoucherRepository.save(ExpenseVoucher.builder()
                .schoolId(schoolId)
                .sessionId(sessionId)
                .voucherNumber("EXP-" + sessionId + "-01001")
                .expenseDate(inRange1)
                .expenseHead(head)
                .amount(new BigDecimal("150.00"))
                .paymentMode(ExpensePaymentMode.CASH)
                .createdBy(1L)
                .active(true)
                .build());
        expenseVoucherRepository.save(ExpenseVoucher.builder()
                .schoolId(schoolId)
                .sessionId(sessionId)
                .voucherNumber("EXP-" + sessionId + "-01002")
                .expenseDate(inRange2)
                .expenseHead(head)
                .amount(new BigDecimal("70.00"))
                .paymentMode(ExpensePaymentMode.BANK)
                .createdBy(1L)
                .active(true)
                .build());
        expenseVoucherRepository.save(ExpenseVoucher.builder()
                .schoolId(schoolId)
                .sessionId(sessionId)
                .voucherNumber("EXP-" + sessionId + "-01003")
                .expenseDate(outOfRangeBefore)
                .expenseHead(head)
                .amount(new BigDecimal("999.00"))
                .paymentMode(ExpensePaymentMode.CASH)
                .createdBy(1L)
                .active(true)
                .build());
        expenseVoucherRepository.save(ExpenseVoucher.builder()
                .schoolId(schoolId)
                .sessionId(sessionId)
                .voucherNumber("EXP-" + sessionId + "-01004")
                .expenseDate(outOfRangeAfter)
                .expenseHead(head)
                .amount(new BigDecimal("888.00"))
                .paymentMode(ExpensePaymentMode.BANK)
                .createdBy(1L)
                .active(true)
                .build());

        ResponseEntity<FinancialOverviewDto> response = restTemplate.exchange(
                "/api/finance/overview/range?start=2025-04-01&end=2026-03-31",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                FinancialOverviewDto.class);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        FinancialOverviewDto body = Objects.requireNonNull(response.getBody());

        Assertions.assertThat(body.getCashRevenue()).isEqualByComparingTo(new BigDecimal("420.00"));
        Assertions.assertThat(body.getBankRevenue()).isEqualByComparingTo(new BigDecimal("300.00"));
        Assertions.assertThat(body.getCashExpense()).isEqualByComparingTo(new BigDecimal("150.00"));
        Assertions.assertThat(body.getBankExpense()).isEqualByComparingTo(new BigDecimal("70.00"));

        Assertions.assertThat(body.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("720.00"));
        Assertions.assertThat(body.getTotalExpense()).isEqualByComparingTo(new BigDecimal("220.00"));
        Assertions.assertThat(body.getNetProfit()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @AfterEach
    void cleanup() {
        fullCleanup();
    }
}
