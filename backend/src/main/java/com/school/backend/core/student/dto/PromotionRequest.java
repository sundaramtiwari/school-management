package com.school.backend.core.student.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PromotionRequest {

    @NotNull
    private Long toClassId;

    private String toSection;

    @NotNull
    private Long sessionId;

    private boolean promoted = true;
    private boolean feePending = false;

    private String remarks;
}
