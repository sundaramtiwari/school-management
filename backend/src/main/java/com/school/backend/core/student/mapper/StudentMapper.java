package com.school.backend.core.student.mapper;

import com.school.backend.core.student.dto.StudentCreateRequest;
import com.school.backend.core.student.dto.StudentDto;
import com.school.backend.core.student.entity.Student;

public interface StudentMapper {
    StudentDto toDto(Student entity);

    StudentDto toDto(Student entity, boolean enrollmentActive);

    Student toEntity(StudentCreateRequest dto);
}
