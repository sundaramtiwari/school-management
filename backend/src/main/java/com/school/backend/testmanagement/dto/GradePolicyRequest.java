package com.school.backend.testmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GradePolicyRequest {

    @NotNull
    private Long schoolId;

    @NotNull
    private Double minPercent;

    @NotNull
    private Double maxPercent;

    @NotBlank
    private String grade;
}
