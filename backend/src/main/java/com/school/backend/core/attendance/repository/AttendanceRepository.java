package com.school.backend.core.attendance.repository;

import com.school.backend.core.attendance.entity.StudentAttendance;
import com.school.backend.core.attendance.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<StudentAttendance, Long> {

    List<StudentAttendance> findByStudentIdAndAttendanceDateBetween(Long studentId, LocalDate start, LocalDate end);

    Optional<StudentAttendance> findByStudentIdAndAttendanceDate(Long studentId, LocalDate date);

    List<StudentAttendance> findByAttendanceDateAndStudentIdIn(LocalDate date, List<Long> studentIds);

    long countByAttendanceDateAndStatusAndSchoolId(LocalDate date,
            AttendanceStatus status, Long schoolId);

    long countByAttendanceDateAndSchoolId(LocalDate date, Long schoolId);
}
