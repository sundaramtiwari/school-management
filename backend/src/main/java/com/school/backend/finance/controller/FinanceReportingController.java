package com.school.backend.finance.controller;

import com.school.backend.common.exception.BusinessException;
import com.school.backend.finance.dto.MonthlyPLResponseDto;
import com.school.backend.finance.dto.SessionPLResponseDto;
import com.school.backend.finance.service.FinanceReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinanceReportingController {

    private final FinanceReportingService financeReportingService;

    @GetMapping("/monthly-pl")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN')")
    public MonthlyPLResponseDto getMonthlyPL(
            @RequestParam int year,
            @RequestParam int month) {
        if (year <= 2000) {
            throw new BusinessException("Year must be greater than 2000");
        }
        if (month < 1 || month > 12) {
            throw new BusinessException("Month must be between 1 and 12");
        }
        return financeReportingService.getMonthlyPL(year, month);
    }

    @GetMapping("/session-pl")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN')")
    public SessionPLResponseDto getSessionPL() {
        return financeReportingService.getSessionPL();
    }
}
