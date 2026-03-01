package com.school.backend.school.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubscriptionExtensionRequest {
    @NotNull
    @Min(1)
    private Integer additionalDays;
    @NotBlank
    private String reason;
}
