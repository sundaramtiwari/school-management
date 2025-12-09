package com.school.backend.core.student.mapper;

import com.school.backend.core.student.dto.StudentDto;
import com.school.backend.core.student.entity.Student;

public interface StudentMapper {
    StudentDto toDto(Student entity);

    Student toEntity(com.school.backend.core.student.dto.StudentCreateRequest dto);
}
