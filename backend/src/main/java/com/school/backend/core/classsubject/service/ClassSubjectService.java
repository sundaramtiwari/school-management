package com.school.backend.core.classsubject.service;

import com.school.backend.common.exception.BusinessException;
import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.common.tenant.SessionContext;
import com.school.backend.core.classsubject.dto.ClassSubjectAssignmentDto;
import com.school.backend.core.classsubject.dto.ClassSubjectDto;
import com.school.backend.core.classsubject.entity.ClassSubject;
import com.school.backend.core.classsubject.entity.SchoolClass;
import com.school.backend.core.classsubject.entity.Subject;
import com.school.backend.core.classsubject.mapper.ClassSubjectMapper;
import com.school.backend.core.classsubject.repository.ClassSubjectRepository;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import com.school.backend.core.classsubject.repository.SubjectRepository;
import com.school.backend.core.teacher.entity.Teacher;
import com.school.backend.core.teacher.repository.TeacherRepository;
import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.repository.AcademicSessionRepository;
import com.school.backend.user.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassSubjectService {

    private final ClassSubjectRepository repository;
    private final SchoolClassRepository classRepo;
    private final SubjectRepository subjectRepo;
    private final TeacherRepository teacherRepo;
    private final AcademicSessionRepository sessionRepo;
    private final ClassSubjectMapper mapper;

    public ClassSubjectDto create(ClassSubjectDto dto) {

        Long schoolId = SecurityUtil.schoolId();

        if (repository.existsBySchoolClassIdAndSubjectId(dto.getClassId(), dto.getSubjectId())) {
            throw new BusinessException("Subject already assigned to class.");
        }

        SchoolClass sc = classRepo.findById(dto.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + dto.getClassId()));

        if (!sc.getSchool().getId().equals(schoolId)) {
            throw new BusinessException("Class belongs to another school.");
        }

        Subject s = subjectRepo.findById(dto.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + dto.getSubjectId()));

        if (!s.getSchoolId().equals(schoolId)) {
            throw new BusinessException("Subject belongs to another school.");
        }

        if (!s.isActive()) {
            throw new BusinessException("Cannot assign inactive subject to class.");
        }

        ClassSubject entity = mapper.toEntity(dto);
        entity.setSchoolId(schoolId);
        entity.setSchoolClass(sc);
        entity.setSubject(s);

        if (dto.getTeacherId() != null) {
            Teacher t = new Teacher();
            t.setId(dto.getTeacherId());
            t.setSchoolId(schoolId);
            entity.setTeacher(t);
        }

        return mapper.toDto(repository.save(entity));
    }

    public ClassSubjectDto getById(Long id) {
        ClassSubject cs = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClassSubject not found: " + id));
        return mapper.toDto(cs);
    }

    public Page<ClassSubjectDto> getByClass(Long classId, Pageable pageable) {
        return repository.findBySchoolClassId(classId, pageable).map(mapper::toDto);
    }

    public Page<ClassSubjectDto> getBySchool(Long schoolId, Pageable pageable) {
        return repository.findBySchoolId(schoolId, pageable).map(mapper::toDto);
    }

    public Page<ClassSubjectDto> getAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Assignment not found: " + id);
        }
        repository.deleteById(id);
    }

    @Transactional
    public ClassSubjectAssignmentDto assignTeacher(Long teacherId, Long sessionId, Long classId, Long subjectId) {
        Long schoolId = SecurityUtil.schoolId();
        Long effectiveSessionId = validateAndGetSessionId(sessionId, schoolId);

        ClassSubject cs = repository.findBySchoolClassIdAndSubjectId(classId, subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Class-Subject mapping not found"));

        if (!cs.getSchoolId().equals(schoolId)) {
            throw new BusinessException("Access Denied");
        }

        if (!cs.getSchoolClass().getSessionId().equals(effectiveSessionId)) {
            throw new BusinessException("Class does not belong to the selected session");
        }

        Teacher t = teacherRepo.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));

        cs.setTeacher(t);
        return toAssignmentDto(repository.save(cs));
    }

    @Transactional(readOnly = true)
    public List<ClassSubjectAssignmentDto> listAssignments(Long teacherId) {
        Long schoolId = SecurityUtil.schoolId();
        Long effectiveSessionId = validateAndGetSessionId(SessionContext.getSessionId(), schoolId);

        List<ClassSubject> assignments;
        if (teacherId != null) {
            assignments = repository.findByTeacherIdAndSessionId(teacherId, effectiveSessionId, schoolId);
        } else {
            assignments = repository.findAssignmentsBySession(effectiveSessionId, schoolId);
        }

        return assignments.stream()
                .map(this::toAssignmentDto)
                .collect(Collectors.toList());
    }

    private Long validateAndGetSessionId(Long sessionId, Long schoolId) {
        Long effectiveId = (sessionId != null) ? sessionId : SessionContext.getSessionId();

        if (effectiveId == null) {
            throw new BusinessException("No active session selected");
        }

        if (!sessionRepo.existsByIdAndSchoolId(effectiveId, schoolId)) {
            throw new ResourceNotFoundException("Session not found or access denied: " + effectiveId);
        }

        return effectiveId;
    }

    @Transactional
    public void deactivateAssignment(Long id) {
        ClassSubject cs = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        if (!cs.getSchoolId().equals(SecurityUtil.schoolId())) {
            throw new BusinessException("Access Denied");
        }
        cs.setTeacher(null);
        repository.save(cs);
    }

    @Transactional
    public void deactivateAllForTeacherInSession(Long teacherId, Long sessionId) {
        Long schoolId = SecurityUtil.schoolId();
        Long effectiveSessionId = validateAndGetSessionId(sessionId, schoolId);
        List<ClassSubject> assignments = repository.findByTeacherIdAndSchoolClassSessionIdAndSchoolId(
                teacherId, effectiveSessionId, schoolId);
        assignments.forEach(a -> a.setTeacher(null));
        repository.saveAll(assignments);
    }

    @Transactional(readOnly = true)
    public List<SchoolClass> getMyAssignedClasses() {
        Long schoolId = SecurityUtil.schoolId();
        Long effectiveSessionId = validateAndGetSessionId(SessionContext.getSessionId(), schoolId);
        Long userId = SecurityUtil.userId();
        Teacher teacher = teacherRepo.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Teacher profile not found for current user"));
        return repository.findDistinctClassesByTeacherAndSession(teacher.getId(), effectiveSessionId, schoolId);
    }

    @Transactional(readOnly = true)
    public List<Subject> getMyAssignedSubjects(Long classId) {
        Long schoolId = SecurityUtil.schoolId();
        Long userId = SecurityUtil.userId();
        Long effectiveSessionId = validateAndGetSessionId(SessionContext.getSessionId(), schoolId);
        Teacher teacher = teacherRepo.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Teacher profile not found for current user"));
        return repository.findDistinctSubjectsByTeacherSessionAndClass(teacher.getId(), effectiveSessionId, classId,
                schoolId);
    }

    private ClassSubjectAssignmentDto toAssignmentDto(ClassSubject cs) {
        return ClassSubjectAssignmentDto.builder()
                .id(cs.getId())
                .teacherId(cs.getTeacher() != null ? cs.getTeacher().getId() : null)
                .teacherName(cs.getTeacher() != null && cs.getTeacher().getUser() != null
                        ? cs.getTeacher().getUser().getFullName()
                        : "")
                .classId(cs.getSchoolClass().getId())
                .className(cs.getSchoolClass().getName()
                        + (cs.getSchoolClass().getSection() != null ? " " + cs.getSchoolClass().getSection() : ""))
                .subjectId(cs.getSubject().getId())
                .subjectName(cs.getSubject().getName())
                .sessionId(cs.getSchoolClass().getSessionId())
                .sessionName(sessionRepo.findById(cs.getSchoolClass().getSessionId())
                        .map(AcademicSession::getName)
                        .orElse(""))
                .build();
    }
}
