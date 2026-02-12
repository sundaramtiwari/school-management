package com.school.backend.fee.controller;

import com.school.backend.fee.dto.FeeStatsDto;
import com.school.backend.fee.dto.FeeSummaryDto;
import com.school.backend.fee.service.FeeSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fees/summary")
@RequiredArgsConstructor
public class FeeSummaryController {

    private final FeeSummaryService service;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public FeeStatsDto getDashboardStats(@RequestParam Long sessionId) {
        return service.getDashboardStats(sessionId);
    }

    @GetMapping("/students/{studentId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'TEACHER', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public FeeSummaryDto getStudentFeeSummary(
            @PathVariable Long studentId,
            @RequestParam Long sessionId) {

        return service.getStudentFeeSummary(studentId, sessionId);
    }
}
