package com.school.backend.school.service;

import com.school.backend.common.exception.InvalidOperationException;
import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.entity.School;
import com.school.backend.school.repository.AcademicSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AcademicSessionService {

    private final AcademicSessionRepository repository;
    private final com.school.backend.school.repository.SchoolRepository schoolRepository;

    public List<AcademicSession> getSessions(Long schoolId) {
        return repository.findBySchoolIdAndActiveTrue(schoolId);
    }

    @Transactional
    public AcademicSession createSession(AcademicSession session) {
        if (repository.existsBySchoolIdAndName(session.getSchoolId(), session.getName())) {
            throw new InvalidOperationException(
                    "Session with name '" + session.getName() + "' already exists for this school");
        }
        AcademicSession saved = repository.save(session);

        School school = schoolRepository.findById(session.getSchoolId())
                .orElseThrow(() -> new RuntimeException("School not found"));

        if (school.getCurrentSessionId() == null) {
            school.setCurrentSessionId(saved.getId());
            schoolRepository.save(school);
        }

        return saved;
    }

    @Transactional
    public AcademicSession updateSession(Long id, AcademicSession updatedSession) {
        AcademicSession session = repository.findById(id)
                .orElseThrow(() -> new InvalidOperationException("Session not found"));

        // Only check if name is changing
        if (!session.getName().equals(updatedSession.getName())) {
            if (repository.existsBySchoolIdAndName(session.getSchoolId(), updatedSession.getName())) {
                throw new InvalidOperationException(
                        "Session with name '" + updatedSession.getName() + "' already exists for this school");
            }
        }

        session.setName(updatedSession.getName());
        session.setActive(updatedSession.isActive());
        // No more start/end date or current flag updates here

        return repository.save(session);
    }

    @Transactional
    public void setCurrentSession(Long schoolId, Long sessionId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new RuntimeException("School not found"));

        // Verify session belongs to school
        AcademicSession session = repository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!session.getSchoolId().equals(schoolId)) {
            throw new RuntimeException("Session does not belong to this school");
        }

        school.setCurrentSessionId(sessionId);
        schoolRepository.save(school);
    }

    public java.util.Optional<AcademicSession> getCurrentSession(Long schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new RuntimeException("School not found"));

        if (school.getCurrentSessionId() == null) {
            return java.util.Optional.empty();
        }

        return repository.findById(school.getCurrentSessionId());
    }
}
