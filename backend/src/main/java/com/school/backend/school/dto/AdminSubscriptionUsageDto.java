package com.school.backend.school.dto;

import com.school.backend.common.enums.ExpiryWarningLevel;
import com.school.backend.common.enums.SubscriptionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class AdminSubscriptionUsageDto {
    private Long subscriptionId;
    private String planName;
    private SubscriptionStatus subscriptionStatus;
    private Integer studentCap;
    private Long activeStudents;
    private BigDecimal usagePercent;
    private LocalDate expiryDate;
    private LocalDate graceEndDate;
    private Long daysToExpiry;
    private ExpiryWarningLevel expiryWarningLevel;
}
