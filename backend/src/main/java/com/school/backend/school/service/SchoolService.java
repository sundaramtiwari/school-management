package com.school.backend.school.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.school.dto.SchoolDto;
import com.school.backend.school.entity.School;
import com.school.backend.school.mapper.SchoolMapper;
import com.school.backend.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SchoolService {

    private final SchoolRepository schoolRepository;

    /**
     * Create school from DTO and return saved DTO
     */
    public SchoolDto create(SchoolDto dto) {
        School entity = SchoolMapper.toEntity(dto);
        // ensure ID is not set by client
        entity.setId(null);
        School saved = schoolRepository.save(entity);
        return SchoolMapper.toDto(saved);
    }

    /**
     * Paginated listing returning Page<SchoolDto>
     */
    public Page<SchoolDto> listSchools(Pageable pageable) {
        return schoolRepository.findAll(pageable)
                .map(SchoolMapper::toDto);
    }

    /**
     * Non-paginated listing (kept for compatibility)
     */
    public List<SchoolDto> getAll() {
        return SchoolMapper.toDtos(schoolRepository.findAll());
    }

    public SchoolDto getByCode(String code) {
        School school = schoolRepository.findBySchoolCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("School not found with code: " + code));
        return SchoolMapper.toDto(school);
    }

    /**
     * Partial update: copy only non-null fields from dto to entity (PATCH semantics).
     */
    public SchoolDto updateByCode(String code, SchoolDto dto) {
        School entity = schoolRepository.findBySchoolCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("School not found with code: " + code));
        SchoolMapper.updateFromDto(dto, entity);
        School saved = schoolRepository.save(entity);
        return SchoolMapper.toDto(saved);
    }

    /**
     * Full replace (PUT semantics) - overwrite entity fields from DTO.
     * This preserves the same database id but replaces fields.
     */
    public SchoolDto replaceByCode(String code, SchoolDto dto) {
        School existing = schoolRepository.findBySchoolCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("School not found with code: " + code));

        // Create a new entity from DTO, but preserve DB id and createdAt/other BaseEntity fields
        School replacement = SchoolMapper.toEntity(dto);
        replacement.setId(existing.getId()); // important: keep PK
        // Optionally: copy audit fields from existing if you want to preserve them.
        // Save replacement (this will act as update since id is present)
        School saved = schoolRepository.save(replacement);
        return SchoolMapper.toDto(saved);
    }
}
