package com.school.backend.core.classsubject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SchoolClassCreateRequest {

    @NotBlank
    private String name; // e.g. "Class 1"

    @NotBlank
    private String section; // e.g. "A"

    @NotNull
    private Long sessionId;

    @NotNull
    private Long schoolId;

    // optional
    private Integer capacity;
    private String remarks;
}
