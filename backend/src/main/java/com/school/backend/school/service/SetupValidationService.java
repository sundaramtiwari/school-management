package com.school.backend.school.service;

import com.school.backend.common.exception.BusinessException;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SetupValidationService {

    private final SchoolClassRepository schoolClassRepository;

    @Transactional(readOnly = true)
    public void ensureAtLeastOneClassExists(Long schoolId, Long sessionId) {
        log.debug("Validating setup prerequisites schoolId={} sessionId={}", schoolId, sessionId);
        if (schoolId == null || sessionId == null) {
            log.warn("Setup validation failed: missing school/session context schoolId={} sessionId={}", schoolId, sessionId);
            throw new BusinessException("Session ID is required for operation.");
        }

        long classCount = schoolClassRepository.countBySchoolIdAndSessionId(schoolId, sessionId);
        if (classCount == 0) {
            log.warn("Setup validation failed: no classes for schoolId={} sessionId={}", schoolId, sessionId);
            throw new BusinessException("At least one class must be created before accessing this module.");
        }
    }
}
