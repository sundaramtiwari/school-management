package com.school.backend.finance.controller;

import com.school.backend.common.exception.BusinessException;
import com.school.backend.finance.service.FinanceExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/finance/export")
@RequiredArgsConstructor
public class FinanceExportController {

    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private final FinanceExportService financeExportService;

    @GetMapping("/daily-cash")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportDailyCash(@RequestParam LocalDate date) {
        byte[] payload = financeExportService.exportDailyCash(date);
        return excelResponse(payload, "daily_cash_" + date + ".xlsx");
    }

    @GetMapping("/monthly-pl")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportMonthlyPL(
            @RequestParam int year,
            @RequestParam int month) {
        if (year <= 2000) {
            throw new BusinessException("Year must be greater than 2000");
        }
        if (month < 1 || month > 12) {
            throw new BusinessException("Month must be between 1 and 12");
        }
        byte[] payload = financeExportService.exportMonthlyPL(year, month);
        return excelResponse(payload, "monthly_pl_" + year + "_" + String.format("%02d", month) + ".xlsx");
    }

    @GetMapping("/session-pl")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportSessionPL() {
        byte[] payload = financeExportService.exportSessionPL();
        return excelResponse(payload, "session_pl.xlsx");
    }

    @GetMapping("/expenses")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportExpenses(@RequestParam LocalDate date) {
        byte[] payload = financeExportService.exportExpenses(date);
        return excelResponse(payload, "expenses_" + date + ".xlsx");
    }

    private ResponseEntity<byte[]> excelResponse(byte[] payload, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(XLSX_CONTENT_TYPE))
                .body(payload);
    }
}
