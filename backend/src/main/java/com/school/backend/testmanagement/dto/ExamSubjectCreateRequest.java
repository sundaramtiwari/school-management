package com.school.backend.testmanagement.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExamSubjectCreateRequest {

    @NotNull
    private Long examId;

    @NotNull
    private Long subjectId;

    @NotNull
    private Integer maxMarks;
}
