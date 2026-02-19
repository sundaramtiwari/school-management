package com.school.backend.fee.controller;

import com.school.backend.fee.dto.FeeStatsDto;
import com.school.backend.fee.dto.FeeSummaryDto;
import com.school.backend.fee.dto.StudentLedgerDto;
import com.school.backend.fee.service.FeeSummaryService;
import com.school.backend.common.tenant.SessionResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fees/summary")
@RequiredArgsConstructor
public class FeeSummaryController {

    private final FeeSummaryService service;
    private final SessionResolver sessionResolver;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public FeeStatsDto getDashboardStats(@RequestParam(required = false) Long sessionId) {
        Long effectiveSessionId = sessionId != null ? sessionId : sessionResolver.resolveForCurrentSchool();
        return service.getDashboardStats(effectiveSessionId);
    }

    @GetMapping("/students/{studentId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public FeeSummaryDto getStudentFeeSummary(
            @PathVariable Long studentId,
            @RequestParam(required = false) Long sessionId) {

        Long effectiveSessionId = sessionId != null ? sessionId : sessionResolver.resolveForCurrentSchool();
        return service.getStudentFeeSummary(studentId, effectiveSessionId);
    }

    @GetMapping("/students/{studentId}/ledger")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public StudentLedgerDto getFullLedger(@PathVariable Long studentId) {
        return service.getStudentFullLedger(studentId);
    }
}
