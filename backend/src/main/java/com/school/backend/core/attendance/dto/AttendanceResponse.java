package com.school.backend.core.attendance.dto;

import com.school.backend.core.attendance.entity.StudentAttendance;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AttendanceResponse {
    private List<StudentAttendance> attendanceList;
    private boolean editable;
    private boolean committed;
}
