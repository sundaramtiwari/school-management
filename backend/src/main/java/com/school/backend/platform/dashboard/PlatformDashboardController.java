package com.school.backend.platform.dashboard;

import com.school.backend.platform.dashboard.dto.SchoolAdminStatsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform/dashboard")
@RequiredArgsConstructor
public class PlatformDashboardController {

    private final PlatformDashboardService platformDashboardService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<PlatformDashboardResponse> getDashboardStats() {
        return ResponseEntity.ok(platformDashboardService.getDashboardStats());
    }

    @GetMapping("/school-admin/stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<SchoolAdminStatsDto> getSchoolAdminStats(@RequestParam Long sessionId) {
        return ResponseEntity.ok(platformDashboardService.getSchoolAdminStats(sessionId));
    }
}
