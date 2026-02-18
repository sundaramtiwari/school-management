package com.school.backend.core.student.dto;

import lombok.Data;

@Data
public class StudentGuardianDto {
    private Long id;
    private Long studentId;
    private Long guardianId;
    private boolean primaryGuardian;
    private String name;
    private String relation;
    private String contactNumber;
}
