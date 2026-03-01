package com.school.backend.finance.controller;

import com.school.backend.common.exception.BusinessException;
import com.school.backend.finance.dto.DailyCashDashboardDto;
import com.school.backend.finance.dto.FinancialOverviewDto;
import com.school.backend.finance.service.FinanceOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/finance/overview")
@RequiredArgsConstructor
public class FinanceOverviewController {

    private final FinanceOverviewService service;

    @GetMapping("/daily")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN')")
    public DailyCashDashboardDto getDailyOverview(@RequestParam(required = false) LocalDate date) {
        return service.getDailyOverview(date);
    }

    @GetMapping("/range")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN')")
    public FinancialOverviewDto getRangeOverview(
            @RequestParam LocalDate start,
            @RequestParam LocalDate end) {
        if (start == null || end == null) {
            throw new BusinessException("Start and End dates are required");
        }
        if (end.isBefore(start)) {
            throw new BusinessException("End date cannot be before Start date");
        }
        return service.getRangeOverview(start, end);
    }
}
