package com.school.backend.school.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PricingPlanDto {
    private Long id;
    private String name;
    private String description;
    private BigDecimal yearlyPrice;
    private Integer studentCap;
    private Integer trialDaysDefault;
    private Integer gracePeriodDaysDefault;
    private Integer warningThresholdPercent;
    private Integer criticalThresholdPercent;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
