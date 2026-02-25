package com.school.backend.core.dashboard.service;

import com.school.backend.common.enums.UserRole;
import com.school.backend.common.exception.InvalidOperationException;
import com.school.backend.common.tenant.SessionContext;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.core.dashboard.dto.SchoolAdminStatsDto;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.fee.service.FeeSummaryService;
import com.school.backend.testmanagement.repository.ExamRepository;
import com.school.backend.transport.repository.TransportEnrollmentRepository;
import com.school.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardStatsService {

        private final StudentRepository studentRepository;
        private final UserRepository userRepository;
        private final TransportEnrollmentRepository transportRepository;
        private final ExamRepository examRepository;
        private final FeeSummaryService feeSummaryService;
        private final com.school.backend.core.attendance.service.AttendanceService attendanceService;

        public SchoolAdminStatsDto getSchoolAdminStats() {
                Long schoolId = TenantContext.getSchoolId();
                Long effectiveSessionId = SessionContext.getSessionId();
                if (effectiveSessionId == null) {
                        throw new InvalidOperationException("Session context is missing in request");
                }

                // 1. Basic Counts
                long totalStudents = studentRepository.countBySchoolIdAndSessionId(schoolId, effectiveSessionId);
                long transportCount = transportRepository.countBySchoolIdAndSessionId(schoolId, effectiveSessionId);
                long totalTeachers = userRepository.countBySchoolIdAndRole(schoolId, UserRole.TEACHER);

                // 2. Fee Stats (Defaulters Count)
                long feePendingCount = feeSummaryService.countDefaulters();

                // 3. Attendance Stats
                double attendancePercentage = attendanceService.getTodayStats(schoolId, effectiveSessionId);

                // 4. Upcoming Exams
                LocalDate now = LocalDate.now();
                var upcomingExams = examRepository
                                .findBySchoolIdAndSessionIdAndStartDateAfter(schoolId, effectiveSessionId, now)
                                .stream()
                                .limit(5)
                                .map(e -> SchoolAdminStatsDto.UpcomingExamDto.builder()
                                                .name(e.getName())
                                                .date(e.getStartDate() != null ? e.getStartDate().toString() : "TBD")
                                                .className("Class " + e.getClassId()) // Need class name here ideally
                                                .build())
                                .collect(Collectors.toList());

                return SchoolAdminStatsDto.builder()
                                .totalStudents(totalStudents)
                                .transportCount(transportCount)
                                .totalTeachers(totalTeachers)
                                .feePendingCount(feePendingCount)
                                .upcomingExams(upcomingExams)
                                .attendancePercentage(attendancePercentage)
                                .build();
        }
}
