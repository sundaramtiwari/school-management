package com.school.backend.core.attendance.controller;

import com.school.backend.core.attendance.entity.StudentAttendance;
import com.school.backend.core.attendance.enums.AttendanceStatus;
import com.school.backend.core.attendance.service.AttendanceService;
import com.school.backend.common.tenant.SessionResolver;
import com.school.backend.user.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final SessionResolver sessionResolver;

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public void markAttendanceBulk(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody Map<Long, AttendanceStatus> attendanceMap) {

        attendanceService.markAttendanceBulk(date, attendanceMap, SecurityUtil.schoolId());
    }

    @GetMapping("/class/{classId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public List<StudentAttendance> getAttendanceByClassAndDate(
            @PathVariable Long classId,
            @RequestParam(required = false) Long sessionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        Long effectiveSessionId = sessionId != null ? sessionId : sessionResolver.resolveForCurrentSchool();
        return attendanceService.getAttendanceByClassAndDate(classId, effectiveSessionId, date);
    }

    @GetMapping("/stats/today")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Double> getTodayStats() {
        double stats = attendanceService.getTodayStats(SecurityUtil.schoolId());
        return Map.of("percentage", stats);
    }
}
