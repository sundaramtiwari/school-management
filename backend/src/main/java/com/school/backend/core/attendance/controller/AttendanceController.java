package com.school.backend.core.attendance.controller;

import com.school.backend.common.tenant.SessionResolver;
import com.school.backend.core.attendance.dto.AttendanceResponse;
import com.school.backend.core.attendance.entity.StudentAttendance;
import com.school.backend.core.attendance.enums.AttendanceStatus;
import com.school.backend.core.attendance.service.AttendanceService;
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
            @RequestParam Long classId,
            @RequestParam(required = false) Long sessionId,
            @RequestBody Map<Long, AttendanceStatus> attendanceMap) {

        Long effectiveSessionId = sessionId != null ? sessionId : sessionResolver.resolveForCurrentSchool();
        attendanceService.markAttendanceBulk(date, classId, effectiveSessionId, attendanceMap, SecurityUtil.schoolId());
    }

    @GetMapping("/class/{classId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public AttendanceResponse getAttendanceByClassAndDate(
            @PathVariable Long classId,
            @RequestParam(required = false) Long sessionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        Long effectiveSessionId = sessionId != null ? sessionId : sessionResolver.resolveForCurrentSchool();
        List<StudentAttendance> attendance = attendanceService.getAttendanceByClassAndDate(classId, effectiveSessionId,
                date);
        boolean editable = attendanceService.isEditable(date);
        boolean committed = !attendance.isEmpty();

        return new AttendanceResponse(attendance, editable, committed);
    }

    @GetMapping("/stats/today")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Double> getTodayStats(@RequestParam(required = false) Long sessionId) {
        Long effectiveSessionId = sessionId != null ? sessionId : sessionResolver.resolveForCurrentSchool();
        double stats = attendanceService.getTodayStats(SecurityUtil.schoolId(), effectiveSessionId);
        return Map.of("percentage", stats);
    }
}
