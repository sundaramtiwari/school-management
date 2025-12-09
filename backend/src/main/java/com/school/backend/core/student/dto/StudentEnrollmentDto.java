package com.school.backend.core.student.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class StudentEnrollmentDto {
    private Long id;
    private Long studentId;
    private Long classId;
    private String section;
    private String session;
    private Integer rollNumber;
    private LocalDate enrollmentDate;
    private boolean active;
    private String remarks;
}
