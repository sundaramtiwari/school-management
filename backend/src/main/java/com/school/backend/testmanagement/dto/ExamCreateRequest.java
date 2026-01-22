package com.school.backend.testmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExamCreateRequest {

    @NotNull
    private Long schoolId;

    @NotNull
    private Long classId;

    @NotBlank
    private String session;

    @NotBlank
    private String name;

    private String examType;
}
