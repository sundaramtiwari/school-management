package com.school.backend.school.service;

import com.school.backend.common.exception.BusinessException;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import com.school.backend.school.repository.AcademicSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SetupValidationService {

    private final AcademicSessionRepository academicSessionRepository;
    private final SchoolClassRepository schoolClassRepository;

    @Transactional(readOnly = true)
    public void ensureSessionExists(Long schoolId) {
        if (schoolId == null)
            return;

        // This check might be redundant if tenancy handles it, but good for explicit
        // safety
        boolean hasSession = academicSessionRepository.findBySchoolId(schoolId).stream()
                .anyMatch(s -> s.isActive());

        // Note: For strict School -> Session workflow, we might check
        // School.currentSessionId
        // But checking repository is safer against stale data.
    }

    @Transactional(readOnly = true)
    public void ensureAtLeastOneClassExists(Long schoolId, Long sessionId) {
        if (schoolId == null || sessionId == null) {
            throw new BusinessException("Session ID is required for operation.");
        }

        long classCount = schoolClassRepository.countBySchoolIdAndSessionId(schoolId, sessionId);
        if (classCount == 0) {
            throw new BusinessException("At least one class must be created before accessing this module.");
        }
    }
}
