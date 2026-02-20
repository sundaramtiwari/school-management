package com.school.backend.core.teacher.service;

import com.school.backend.common.exception.BusinessException;
import com.school.backend.core.classsubject.entity.SchoolClass;
import com.school.backend.core.classsubject.entity.Subject;
import com.school.backend.core.classsubject.repository.ClassSubjectRepository;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import com.school.backend.core.classsubject.repository.SubjectRepository;
import com.school.backend.core.teacher.dto.TeacherAssignmentListItemDto;
import com.school.backend.core.teacher.entity.Teacher;
import com.school.backend.core.teacher.entity.TeacherAssignment;
import com.school.backend.core.teacher.repository.TeacherAssignmentRepository;
import com.school.backend.core.teacher.repository.TeacherRepository;
import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.repository.AcademicSessionRepository;
import com.school.backend.user.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherAssignmentService {

    private final TeacherAssignmentRepository repository;
    private final TeacherRepository teacherRepository;
    private final AcademicSessionRepository sessionRepository;
    private final SchoolClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final ClassSubjectRepository classSubjectRepository;

    @Transactional
    public TeacherAssignment assignTeacher(Long teacherId, Long sessionId, Long classId, Long subjectId) {
        Long schoolId = SecurityUtil.schoolId();

        // 1. Validation: Entity existence and same-school integrity
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new BusinessException("Teacher not found"));
        validateSchool(teacher.getSchoolId(), schoolId, "Teacher");

        AcademicSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException("Session not found"));
        validateSchool(session.getSchoolId(), schoolId, "Session");

        SchoolClass schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("Class not found"));
        validateSchool(schoolClass.getSchoolId(), schoolId, "Class");

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new BusinessException("Subject not found"));
        validateSchool(subject.getSchoolId(), schoolId, "Subject");

        // 2. Logic: Class must belong to session
        if (!schoolClass.getSessionId().equals(sessionId)) {
            throw new BusinessException("Class does not belong to the selected session");
        }

        // 3. Logic: Subject must belong to class (via ClassSubject mapping)
        boolean subjectExistsInClass = classSubjectRepository.existsBySchoolClassIdAndSubjectId(classId, subjectId);
        if (!subjectExistsInClass) {
            throw new BusinessException("Subject is not mapped to this class");
        }

        // 4. Logic: Prevent duplicate active assignment
        if (repository.existsByTeacherIdAndSessionIdAndSchoolClassIdAndSubjectIdAndActiveTrue(teacherId, sessionId,
                classId, subjectId)) {
            throw new BusinessException("Active assignment already exists for this combination");
        }

        TeacherAssignment assignment = TeacherAssignment.builder()
                .schoolId(schoolId)
                .teacher(teacher)
                .session(session)
                .schoolClass(schoolClass)
                .subject(subject)
                .active(true)
                .assignedAt(LocalDateTime.now())
                .assignedBy(SecurityUtil.current().getUserId())
                .build();

        return repository.save(assignment);
    }

    @Transactional
    public void deactivateAssignment(Long id) {
        TeacherAssignment assignment = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Assignment not found"));

        validateSchool(assignment.getSchoolId(), SecurityUtil.schoolId(), "Assignment");

        repository.delete(assignment);
    }

    @Transactional(readOnly = true)
    public List<TeacherAssignment> getTeacherAssignments(Long teacherId, Long sessionId) {
        return repository.findByTeacherIdAndSessionIdAndActiveTrue(teacherId, sessionId);
    }

    @Transactional(readOnly = true)
    public List<TeacherAssignmentListItemDto> listBySession(Long sessionId) {
        Long schoolId = SecurityUtil.schoolId();
        List<TeacherAssignment> assignments = repository.findBySessionIdAndSchoolIdAndActiveTrue(sessionId, schoolId);
        return assignments.stream()
                .map(this::toListItemDto)
                .collect(Collectors.toList());
    }

    private TeacherAssignmentListItemDto toListItemDto(TeacherAssignment a) {
        TeacherAssignmentListItemDto dto = new TeacherAssignmentListItemDto();
        dto.setId(a.getId());
        dto.setTeacherName(a.getTeacher() != null && a.getTeacher().getUser() != null
                ? a.getTeacher().getUser().getFullName()
                : "");
        dto.setClassName(a.getSchoolClass() != null
                ? a.getSchoolClass().getName()
                        + (a.getSchoolClass().getSection() != null ? " " + a.getSchoolClass().getSection() : "")
                : "");
        dto.setSubjectName(a.getSubject() != null ? a.getSubject().getName() : "");
        dto.setSessionName(a.getSession() != null ? a.getSession().getName() : "");
        dto.setStatus("Active");
        return dto;
    }

    /**
     * Optional hook for future session rollover.
     */
    @Transactional
    public void copyAssignments(Long fromSessionId, Long toSessionId) {
        // This is a placeholder for future session copy workflow.
        // Implementation would involve mapping old classes/subjects to new ones.
        // Since SchoolClass is session-dependent, IDs will change.
    }

    @Transactional
    public void deactivateAllForTeacher(Long teacherId) {
        List<TeacherAssignment> activeAssignments = repository.findByTeacherIdAndActiveTrue(teacherId);
        activeAssignments.forEach(a -> a.setActive(false));
        repository.saveAll(activeAssignments);
    }

    @Transactional(readOnly = true)
    public List<SchoolClass> getMyAssignedClasses(Long sessionId) {
        Long schoolId = SecurityUtil.schoolId();
        Long userId = SecurityUtil.userId();
        Teacher teacher = teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Teacher profile not found for current user"));
        return repository.findDistinctClassesByTeacherAndSession(teacher.getId(), sessionId, schoolId);
    }

    @Transactional(readOnly = true)
    public List<Subject> getMyAssignedSubjects(Long sessionId, Long classId) {
        Long schoolId = SecurityUtil.schoolId();
        Long userId = SecurityUtil.userId();
        Teacher teacher = teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Teacher profile not found for current user"));
        return repository.findDistinctSubjectsByTeacherSessionAndClass(teacher.getId(), sessionId, classId, schoolId);
    }

    private void validateSchool(Long entitySchoolId, Long currentSchoolId, String entityName) {
        if (!entitySchoolId.equals(currentSchoolId)) {
            throw new BusinessException(entityName + " does not belong to this school");
        }
    }
}
