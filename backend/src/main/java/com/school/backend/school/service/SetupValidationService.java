package com.school.backend.school.service;

import com.school.backend.common.exception.BusinessException;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import com.school.backend.school.repository.AcademicSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SetupValidationService {

    private final SchoolClassRepository schoolClassRepository;
    private final AcademicSessionRepository academicSessionRepository;

    @Transactional(readOnly = true)
    public void ensureAtLeastOneClassExists(Long schoolId, Long sessionId) {
        log.debug("Validating setup prerequisites schoolId={} sessionId={}", schoolId, sessionId);
        Long tenantSchoolId = TenantContext.getSchoolId();
        if (tenantSchoolId == null) {
            throw new BusinessException("School context is required for this operation.");
        }
        if (schoolId == null || sessionId == null) {
            log.warn("Setup validation failed: missing school/session context schoolId={} sessionId={}", schoolId, sessionId);
            throw new BusinessException("Session ID is required for operation.");
        }
        if (!tenantSchoolId.equals(schoolId)) {
            log.warn("Setup validation failed: tenant mismatch tenantSchoolId={} schoolId={}", tenantSchoolId, schoolId);
            throw new BusinessException("Invalid school context for current tenant.");
        }

        validateSessionBelongsToTenant(sessionId);

        long classCount = schoolClassRepository.countBySchoolIdAndSessionId(schoolId, sessionId);
        if (classCount == 0) {
            log.warn("Setup validation failed: no classes for schoolId={} sessionId={}", schoolId, sessionId);
            throw new BusinessException("At least one class must be created before accessing this module.");
        }
    }

    @Transactional(readOnly = true)
    public void validateSessionBelongsToTenant(Long sessionId) {
        Long tenantSchoolId = TenantContext.getSchoolId();
        if (tenantSchoolId == null || sessionId == null) {
            throw new BusinessException("Invalid session context for current tenant.");
        }
        boolean belongsToTenant = academicSessionRepository.existsByIdAndSchoolId(sessionId, tenantSchoolId);
        if (!belongsToTenant) {
            throw new BusinessException("Session does not belong to current tenant.");
        }
    }
}
