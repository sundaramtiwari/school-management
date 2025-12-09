package com.school.backend.core.classsubject.mapper;

import com.school.backend.core.classsubject.dto.ClassSubjectDto;
import com.school.backend.core.classsubject.entity.ClassSubject;

public interface ClassSubjectMapper {
    ClassSubjectDto toDto(ClassSubject entity);

    ClassSubject toEntity(ClassSubjectDto dto);
}
