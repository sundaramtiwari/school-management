package com.school.backend.core.guardian.mapper;

import com.school.backend.core.guardian.dto.GuardianCreateRequest;
import com.school.backend.core.guardian.dto.GuardianDto;
import com.school.backend.core.guardian.entity.Guardian;
import org.springframework.stereotype.Component;

@Component
public class GuardianMapperImpl implements GuardianMapper {

    @Override
    public GuardianDto toDto(Guardian entity) {
        if (entity == null) return null;
        GuardianDto dto = new GuardianDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setAadharNumber(entity.getAadharNumber());
        dto.setRelation(entity.getRelation());
        dto.setContactNumber(entity.getContactNumber());
        dto.setEmail(entity.getEmail());
        dto.setAddress(entity.getAddress());
        if (entity.getSchool() != null) dto.setSchoolId(entity.getSchool().getId());
        dto.setPhotoUrl(entity.getPhotoUrl());
        dto.setActive(entity.isActive());
        return dto;
    }

    @Override
    public Guardian toEntity(GuardianCreateRequest dto) {
        if (dto == null) return null;
        Guardian entity = new Guardian();
        entity.setName(dto.getName());
        entity.setAadharNumber(dto.getAadharNumber());
        entity.setRelation(dto.getRelation());
        entity.setContactNumber(dto.getContactNumber());
        entity.setEmail(dto.getEmail());
        entity.setAddress(dto.getAddress());
        entity.setPhotoUrl(dto.getPhotoUrl());
        return entity;
    }
}
