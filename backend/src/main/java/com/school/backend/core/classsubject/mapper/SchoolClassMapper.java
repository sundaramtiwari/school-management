package com.school.backend.core.classsubject.mapper;

import com.school.backend.core.classsubject.dto.SchoolClassDto;
import com.school.backend.core.classsubject.entity.SchoolClass;

public interface SchoolClassMapper {
    SchoolClassDto toDto(SchoolClass entity);

    SchoolClass toEntity(SchoolClassDto dto);
}
