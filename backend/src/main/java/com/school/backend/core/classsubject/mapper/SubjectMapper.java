package com.school.backend.core.classsubject.mapper;

import com.school.backend.core.classsubject.dto.SubjectDto;
import com.school.backend.core.classsubject.entity.Subject;

public interface SubjectMapper {
    SubjectDto toDto(Subject entity);

    Subject toEntity(SubjectDto dto);
}
