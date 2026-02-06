package com.school.backend.core.classsubject.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.core.classsubject.dto.SubjectDto;
import com.school.backend.core.classsubject.entity.Subject;
import com.school.backend.core.classsubject.mapper.SubjectMapper;
import com.school.backend.core.classsubject.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository repository;
    private final SubjectMapper mapper;

    public SubjectDto create(SubjectDto dto) {
        if (repository.existsByNameIgnoreCaseAndSchoolId(dto.getName(), dto.getSchoolId())) {
            throw new IllegalArgumentException("Subject already exists: " + dto.getName());
        }
        Subject entity = mapper.toEntity(dto);
        return mapper.toDto(repository.save(entity));
    }

    public SubjectDto update(Long id, SubjectDto dto) {
        Subject existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + id));

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
        return mapper.toDto(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + id)));
    }

    public Page<SubjectDto> getBySchool(Long schoolId, Pageable pageable) {
        return repository.findBySchoolIdAndActiveTrue(schoolId, pageable).map(mapper::toDto);
    }

    public Page<SubjectDto> getAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Subject not found: " + id);
        }
        repository.deleteById(id);
    }
}
