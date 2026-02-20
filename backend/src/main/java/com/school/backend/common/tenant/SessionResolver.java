package com.school.backend.common.tenant;

import com.school.backend.common.exception.InvalidOperationException;
import com.school.backend.school.entity.School;
import com.school.backend.school.repository.AcademicSessionRepository;
import com.school.backend.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionResolver {

    private final SchoolRepository schoolRepository;
    private final AcademicSessionRepository sessionRepository;

    public Long resolveForCurrentSchool() {
        Long schoolId = TenantContext.getSchoolId();
        if (schoolId == null) {
            throw new InvalidOperationException("School context is missing in request");
        }

        Long requestedSessionId = SessionContext.getSessionId();
        if (requestedSessionId != null) {
            var session = sessionRepository.findById(requestedSessionId)
                    .orElseThrow(() -> new InvalidOperationException("Session not found: " + requestedSessionId));

            if (!schoolId.equals(session.getSchoolId())) {
                throw new InvalidOperationException("Session does not belong to this school");
            }
            return requestedSessionId;
        }

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new InvalidOperationException("School not found: " + schoolId));

        if (school.getCurrentSessionId() == null) {
            throw new InvalidOperationException("No current session is configured for this school");
        }

        return school.getCurrentSessionId();
    }

    public Long validateForCurrentSchool(Long sessionId) {
        Long schoolId = TenantContext.getSchoolId();
        if (schoolId == null) {
            throw new InvalidOperationException("School context is missing in request");
        }
        if (sessionId == null) {
            throw new InvalidOperationException("Session context is missing in request");
        }
        if (!sessionRepository.existsByIdAndSchoolId(sessionId, schoolId)) {
            throw new InvalidOperationException("Session does not belong to this school");
        }
        return sessionId;
    }
}
