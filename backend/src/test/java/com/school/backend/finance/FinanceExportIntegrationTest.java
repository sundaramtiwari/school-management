package com.school.backend.finance;

import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.school.entity.School;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Objects;

public class FinanceExportIntegrationTest extends BaseAuthenticatedIntegrationTest {

    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Test
    void all_finance_export_endpoints_should_return_xlsx_file() {
        ResponseEntity<School> schoolResp = restTemplate.exchange(
                "/api/schools",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "name", "Export School",
                        "displayName", "EXP",
                        "board", "CBSE",
                        "schoolCode", "EXP-001"), headers),
                School.class);
        Long schoolId = Objects.requireNonNull(schoolResp.getBody()).getId();
        loginAsSchoolAdmin(schoolId);

        Long sessionId = setupSession(schoolId, sessionRepository, schoolRepository);
        setSessionHeader(sessionId);

        assertExcel("/api/finance/export/daily-cash?date=2026-02-25");
        assertExcel("/api/finance/export/range-pl?start=2026-02-01&end=2026-02-28");
        assertExcel("/api/finance/export/range-pl?start=2025-04-01&end=2026-03-31");
        assertExcel("/api/finance/export/expenses?date=2026-02-25");
    }

    private void assertExcel(String url) {
        ResponseEntity<byte[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                byte[].class);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getHeaders().getContentType()).isNotNull();
        Assertions.assertThat(Objects.requireNonNull(response.getHeaders().getContentType()).toString())
                .isEqualTo(XLSX_CONTENT_TYPE);
        byte[] body = response.getBody();
        Assertions.assertThat(body).isNotNull();
        Assertions.assertThat(body.length).isGreaterThan(0);
    }

    @AfterEach
    void cleanup() {
        fullCleanup();
    }
}
