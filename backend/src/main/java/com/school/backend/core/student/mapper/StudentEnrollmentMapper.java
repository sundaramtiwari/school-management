package com.school.backend.core.student.mapper;

import com.school.backend.core.student.dto.StudentEnrollmentDto;
import com.school.backend.core.student.entity.StudentEnrollment;

public interface StudentEnrollmentMapper {
    StudentEnrollmentDto toDto(StudentEnrollment e);

    StudentEnrollment toEntity(com.school.backend.core.student.dto.StudentEnrollmentRequest r);
}
