package com.school.backend.core.classsubject.mapper;

import com.school.backend.core.classsubject.dto.SchoolClassDto;
import com.school.backend.core.classsubject.entity.SchoolClass;
import org.springframework.stereotype.Component;

@Component
public class SchoolClassMapperImpl implements SchoolClassMapper {

    @Override
    public SchoolClassDto toDto(SchoolClass entity) {
        if (entity == null)
            return null;
        SchoolClassDto dto = new SchoolClassDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setSection(entity.getSection());
        dto.setSessionId(entity.getSessionId());
        dto.setCapacity(entity.getCapacity());
        dto.setRemarks(entity.getRemarks());
        dto.setActive(entity.isActive());

        if (entity.getClassTeacher() != null) {
            dto.setClassTeacherId(entity.getClassTeacher().getId());
        }
        return dto;
    }

    @Override
    public SchoolClass toEntity(SchoolClassDto dto) {
        if (dto == null)
            return null;
        SchoolClass entity = new SchoolClass();
        entity.setName(dto.getName());
        entity.setSection(dto.getSection());
        entity.setSessionId(dto.getSessionId());
        entity.setCapacity(dto.getCapacity());
        entity.setRemarks(dto.getRemarks());
        entity.setActive(dto.isActive());
        // do not set related entities here; service will set school/classTeacher by id
        return entity;
    }
}
