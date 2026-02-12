package com.school.backend.core.dashboard.controller;

import com.school.backend.core.dashboard.dto.SchoolAdminStatsDto;
import com.school.backend.core.dashboard.service.DashboardStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardStatsController {

    private final DashboardStatsService service;

    @GetMapping("/school-admin/stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public SchoolAdminStatsDto getSchoolAdminStats(@RequestParam(required = false) Long sessionId) {
        return service.getSchoolAdminStats(sessionId);
    }
}
