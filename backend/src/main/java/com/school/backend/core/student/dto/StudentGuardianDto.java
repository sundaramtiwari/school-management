package com.school.backend.core.student.dto;

import lombok.Data;

@Data
public class StudentGuardianDto {
    private Long id;
    private Long studentId;
    private Long guardianId;
    private boolean primaryGuardian;
}
