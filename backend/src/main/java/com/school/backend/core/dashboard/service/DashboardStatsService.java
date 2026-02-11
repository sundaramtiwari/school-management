package com.school.backend.core.dashboard.service;

import com.school.backend.common.enums.UserRole;
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

    public SchoolAdminStatsDto getSchoolAdminStats(String session) {
        Long schoolId = TenantContext.getSchoolId();

        // 1. Basic Counts
        long totalStudents = studentRepository.countBySchoolId(schoolId);
        long transportCount = transportRepository.countBySchoolId(schoolId);
        long totalTeachers = userRepository.countBySchoolIdAndRole(schoolId, UserRole.TEACHER);

        // 2. Fee Stats (Defaulters Count)
        long feePendingCount = feeSummaryService.getAllDefaulters().size();

        // 3. Upcoming Exams
        LocalDate now = LocalDate.now();
        var upcomingExams = examRepository.findBySchoolIdAndStartDateAfter(schoolId, now)
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
                .attendancePercentage(85.0) // Mock for now until attendance service is checked
                .build();
    }
}
