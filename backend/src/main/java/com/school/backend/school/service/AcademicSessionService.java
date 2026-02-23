package com.school.backend.school.service;

import com.school.backend.common.exception.InvalidOperationException;
import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.entity.School;
import com.school.backend.school.repository.AcademicSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AcademicSessionService {

    private final AcademicSessionRepository repository;
    private final com.school.backend.school.repository.SchoolRepository schoolRepository;

    public List<AcademicSession> getSessions(Long schoolId) {
        log.debug("Fetching all sessions for schoolId={}", schoolId);
        return repository.findBySchoolId(schoolId);
    }

    @Transactional
    public AcademicSession createSession(AcademicSession session) {
        log.info("Creating academic session name={} for schoolId={}", session.getName(), session.getSchoolId());
        validateDates(session.getStartDate(), session.getEndDate());
        if (repository.existsBySchoolIdAndName(session.getSchoolId(), session.getName())) {
            log.warn("Session creation blocked: duplicate name={} for schoolId={}", session.getName(), session.getSchoolId());
            throw new InvalidOperationException(
                    "Session with name '" + session.getName() + "' already exists for this school");
        }
        AcademicSession saved = repository.save(session);

        School school = schoolRepository.findById(session.getSchoolId())
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        if (saved.isActive()) {
            enforceSingleActiveSession(saved.getSchoolId(), saved.getId());
            school.setCurrentSessionId(saved.getId());
            schoolRepository.save(school);
        } else if (school.getCurrentSessionId() == null) {
            school.setCurrentSessionId(saved.getId());
            schoolRepository.save(school);
            log.info("Initialized currentSessionId={} for schoolId={}", saved.getId(), school.getId());
        }

        return saved;
    }

    @Transactional
    public AcademicSession updateSession(Long id, AcademicSession updatedSession) {
        log.info("Updating academic session id={} (name={}, active={})", id, updatedSession.getName(), updatedSession.isActive());
        AcademicSession session = repository.findById(id)
                .orElseThrow(() -> new InvalidOperationException("Session not found"));

        // Only check if name is changing
        if (!session.getName().equals(updatedSession.getName())) {
            if (repository.existsBySchoolIdAndName(session.getSchoolId(), updatedSession.getName())) {
                log.warn("Session rename blocked: duplicate name={} for schoolId={}", updatedSession.getName(), session.getSchoolId());
                throw new InvalidOperationException(
                        "Session with name '" + updatedSession.getName() + "' already exists for this school");
            }
        }

        session.setName(updatedSession.getName());
        session.setActive(updatedSession.isActive());
        // startDate/endDate are immutable after creation.
        AcademicSession saved = repository.save(session);
        School school = schoolRepository.findById(saved.getSchoolId())
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        if (saved.isActive()) {
            enforceSingleActiveSession(saved.getSchoolId(), saved.getId());
            school.setCurrentSessionId(saved.getId());
            schoolRepository.save(school);
        } else if (school.getCurrentSessionId() != null && school.getCurrentSessionId().equals(saved.getId())) {
            repository.findFirstBySchoolIdAndActiveTrueOrderByStartDateDesc(saved.getSchoolId())
                    .ifPresentOrElse(
                            active -> school.setCurrentSessionId(active.getId()),
                            () -> school.setCurrentSessionId(null));
            schoolRepository.save(school);
        }

        return saved;
    }

    @Transactional
    public void setCurrentSession(Long schoolId, Long sessionId) {
        log.info("Setting current session schoolId={} sessionId={}", schoolId, sessionId);
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        // Verify session belongs to school
        AcademicSession session = repository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        if (!session.getSchoolId().equals(schoolId)) {
            log.warn("Rejected current session change: sessionId={} belongsToSchoolId={}, attemptedSchoolId={}",
                    sessionId, session.getSchoolId(), schoolId);
            throw new InvalidOperationException("Session does not belong to this school");
        }

        if (!session.isActive()) {
            session.setActive(true);
            repository.save(session);
        }
        enforceSingleActiveSession(schoolId, sessionId);
        school.setCurrentSessionId(sessionId);
        schoolRepository.save(school);
    }

    public java.util.Optional<AcademicSession> getCurrentSession(Long schoolId) {
        log.debug("Fetching current session for schoolId={}", schoolId);
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        if (school.getCurrentSessionId() == null) {
            return java.util.Optional.empty();
        }

        return repository.findById(school.getCurrentSessionId());
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new InvalidOperationException("Session startDate and endDate are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new InvalidOperationException("Session endDate must be on or after startDate");
        }
    }

    private void enforceSingleActiveSession(Long schoolId, Long activeSessionId) {
        repository.deactivateOtherActiveSessions(schoolId, activeSessionId);
    }
}
