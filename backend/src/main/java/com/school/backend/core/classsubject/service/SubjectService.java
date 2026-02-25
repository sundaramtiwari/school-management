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

    private String toTitleCase(String input) {
        if (input == null || input.isBlank())
            return "";
        String[] words = input.trim().toLowerCase().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }

    private static @NonNull Subject getCurrent(SubjectDto dto, Subject current, String normalizedName) {
        if (current.isActive()) {
            throw new BusinessException(
                    "Subject already exists in this school: " + normalizedName);
        }
        current.setActive(true);
        current.setName(normalizedName);
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

        String normalizedName = toTitleCase(dto.getName());
        var existing = repository.findByNameIgnoreCaseAndSchoolId(normalizedName, schoolId);
        if (existing.isPresent()) {
            Subject entity = existing.get();
            if (entity.isActive()) {
                throw new BusinessException("Subject already exists in this school: " + normalizedName);
            } else {
                throw new BusinessException("Subject exists but inactive. Please reactivate instead.");
            }
        }
        Subject entity = mapper.toEntity(dto);
        entity.setName(normalizedName);
        entity.setSchoolId(schoolId);
        entity.setActive(true); // Default to active
        return mapper.toDto(repository.save(entity));
    }

    public SubjectDto update(Long id, SubjectDto dto) {
        Subject existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + id));

        Long schoolId = SecurityUtil.schoolId();
        if (!existing.getSchoolId().equals(schoolId)) {
            throw new BusinessException(
                    "Access Denied: Subject belongs to another school.");
        }

        String normalizedName = toTitleCase(dto.getName());
        // Check for duplicate name if name is changed
        if (!existing.getName().equalsIgnoreCase(normalizedName)) {
            repository.findByNameIgnoreCaseAndSchoolId(normalizedName, schoolId).ifPresent(s -> {
                if (s.isActive()) {
                    throw new BusinessException("Subject already exists in this school: " + normalizedName);
                } else {
                    throw new BusinessException("Subject exists but inactive. Please reactivate instead.");
                }
            });
        }

        existing.setName(normalizedName);
        existing.setCode(dto.getCode());
        existing.setType(dto.getType());
        existing.setMaxMarks(dto.getMaxMarks());
        existing.setMinMarks(dto.getMinMarks());
        existing.setActive(dto.isActive());
        existing.setRemarks(dto.getRemarks());

        return mapper.toDto(repository.save(existing));
    }

    @org.springframework.transaction.annotation.Transactional
    public SubjectDto toggleActive(Long id) {
        Subject subject = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + id));

        if (!subject.getSchoolId().equals(SecurityUtil.schoolId())) {
            throw new BusinessException("Access Denied");
        }

        subject.setActive(!subject.isActive());
        return mapper.toDto(repository.save(subject));
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
