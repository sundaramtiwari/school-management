package com.school.backend.platform.dashboard;

import com.school.backend.common.tenant.TenantContext;
import com.school.backend.core.attendance.enums.AttendanceStatus;
import com.school.backend.core.attendance.repository.AttendanceRepository;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.core.teacher.repository.TeacherRepository;
import com.school.backend.platform.dashboard.dto.SchoolAdminStatsDto;
import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.repository.AcademicSessionRepository;
import com.school.backend.school.repository.SchoolRepository;
import com.school.backend.testmanagement.repository.ExamRepository;
import com.school.backend.transport.repository.TransportEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlatformDashboardService {

    private final SchoolRepository schoolRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final AcademicSessionRepository academicSessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final ExamRepository examRepository;
    private final TransportEnrollmentRepository transportEnrollmentRepository;

    @Transactional(readOnly = true)
    public PlatformDashboardResponse getDashboardStats() {
        long totalSchools = schoolRepository.count();
        long totalStudents = studentRepository.count();
        long totalTeachers = teacherRepository.count();
        long totalActiveSessions = academicSessionRepository.countByActiveTrue();

        // Subscription due count is hardcoded to 0 for now as per requirements
        long subscriptionDueCount = 0;

        return PlatformDashboardResponse.builder()
                .totalSchools(totalSchools)
                .totalStudents(totalStudents)
                .totalTeachers(totalTeachers)
                .totalActiveSessions(totalActiveSessions)
                .subscriptionDueCount(subscriptionDueCount)
                .build();
    }

    @Transactional(readOnly = true)
    public SchoolAdminStatsDto getSchoolAdminStats(Long sessionId) {
        Long schoolId = TenantContext.getSchoolId();

        AcademicSession session = academicSessionRepository.findByIdAndSchoolId(sessionId, schoolId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        long totalStudents = studentRepository.countBySchoolIdAndSessionId(schoolId, sessionId);
        long totalTeachers = teacherRepository.countBySchoolId(schoolId);
        long transportCount = transportEnrollmentRepository.countBySchoolIdAndSessionId(schoolId, sessionId);

        // Defaulters (min amount 1)
        long feePendingCount = studentRepository.countDefaulters(
                schoolId, sessionId, null, null, BigDecimal.ONE, LocalDate.now(), session.getStartDate());

        // Attendance (today)
        long present = attendanceRepository.countByAttendanceDateAndStatusAndSchoolId(LocalDate.now(),
                AttendanceStatus.PRESENT, schoolId);
        long totalAttendance = attendanceRepository.countByAttendanceDateAndSchoolId(LocalDate.now(), schoolId);
        double attendancePercentage = totalAttendance == 0 ? 0 : (double) present * 100 / totalAttendance;

        // Upcoming Exams (Limit 5)
        var exams = examRepository.findUpcomingExamViews(schoolId, sessionId, LocalDate.now(), PageRequest.of(0, 5));

        return SchoolAdminStatsDto.builder()
                .totalStudents(totalStudents)
                .totalTeachers(totalTeachers)
                .transportCount(transportCount)
                .feePendingCount(feePendingCount)
                .attendancePercentage(Math.round(attendancePercentage * 10.0) / 10.0)
                .upcomingExams(exams.stream().map(e -> SchoolAdminStatsDto.UpcomingExamDto.builder()
                        .name(e.getName())
                        .date(e.getStartDate().toString())
                        .className(e.getClassName() + " " + e.getClassSection())
                        .build()).collect(Collectors.toList()))
                .build();
    }
}
