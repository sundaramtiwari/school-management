package com.school.backend.core.classsubject.mapper;

import com.school.backend.common.tenant.TenantContext;
import com.school.backend.core.classsubject.dto.SubjectDto;
import com.school.backend.core.classsubject.entity.Subject;
import org.springframework.stereotype.Component;

@Component
public class SubjectMapperImpl implements SubjectMapper {

    @Override
    public SubjectDto toDto(Subject entity) {
        if (entity == null) return null;
        SubjectDto dto = new SubjectDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCode(entity.getCode());
        dto.setType(entity.getType());
        dto.setMaxMarks(entity.getMaxMarks());
        dto.setMinMarks(entity.getMinMarks());
        dto.setActive(entity.isActive());
        dto.setRemarks(entity.getRemarks());
        return dto;
    }

    @Override
    public Subject toEntity(SubjectDto dto) {
        if (dto == null) return null;
        Subject entity = new Subject();
        entity.setName(dto.getName());
        entity.setCode(dto.getCode());
        entity.setType(dto.getType());
        entity.setMaxMarks(dto.getMaxMarks());
        entity.setMinMarks(dto.getMinMarks());
        entity.setActive(dto.isActive());
        entity.setRemarks(dto.getRemarks());
        entity.setSchoolId(TenantContext.getSchoolId());
        return entity;
    }
}
