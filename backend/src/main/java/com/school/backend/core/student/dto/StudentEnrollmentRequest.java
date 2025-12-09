package com.school.backend.core.student.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StudentEnrollmentRequest {
    @NotNull
    private Long studentId;
    @NotNull
    private Long classId;
    private String section;
    @NotNull
    private String session;
    private Integer rollNumber;
    private LocalDate enrollmentDate;
    private String remarks;
}
