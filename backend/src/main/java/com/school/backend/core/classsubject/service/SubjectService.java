package com.school.backend.core.classsubject.service;

import com.school.backend.common.exception.BusinessException;
import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.core.classsubject.dto.SubjectDto;
import com.school.backend.core.classsubject.entity.Subject;
import com.school.backend.core.classsubject.mapper.SubjectMapper;
import com.school.backend.core.classsubject.repository.SubjectRepository;
import com.school.backend.user.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository repository;
    private final SubjectMapper mapper;

    private static @NonNull Subject getCurrent(SubjectDto dto, Subject current) {
        if (current.isActive()) {
            throw new BusinessException(
                    "Subject already exists in this school: " + dto.getName());
        }
        current.setActive(true);
        current.setCode(dto.getCode());
        current.setType(dto.getType());
        current.setMaxMarks(dto.getMaxMarks());
        current.setMinMarks(dto.getMinMarks());
        current.setRemarks(dto.getRemarks());
        return current;
    }

    public SubjectDto create(SubjectDto dto) {
        Long schoolId = SecurityUtil.schoolId();
        dto.setSchoolId(schoolId);

        var existing = repository.findByNameIgnoreCaseAndSchoolId(dto.getName(), schoolId);
        if (existing.isPresent()) {
            Subject current = getCurrent(dto, existing.get());
            return mapper.toDto(repository.save(current));
        }
        Subject entity = mapper.toEntity(dto);
        entity.setSchoolId(schoolId);
        entity.setActive(true); // Default to active
        return mapper.toDto(repository.save(entity));
    }

    public SubjectDto update(Long id, SubjectDto dto) {
        Subject existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + id));

        if (!existing.getSchoolId().equals(SecurityUtil.schoolId())) {
            throw new com.school.backend.common.exception.BusinessException(
                    "Access Denied: Subject belongs to another school.");
        }

        existing.setName(dto.getName());
        existing.setCode(dto.getCode());
        existing.setType(dto.getType());
        existing.setMaxMarks(dto.getMaxMarks());
        existing.setMinMarks(dto.getMinMarks());
        existing.setActive(dto.isActive());
        existing.setRemarks(dto.getRemarks());

        return mapper.toDto(repository.save(existing));
    }

    public SubjectDto getById(Long id) {
        Subject subject = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + id));
        if (!subject.isActive()) {
            throw new ResourceNotFoundException("Subject not found: " + id);
        }

        // Optional: strict read validation
        // if (!subject.getSchoolId().equals(SecurityUtil.schoolId())) ...

        return mapper.toDto(subject);
    }

    public Page<SubjectDto> getBySchool(Long schoolId, Boolean active, Pageable pageable) {
        if (Boolean.TRUE.equals(active)) {
            // Fetch only active subjects
            return repository.findBySchoolIdAndActiveTrue(schoolId, pageable).map(mapper::toDto);
        } else {
            // Fetch all subjects (active + inactive) if active is null or false
            // Note: If strict "inactive only" is required, we would need a new repository
            // method.
            // Current requirement (inferred): "Show Inactive" checkbox OFF -> Active Only.
            // ON -> All.
            return repository.findBySchoolId(schoolId, pageable).map(mapper::toDto);
        }
    }

    public Page<SubjectDto> getAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }
}
