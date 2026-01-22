package com.school.backend.testmanagement.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MarkEntryRequest {

    @NotNull
    private Long examSubjectId;

    @NotNull
    private Long studentId;

    @NotNull
    private Integer marksObtained;

    private String remarks;
}
