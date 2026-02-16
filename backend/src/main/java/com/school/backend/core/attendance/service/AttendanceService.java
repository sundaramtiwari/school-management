package com.school.backend.core.attendance.service;

import com.school.backend.common.exception.BusinessException;
import com.school.backend.core.attendance.entity.StudentAttendance;
import com.school.backend.core.attendance.enums.AttendanceStatus;
import com.school.backend.core.attendance.repository.AttendanceRepository;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import com.school.backend.core.student.entity.StudentEnrollment;
import com.school.backend.core.student.repository.StudentEnrollmentRepository;
import com.school.backend.user.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final SchoolClassRepository classRepository;

    /**
     * Timezone-safe validation - Teachers can only edit today's attendance
     */
    private void validateEditPermission(LocalDate date) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        boolean isTeacher = SecurityUtil.hasRole("TEACHER");

        if (isTeacher && !date.equals(today)) {
            throw new BusinessException("Teachers can only edit today's attendance");
        }
        // SCHOOL_ADMIN, SUPER_ADMIN, PLATFORM_ADMIN can edit any date
    }

    /**
     * Check if attendance is editable for current user
     */
    public boolean isEditable(LocalDate date) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        boolean isTeacher = SecurityUtil.hasRole("TEACHER");
        return !isTeacher || date.equals(today);
    }

    /**
     * Validate tenant access for class
     */
    private void validateClassAccess(Long classId, Long schoolId) {
        if (!classRepository.existsByIdAndSchoolId(classId, schoolId)) {
            throw new BusinessException("Invalid class access");
        }
    }

    @Transactional
    public void markAttendanceBulk(LocalDate date, Long classId, Long sessionId,
            Map<Long, AttendanceStatus> attendanceMap, Long schoolId) {
        // 1. Validate edit permission
        validateEditPermission(date);

        // 2. Validate tenant access
        validateClassAccess(classId, schoolId);

        // 3. Fetch existing attendance for these students on this date
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
                            .classId(classId)
                            .sessionId(sessionId)
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
    public List<StudentAttendance> getAttendanceByClassAndDate(Long classId, Long sessionId, LocalDate date) {
        List<Long> studentIds = enrollmentRepository.findByClassIdAndSessionId(classId, sessionId)
                .stream()
                .map(StudentEnrollment::getStudentId)
                .collect(Collectors.toList());

        if (studentIds.isEmpty())
            return new ArrayList<>();

        return attendanceRepository.findByAttendanceDateAndStudentIdIn(date, studentIds);
    }

    @Transactional(readOnly = true)
    public double getTodayStats(Long schoolId, Long sessionId) {
        long totalStudents = enrollmentRepository
                .countBySchoolIdAndSessionIdAndActiveTrue(schoolId, sessionId);
        if (totalStudents == 0) {
            return 0.0;
        }

        // Count PRESENT + LATE + HALF_DAY as present
        long present = attendanceRepository.countByAttendanceDateAndStatusAndSchoolId(LocalDate.now(),
                AttendanceStatus.PRESENT, schoolId);
        long late = attendanceRepository.countByAttendanceDateAndStatusAndSchoolId(LocalDate.now(),
                AttendanceStatus.LATE, schoolId);
        long halfDay = attendanceRepository.countByAttendanceDateAndStatusAndSchoolId(LocalDate.now(),
                AttendanceStatus.HALF_DAY, schoolId);

        return ((double) (present + late + halfDay) / totalStudents) * 100.0;
    }

}
