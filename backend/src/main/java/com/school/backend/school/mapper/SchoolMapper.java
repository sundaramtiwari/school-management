package com.school.backend.school.mapper;

import com.school.backend.school.dto.SchoolDto;
import com.school.backend.school.entity.School;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Manual mapper for School <-> SchoolDto
 * - toDto: maps entity to dto
 * - toEntity: maps dto to new entity (sets id only if dto.id != null)
 * - updateFromDto: copies only non-null fields from dto to existing entity (partial update)
 */
public final class SchoolMapper {

    private SchoolMapper() {
    }

    public static SchoolDto toDto(School entity) {
        if (entity == null) return null;

        SchoolDto dto = new SchoolDto();
        dto.setId(entity.getId());

        dto.setName(entity.getName());
        dto.setDisplayName(entity.getDisplayName());
        dto.setBoard(entity.getBoard());
        dto.setMedium(entity.getMedium());
        dto.setSchoolCode(entity.getSchoolCode());

        dto.setAddress(entity.getAddress());
        dto.setCity(entity.getCity());
        dto.setState(entity.getState());
        dto.setPincode(entity.getPincode());

        dto.setContactNumber(entity.getContactNumber());
        dto.setContactEmail(entity.getContactEmail());

        dto.setWebsite(entity.getWebsite());
        dto.setLogoUrl(entity.getLogoUrl());
        dto.setDescription(entity.getDescription());

        dto.setActive(entity.isActive());
        return dto;
    }

    public static School toEntity(SchoolDto dto) {
        if (dto == null) return null;

        School entity = new School();

        // set id only if provided (useful for tests or upsert flows)
        if (dto.getId() != null) {
            entity.setId(dto.getId());
        }

        entity.setName(dto.getName());
        entity.setDisplayName(dto.getDisplayName());
        entity.setBoard(dto.getBoard());
        entity.setMedium(dto.getMedium());
        entity.setSchoolCode(dto.getSchoolCode());

        entity.setAddress(dto.getAddress());
        entity.setCity(dto.getCity());
        entity.setState(dto.getState());
        entity.setPincode(dto.getPincode());

        entity.setContactNumber(dto.getContactNumber());
        entity.setContactEmail(dto.getContactEmail());

        entity.setWebsite(dto.getWebsite());
        entity.setLogoUrl(dto.getLogoUrl());
        entity.setDescription(dto.getDescription());

        // boolean: if dto.active is null, keep default true on entity (as entity initializes)
        if (dto.getActive() != null) {
            entity.setActive(dto.getActive());
        }

        return entity;
    }

    public static List<SchoolDto> toDtos(List<School> entities) {
        if (entities == null) return List.of();
        return entities.stream()
                .filter(Objects::nonNull)
                .map(SchoolMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Partial update: copy only non-null fields from dto to entity.
     * Does NOT change id.
     */
    public static void updateFromDto(SchoolDto dto, School entity) {
        if (dto == null || entity == null) return;

        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getDisplayName() != null) entity.setDisplayName(dto.getDisplayName());
        if (dto.getBoard() != null) entity.setBoard(dto.getBoard());
        if (dto.getMedium() != null) entity.setMedium(dto.getMedium());
        if (dto.getSchoolCode() != null) entity.setSchoolCode(dto.getSchoolCode());

        if (dto.getAddress() != null) entity.setAddress(dto.getAddress());
        if (dto.getCity() != null) entity.setCity(dto.getCity());
        if (dto.getState() != null) entity.setState(dto.getState());
        if (dto.getPincode() != null) entity.setPincode(dto.getPincode());

        if (dto.getContactNumber() != null) entity.setContactNumber(dto.getContactNumber());
        if (dto.getContactEmail() != null) entity.setContactEmail(dto.getContactEmail());

        if (dto.getWebsite() != null) entity.setWebsite(dto.getWebsite());
        if (dto.getLogoUrl() != null) entity.setLogoUrl(dto.getLogoUrl());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());

        if (dto.getActive() != null) entity.setActive(dto.getActive());
    }
}
