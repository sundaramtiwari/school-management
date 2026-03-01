package com.school.backend.school.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PricingPlanCreateRequest {
    @NotBlank
    private String name;
    private String description;
    @NotNull
    @DecimalMin("0.00")
    private BigDecimal yearlyPrice;
    @NotNull
    @Min(1)
    private Integer studentCap;
    @NotNull
    @Min(0)
    @Max(365)
    private Integer trialDaysDefault;
    @NotNull
    @Min(0)
    @Max(180)
    private Integer gracePeriodDaysDefault;
    @NotNull
    @Min(0)
    @Max(100)
    private Integer warningThresholdPercent;
    @NotNull
    @Min(0)
    @Max(100)
    private Integer criticalThresholdPercent;
    private Boolean active = true;
}
