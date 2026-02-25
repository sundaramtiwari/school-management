package com.school.backend.core.classsubject.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.common.tenant.SessionResolver;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.core.classsubject.dto.SchoolClassDto;
import com.school.backend.core.classsubject.entity.SchoolClass;
import com.school.backend.core.classsubject.mapper.SchoolClassMapper;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import com.school.backend.core.teacher.entity.Teacher;
import com.school.backend.core.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SchoolClassService {

    private final SchoolClassRepository repository;
    private final SchoolClassMapper mapper;
    private final TeacherRepository teacherRepository;
    private final SessionResolver sessionResolver;
    private final com.school.backend.core.student.repository.StudentEnrollmentRepository enrollmentRepo;

    public SchoolClassDto create(SchoolClassDto dto) {
        Long schoolId = TenantContext.getSchoolId();
        var existing = repository.findByNameAndSectionAndSessionIdAndSchoolId(
                dto.getName(), dto.getSection(), dto.getSessionId(), schoolId);
        if (existing.isPresent()) {
            SchoolClass current = existing.get();
            if (current.isActive()) {
                throw new com.school.backend.common.exception.BusinessException(
                        "Class with same name/section/session already exists for this school");
            }
            current.setActive(true);
            current.setCapacity(dto.getCapacity());
            current.setRemarks(dto.getRemarks());
            if (dto.getClassTeacherId() != null) {
                Teacher t = new Teacher();
                t.setId(dto.getClassTeacherId());
                t.setSchoolId(schoolId);
                current.setClassTeacher(t);
            } else {
                current.setClassTeacher(null);
            }
            return mapper.toDto(repository.save(current));
        }

        SchoolClass entity = mapper.toEntity(dto);

        // assign school stub by id to avoid extra DB hit
        entity.setSchoolId(schoolId);

        // assign teacher stub if provided
        if (dto.getClassTeacherId() != null) {
            Teacher t = new Teacher();
            t.setId(dto.getClassTeacherId());
            t.setSchoolId(schoolId);
            entity.setClassTeacher(t);
        }

        return mapper.toDto(repository.save(entity));
    }

    public SchoolClassDto update(Long id, SchoolClassDto dto) {
        Long schoolId = TenantContext.getSchoolId();
        SchoolClass existing = repository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("SchoolClass not found: " + id));

        existing.setName(dto.getName());
        existing.setSection(dto.getSection());
        existing.setSessionId(dto.getSessionId());
        existing.setCapacity(dto.getCapacity());
        existing.setRemarks(dto.getRemarks());
        existing.setActive(dto.isActive());

        // update teacher relation
        if (dto.getClassTeacherId() != null) {
            Teacher t = new Teacher();
            t.setId(dto.getClassTeacherId());
            t.setSchoolId(schoolId);
            existing.setClassTeacher(t);
        } else {
            existing.setClassTeacher(null);
        }

        return mapper.toDto(repository.save(existing));
    }

    public SchoolClassDto getById(Long id) {
        Long schoolId = TenantContext.getSchoolId();
        SchoolClass entity = repository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("SchoolClass not found: " + id));
        return mapper.toDto(entity);
    }

    public SchoolClassDto getByIdAndSchool(Long id, Long schoolId) {
        SchoolClass entity = repository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("SchoolClass not found for given school: " + id));
        return mapper.toDto(entity);
    }

    public Page<SchoolClassDto> getBySchool(Long schoolId, Pageable pageable, boolean includeInactive) {
        Long sessionId = sessionResolver.resolveForCurrentSchool();
        if (includeInactive) {
            return repository.findBySchoolIdAndSessionId(schoolId, sessionId, pageable).map(mapper::toDto);
        } else {
            return repository.findBySchoolIdAndSessionIdAndActiveTrue(schoolId, sessionId, pageable).map(mapper::toDto);
        }
    }

    public Page<SchoolClassDto> getMyClasses(Long userId, Long schoolId, Pageable pageable, boolean includeInactive) {
        // Find teacher profile for user
        Teacher teacher = teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found for user: " + userId));

        if (includeInactive) {
            return repository.findByClassTeacherIdAndSchoolId(teacher.getId(), schoolId, pageable)
                    .map(mapper::toDto);
        } else {
            return repository.findByClassTeacherIdAndSchoolIdAndActiveTrue(teacher.getId(), schoolId, pageable)
                    .map(mapper::toDto);
        }
    }

    public Page<SchoolClassDto> getBySchoolAndSession(Long schoolId, Long sessionId, Pageable pageable,
            boolean includeInactive) {
        if (includeInactive) {
            return repository.findBySchoolIdAndSessionId(schoolId, sessionId, pageable).map(mapper::toDto);
        } else {
            return repository.findBySchoolIdAndSessionIdAndActiveTrue(schoolId, sessionId, pageable).map(mapper::toDto);
        }
    }

    public Page<SchoolClassDto> getAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @org.springframework.transaction.annotation.Transactional
    public SchoolClassDto toggleActive(Long id) {
        Long schoolId = TenantContext.getSchoolId();
        SchoolClass schoolClass = repository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("SchoolClass not found: " + id));

        if (schoolClass.isActive()) {
            // Deactivating: check for active enrollments
            Long sessionId = com.school.backend.common.tenant.SessionContext.getSessionId();
            if (sessionId != null && schoolClass.getSessionId().equals(sessionId)) {
                long activeEnrollments = enrollmentRepo.countByClassIdAndSessionIdAndActiveTrue(id, sessionId);
                if (activeEnrollments > 0) {
                    throw new com.school.backend.common.exception.BusinessException(
                            "Cannot deactivate class with active student enrollments in the current session.");
                }
            }
        }

        schoolClass.setActive(!schoolClass.isActive());
        return mapper.toDto(repository.save(schoolClass));
    }

    public void delete(Long id) {
        Long schoolId = TenantContext.getSchoolId();
        SchoolClass schoolClass = repository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("SchoolClass not found: " + id));

        // Deactivation check logic repeated for delete (soft-delete)
        if (schoolClass.isActive()) {
            Long sessionId = com.school.backend.common.tenant.SessionContext.getSessionId();
            if (sessionId != null && schoolClass.getSessionId().equals(sessionId)) {
                long activeEnrollments = enrollmentRepo.countByClassIdAndSessionIdAndActiveTrue(id, sessionId);
                if (activeEnrollments > 0) {
                    throw new com.school.backend.common.exception.BusinessException(
                            "Cannot deactivate/delete class with active student enrollments in the current session.");
                }
            }
        }

        schoolClass.setActive(false);
        repository.save(schoolClass);
    }

    public long getClassCount() {
        Long schoolId = TenantContext.getSchoolId();
        Long sessionId = sessionResolver.resolveForCurrentSchool();
        return repository.countBySchoolIdAndSessionIdAndActiveTrue(schoolId, sessionId);
    }
}
