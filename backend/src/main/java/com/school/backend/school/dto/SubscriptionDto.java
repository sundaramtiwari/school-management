package com.school.backend.school.dto;

import com.school.backend.common.enums.SubscriptionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class SubscriptionDto {
    private Long id;
    private Long schoolId;
    private Long pricingPlanId;
    private String pricingPlanName;
    private SubscriptionStatus status;
    private LocalDate startDate;
    private LocalDate trialEndDate;
    private LocalDate expiryDate;
    private LocalDate graceEndDate;
    private Integer gracePeriodDays;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
