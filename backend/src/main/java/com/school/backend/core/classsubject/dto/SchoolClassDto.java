package com.school.backend.core.classsubject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SchoolClassDto {
    private Long id;
    @NotBlank
    private String name;
    private String section;
    @NotBlank
    private String session;
    private Integer capacity;
    @NotNull
    private Long schoolId;
    private Long classTeacherId;
    private boolean active = true;
    private String remarks;
}
