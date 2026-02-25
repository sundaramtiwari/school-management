package com.school.backend.fee.controller;

import com.school.backend.fee.dto.FeeStatsDto;
import com.school.backend.fee.dto.FeeSummaryDto;
import com.school.backend.fee.dto.StudentLedgerDto;
import com.school.backend.fee.service.FeeSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fees/summary")
@RequiredArgsConstructor
public class FeeSummaryController {

    private final FeeSummaryService service;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public FeeStatsDto getDashboardStats() {
        return service.getDashboardStats();
    }

    @GetMapping("/students/{studentId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public FeeSummaryDto getStudentFeeSummary(
            @PathVariable Long studentId) {
        return service.getStudentFeeSummary(studentId);
    }

    @GetMapping("/students/{studentId}/ledger")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public StudentLedgerDto getFullLedger(@PathVariable Long studentId) {
        return service.getStudentFullLedger(studentId);
    }
}
