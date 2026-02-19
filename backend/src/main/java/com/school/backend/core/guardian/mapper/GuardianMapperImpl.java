package com.school.backend.core.guardian.mapper;

import com.school.backend.core.guardian.dto.GuardianCreateRequest;
import com.school.backend.core.guardian.dto.GuardianDto;
import com.school.backend.core.guardian.entity.Guardian;
import org.springframework.stereotype.Component;

@Component
public class GuardianMapperImpl implements GuardianMapper {

    @Override
    public GuardianDto toDto(Guardian entity) {
        if (entity == null)
            return null;
        return GuardianDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .aadharNumber(entity.getAadharNumber())
                .relation(entity.getRelation())
                .contactNumber(entity.getContactNumber())
                .email(entity.getEmail())
                .address(entity.getAddress())
                .occupation(entity.getOccupation())
                .qualification(entity.getQualification())
                .whatsappEnabled(entity.isWhatsappEnabled())
                .photoUrl(entity.getPhotoUrl())
                .active(entity.isActive())
                .build();
    }

    @Override
    public Guardian toEntity(GuardianCreateRequest dto) {
        if (dto == null)
            return null;
        Guardian entity = new Guardian();
        entity.setName(dto.getName());
        entity.setAadharNumber(dto.getAadharNumber());
        entity.setRelation(dto.getRelation());
        entity.setContactNumber(dto.getContactNumber());
        entity.setEmail(dto.getEmail());
        entity.setAddress(dto.getAddress());
        entity.setOccupation(dto.getOccupation());
        entity.setQualification(dto.getQualification());
        entity.setWhatsappEnabled(dto.isWhatsappEnabled());
        return entity;
    }
}
