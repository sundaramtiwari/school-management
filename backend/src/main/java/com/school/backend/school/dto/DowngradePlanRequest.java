package com.school.backend.school.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DowngradePlanRequest {
    @NotNull
    private Long newPlanId;
    private String reason;
}
