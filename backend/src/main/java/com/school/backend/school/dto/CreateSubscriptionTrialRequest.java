package com.school.backend.school.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateSubscriptionTrialRequest {
    @NotNull
    private Long schoolId;
    @NotNull
    private Long pricingPlanId;
    @Min(1)
    @Max(365)
    private Integer trialDays;
}
