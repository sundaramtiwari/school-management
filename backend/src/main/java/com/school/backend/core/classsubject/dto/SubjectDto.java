package com.school.backend.core.classsubject.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubjectDto {
    private Long id;
    @NotBlank
    private String name;
    private String code;
    private String type;
    private Integer maxMarks;
    private Integer minMarks;
    private boolean active = true;
    private String remarks;
    private Long schoolId;
}
