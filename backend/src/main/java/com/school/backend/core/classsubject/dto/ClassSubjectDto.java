package com.school.backend.core.classsubject.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClassSubjectDto {
    private Long id;
    @NotNull
    private Long classId;
    @NotNull
    private Long subjectId;
    private Long teacherId;
    private Integer displayOrder;
    private boolean active = true;
    private Long schoolId;

    // Flattened fields for frontend display
    private String subjectName;
    private String subjectCode;
}
