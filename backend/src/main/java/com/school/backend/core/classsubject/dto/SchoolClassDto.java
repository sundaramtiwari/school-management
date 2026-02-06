package com.school.backend.core.classsubject.dto;

import jakarta.validation.constraints.NotBlank;
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
    private Long classTeacherId;
    private boolean active = true;
    private String remarks;
    private Long schoolId;
}
