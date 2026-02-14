package com.school.backend.platform.dashboard;

import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.core.teacher.repository.TeacherRepository;
import com.school.backend.school.repository.AcademicSessionRepository;
import com.school.backend.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlatformDashboardService {

    private final SchoolRepository schoolRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final AcademicSessionRepository academicSessionRepository;

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
}
