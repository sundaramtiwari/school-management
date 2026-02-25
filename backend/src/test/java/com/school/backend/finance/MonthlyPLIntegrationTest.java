package com.school.backend.finance;

import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.common.enums.ExpensePaymentMode;
import com.school.backend.expense.entity.ExpenseHead;
import com.school.backend.expense.entity.ExpenseVoucher;
import com.school.backend.fee.entity.FeePayment;
import com.school.backend.finance.dto.MonthlyPLResponseDto;
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

public class MonthlyPLIntegrationTest extends BaseAuthenticatedIntegrationTest {

    @Test
    void monthly_pl_should_return_correct_totals_and_mode_segregation() {
        ResponseEntity<School> schoolResp = restTemplate.exchange(
                "/api/schools",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "name", "Finance PnL School",
                        "displayName", "FPS",
                        "board", "CBSE",
                        "schoolCode", "FPS-001"), headers),
                School.class);
        Long schoolId = Objects.requireNonNull(schoolResp.getBody()).getId();
        loginAsSchoolAdmin(schoolId);

        Long sessionId = setupSession(schoolId, sessionRepository, schoolRepository);
        setSessionHeader(sessionId);

        // Revenue entries in Feb 2026: one cash, one bank (UPI should be bank bucket)
        feePaymentRepository.save(FeePayment.builder()
                .schoolId(schoolId)
                .sessionId(sessionId)
                .studentId(1001L)
                .principalPaid(new BigDecimal("500.00"))
                .lateFeePaid(new BigDecimal("50.00"))
                .paymentDate(LocalDate.of(2026, 2, 10))
                .mode(" cash ")
                .remarks("cash revenue")
                .build());

        feePaymentRepository.save(FeePayment.builder()
                .schoolId(schoolId)
                .sessionId(sessionId)
                .studentId(1002L)
                .principalPaid(new BigDecimal("300.00"))
                .lateFeePaid(BigDecimal.ZERO)
                .paymentDate(LocalDate.of(2026, 2, 12))
                .mode("UPI")
                .remarks("bank revenue")
                .build());

        // Out-of-month noise (should not be counted)
        feePaymentRepository.save(FeePayment.builder()
                .schoolId(schoolId)
                .sessionId(sessionId)
                .studentId(1003L)
                .principalPaid(new BigDecimal("999.00"))
                .lateFeePaid(BigDecimal.ZERO)
                .paymentDate(LocalDate.of(2026, 3, 1))
                .mode("CASH")
                .remarks("out of month")
                .build());

        ExpenseHead stationery = expenseHeadRepository.save(ExpenseHead.builder()
                .schoolId(schoolId)
                .name("Stationery")
                .normalizedName("Stationery")
                .description("stationery")
                .active(true)
                .build());

        ExpenseHead maintenance = expenseHeadRepository.save(ExpenseHead.builder()
                .schoolId(schoolId)
                .name("Maintenance")
                .normalizedName("Maintenance")
                .description("maintenance")
                .active(true)
                .build());

        // Expense entries in Feb 2026: one cash, one bank
        expenseVoucherRepository.save(ExpenseVoucher.builder()
                .schoolId(schoolId)
                .sessionId(sessionId)
                .voucherNumber("EXP-" + sessionId + "-00901")
                .expenseDate(LocalDate.of(2026, 2, 15))
                .expenseHead(stationery)
                .amount(new BigDecimal("200.00"))
                .paymentMode(ExpensePaymentMode.CASH)
                .createdBy(1L)
                .active(true)
                .build());

        expenseVoucherRepository.save(ExpenseVoucher.builder()
                .schoolId(schoolId)
                .sessionId(sessionId)
                .voucherNumber("EXP-" + sessionId + "-00902")
                .expenseDate(LocalDate.of(2026, 2, 20))
                .expenseHead(maintenance)
                .amount(new BigDecimal("80.00"))
                .paymentMode(ExpensePaymentMode.BANK)
                .createdBy(1L)
                .active(true)
                .build());

        // Out-of-month expense noise
        expenseVoucherRepository.save(ExpenseVoucher.builder()
                .schoolId(schoolId)
                .sessionId(sessionId)
                .voucherNumber("EXP-" + sessionId + "-00903")
                .expenseDate(LocalDate.of(2026, 3, 2))
                .expenseHead(maintenance)
                .amount(new BigDecimal("777.00"))
                .paymentMode(ExpensePaymentMode.CASH)
                .createdBy(1L)
                .active(true)
                .build());

        ResponseEntity<MonthlyPLResponseDto> response = restTemplate.exchange(
                "/api/finance/monthly-pl?year=2026&month=2",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                MonthlyPLResponseDto.class);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        MonthlyPLResponseDto body = Objects.requireNonNull(response.getBody());
        Assertions.assertThat(body.getYear()).isEqualTo(2026);
        Assertions.assertThat(body.getMonth()).isEqualTo(2);

        Assertions.assertThat(body.getCashRevenue()).isEqualByComparingTo(new BigDecimal("550.00"));
        Assertions.assertThat(body.getBankRevenue()).isEqualByComparingTo(new BigDecimal("300.00"));
        Assertions.assertThat(body.getCashExpense()).isEqualByComparingTo(new BigDecimal("200.00"));
        Assertions.assertThat(body.getBankExpense()).isEqualByComparingTo(new BigDecimal("80.00"));
        Assertions.assertThat(body.getNetCash()).isEqualByComparingTo(new BigDecimal("350.00"));
        Assertions.assertThat(body.getNetBank()).isEqualByComparingTo(new BigDecimal("220.00"));

        Assertions.assertThat(body.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("850.00"));
        Assertions.assertThat(body.getTotalExpense()).isEqualByComparingTo(new BigDecimal("280.00"));
        Assertions.assertThat(body.getNetProfit()).isEqualByComparingTo(new BigDecimal("570.00"));
    }

    @AfterEach
    void cleanup() {
        fullCleanup();
    }
}
