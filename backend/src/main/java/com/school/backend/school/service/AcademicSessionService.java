package com.school.backend.school.service;

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
        // No more current flag reset or date validations
        return repository.save(session);
    }

    @Transactional
    public AcademicSession updateSession(Long id, AcademicSession details) {
        AcademicSession session = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        session.setName(details.getName());
        session.setActive(details.isActive());
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

    public AcademicSession getCurrentSession(Long schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new RuntimeException("School not found"));

        if (school.getCurrentSessionId() == null) {
            return null; // or throw exception if current session is required
        }

        return repository.findById(school.getCurrentSessionId())
                .orElse(null);
    }
}
