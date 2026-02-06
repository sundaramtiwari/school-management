package com.school.backend.core.attendance.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.core.attendance.entity.StudentAttendance;
import com.school.backend.core.attendance.enums.AttendanceStatus;
import com.school.backend.core.attendance.repository.AttendanceRepository;
import com.school.backend.core.student.repository.StudentEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentEnrollmentRepository enrollmentRepository;

    @Transactional
    public void markAttendanceBulk(LocalDate date, Map<Long, AttendanceStatus> attendanceMap, Long schoolId) {
        // 1. Fetch existing attendance for these students on this date
        List<Long> studentIds = new ArrayList<>(attendanceMap.keySet());
        List<StudentAttendance> existing = attendanceRepository.findByAttendanceDateAndStudentIdIn(date, studentIds);
        Map<Long, StudentAttendance> existingMap = existing.stream()
                .collect(Collectors.toMap(StudentAttendance::getStudentId, a -> a));

        List<StudentAttendance> toSave = new ArrayList<>();

        for (Map.Entry<Long, AttendanceStatus> entry : attendanceMap.entrySet()) {
            Long studentId = entry.getKey();
            AttendanceStatus status = entry.getValue();

            StudentAttendance attendance = existingMap.getOrDefault(studentId,
                    StudentAttendance.builder()
                            .studentId(studentId)
                            .attendanceDate(date)
                            .schoolId(schoolId)
                            .build());

            attendance.setStatus(status);
            toSave.add(attendance);
        }

        attendanceRepository.saveAll(toSave);
    }

    @Transactional(readOnly = true)
    public List<StudentAttendance> getAttendanceByClassAndDate(Long classId, String session, LocalDate date) {
        List<Long> studentIds = enrollmentRepository.findByClassIdAndSession(classId, session)
                .stream()
                .map(e -> e.getStudentId())
                .collect(Collectors.toList());

        if (studentIds.isEmpty())
            return new ArrayList<>();

        return attendanceRepository.findByAttendanceDateAndStudentIdIn(date, studentIds);
    }
}
