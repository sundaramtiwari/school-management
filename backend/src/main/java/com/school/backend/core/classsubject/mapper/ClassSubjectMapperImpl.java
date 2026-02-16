package com.school.backend.core.classsubject.mapper;

import com.school.backend.core.classsubject.dto.ClassSubjectDto;
import com.school.backend.core.classsubject.entity.ClassSubject;
import org.springframework.stereotype.Component;

@Component
public class ClassSubjectMapperImpl implements ClassSubjectMapper {

    @Override
    public ClassSubjectDto toDto(ClassSubject entity) {
        if (entity == null)
            return null;
        ClassSubjectDto dto = new ClassSubjectDto();
        dto.setId(entity.getId());
        if (entity.getSchoolClass() != null)
            dto.setClassId(entity.getSchoolClass().getId());
        if (entity.getSubject() != null)
            dto.setSubjectId(entity.getSubject().getId());
        dto.setTeacherId(entity.getTeacher() != null ? entity.getTeacher().getId() : null);
        dto.setDisplayOrder(entity.getDisplayOrder());
        dto.setActive(entity.isActive());
        dto.setSchoolId(entity.getSchoolId());

        // Populate flattened subject fields
        if (entity.getSubject() != null) {
            dto.setSubjectName(entity.getSubject().getName());
            dto.setSubjectCode(entity.getSubject().getCode());
        }

        return dto;
    }

    @Override
    public ClassSubject toEntity(ClassSubjectDto dto) {
        if (dto == null)
            return null;
        ClassSubject entity = new ClassSubject();
        entity.setDisplayOrder(dto.getDisplayOrder());
        entity.setActive(dto.isActive());
        // service will set schoolClass, subject, teacher, school explicitly
        return entity;
    }
}
