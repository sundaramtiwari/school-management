package com.school.backend.core.student.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PromotionRequest {
    @NotNull
    private Long studentId;
    @NotNull
    private Long fromClassId;
    @NotNull
    private Long toClassId;
    @NotNull
    private String session;
    private LocalDate promotedOn;
    private String remarks;
    private boolean feePending;
}
