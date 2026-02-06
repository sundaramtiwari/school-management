package com.school.backend.core.attendance.controller;

import com.school.backend.core.attendance.entity.StudentAttendance;
import com.school.backend.core.attendance.enums.AttendanceStatus;
import com.school.backend.core.attendance.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/bulk")
    public void markAttendanceBulk(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Long schoolId,
            @RequestBody Map<Long, AttendanceStatus> attendanceMap) {

        attendanceService.markAttendanceBulk(date, attendanceMap, schoolId);
    }

    @GetMapping("/class/{classId}")
    public List<StudentAttendance> getAttendanceByClassAndDate(
            @PathVariable Long classId,
            @RequestParam String session,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return attendanceService.getAttendanceByClassAndDate(classId, session, date);
    }
}
