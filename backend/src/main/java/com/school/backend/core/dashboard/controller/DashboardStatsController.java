package com.school.backend.core.dashboard.controller;

import com.school.backend.core.dashboard.dto.DailyCashDashboardDto;
import com.school.backend.core.dashboard.dto.SchoolAdminStatsDto;
import com.school.backend.core.dashboard.service.DashboardStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardStatsController {

    private final DashboardStatsService service;

    @GetMapping("/school-admin/stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public SchoolAdminStatsDto getSchoolAdminStats() {
        return service.getSchoolAdminStats();
    }

    @GetMapping("/daily-cash")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN')")
    public DailyCashDashboardDto getDailyCash(@RequestParam(required = false) LocalDate date) {
        return service.getDailyCashDashboard(date);
    }
}
