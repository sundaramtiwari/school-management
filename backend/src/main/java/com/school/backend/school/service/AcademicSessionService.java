package com.school.backend.school.service;

import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.repository.AcademicSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AcademicSessionService {

    private final AcademicSessionRepository repository;

    public List<AcademicSession> getSessions(Long schoolId) {
        return repository.findBySchoolIdAndActiveTrue(schoolId);
    }

    @Transactional
    public AcademicSession createSession(AcademicSession session) {
        if (session.isCurrent()) {
            resetCurrentStatus(session.getSchoolId());
        }
        return repository.save(session);
    }

    @Transactional
    public AcademicSession updateSession(Long id, AcademicSession details) {
        AcademicSession session = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        session.setName(details.getName());
        session.setStartDate(details.getStartDate());
        session.setEndDate(details.getEndDate());
        session.setActive(details.isActive());

        if (details.isCurrent() && !session.isCurrent()) {
            resetCurrentStatus(session.getSchoolId());
            session.setCurrent(true);
        }

        return repository.save(session);
    }

    private void resetCurrentStatus(Long schoolId) {
        List<AcademicSession> sessions = repository.findBySchoolId(schoolId);
        sessions.forEach(s -> s.setCurrent(false));
        repository.saveAll(sessions);
    }
}
