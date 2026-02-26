package com.school.backend.finance;

import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.core.dashboard.dto.DailyCashDashboardDto;
import com.school.backend.fee.entity.FeePayment;
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
import java.util.Map;
import java.util.Objects;

public class FinanceAccountTransferIntegrationTest extends BaseAuthenticatedIntegrationTest {

    @Test
    void transfer_should_reduce_cash_increase_bank_and_keep_revenue_unchanged() {
        ResponseEntity<School> schoolResp = restTemplate.exchange(
                "/api/schools",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "name", "Transfer School",
                        "displayName", "TS",
                        "board", "CBSE",
                        "schoolCode", "TRF-001"), headers),
                School.class);
        Long schoolId = Objects.requireNonNull(schoolResp.getBody()).getId();
        loginAsSchoolAdmin(schoolId);

        Long sessionId = setupSession(schoolId, sessionRepository, schoolRepository);
        setSessionHeader(sessionId);

        LocalDate date = LocalDate.of(2026, 2, 20);
        feePaymentRepository.save(FeePayment.builder()
                .schoolId(schoolId)
                .sessionId(sessionId)
                .studentId(3001L)
                .principalPaid(new BigDecimal("500.00"))
                .lateFeePaid(BigDecimal.ZERO)
                .paymentDate(date)
                .mode("CASH")
                .build());

        FinanceAccountTransferRequest req = new FinanceAccountTransferRequest();
        req.setTransferDate(date);
        req.setAmount(new BigDecimal("200.00"));
        req.setReferenceNumber("TRF-001");
        req.setRemarks("Cash to bank");

        ResponseEntity<Map> transferResp = restTemplate.exchange(
                "/api/finance/transfers",
                HttpMethod.POST,
                new HttpEntity<>(req, headers),
                Map.class);
        Assertions.assertThat(transferResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<DailyCashDashboardDto> dailyResp = restTemplate.exchange(
                "/api/dashboard/daily-cash?date=" + date,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                DailyCashDashboardDto.class);
        Assertions.assertThat(dailyResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        DailyCashDashboardDto body = Objects.requireNonNull(dailyResp.getBody());

        Assertions.assertThat(body.getTotalFeeCollected()).isEqualByComparingTo(new BigDecimal("500.00"));
        Assertions.assertThat(body.getCashRevenue()).isEqualByComparingTo(new BigDecimal("500.00"));
        Assertions.assertThat(body.getBankRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        Assertions.assertThat(body.getNetCash()).isEqualByComparingTo(new BigDecimal("300.00"));
        Assertions.assertThat(body.getNetBank()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @AfterEach
    void cleanup() {
        fullCleanup();
    }
}
