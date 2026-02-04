package com.school.backend.core.classsubject.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.core.classsubject.dto.SchoolClassDto;
import com.school.backend.core.classsubject.entity.SchoolClass;
import com.school.backend.core.classsubject.mapper.SchoolClassMapper;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import com.school.backend.core.teacher.entity.Teacher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SchoolClassService {

    private final SchoolClassRepository repository;
    private final SchoolClassMapper mapper;

    public SchoolClassDto create(SchoolClassDto dto) {
        Long schoolId = TenantContext.getSchoolId();
        System.err.println("SchoolID: " + schoolId);
        // duplication check using repository method
        if (repository.existsByNameAndSectionAndSessionAndSchoolId(dto.getName(),
                dto.getSection(), dto.getSession(), schoolId)) {
            throw new IllegalArgumentException("Class with same name/section/session already exists for this school");
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
        SchoolClass existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SchoolClass not found: " + id));

        existing.setName(dto.getName());
        existing.setSection(dto.getSection());
        existing.setSession(dto.getSession());
        existing.setCapacity(dto.getCapacity());
        existing.setRemarks(dto.getRemarks());
        existing.setActive(dto.isActive());

        // update teacher relation
        if (dto.getClassTeacherId() != null) {
            Teacher t = new Teacher();
            t.setId(dto.getClassTeacherId());
            t.setSchoolId(TenantContext.getSchoolId());
            existing.setClassTeacher(t);
        } else {
            existing.setClassTeacher(null);
        }

        return mapper.toDto(repository.save(existing));
    }

    public SchoolClassDto getById(Long id) {
        SchoolClass entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SchoolClass not found: " + id));
        return mapper.toDto(entity);
    }

    public SchoolClassDto getByIdAndSchool(Long id, Long schoolId) {
        SchoolClass entity = repository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("SchoolClass not found for given school: " + id));
        return mapper.toDto(entity);
    }

    public Page<SchoolClassDto> getBySchool(Long schoolId, Pageable pageable) {
        return repository.findBySchoolId(schoolId, pageable).map(mapper::toDto);
    }

    public Page<SchoolClassDto> getBySchoolAndSession(Long schoolId, String session, Pageable pageable) {
        return repository.findBySchoolIdAndSession(schoolId, session, pageable).map(mapper::toDto);
    }

    public Page<SchoolClassDto> getAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("SchoolClass not found: " + id);
        }
        repository.deleteById(id);
    }
}
