package com.school.backend.core.classsubject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SchoolClassCreateRequest {

    @NotBlank
    private String name;        // e.g. "Class 1"

    @NotBlank
    private String section;     // e.g. "A"

    @NotBlank
    private String session;     // e.g. "2024-25"

    @NotNull
    private Long schoolId;

    // optional
    private Integer capacity;
    private String remarks;
}
