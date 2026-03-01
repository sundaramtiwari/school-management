package com.school.backend.school.dto;

import com.school.backend.common.enums.ExpiryWarningLevel;
import com.school.backend.common.enums.SubscriptionStatus;
import com.school.backend.common.enums.UsageWarningLevel;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SubscriptionAccessStatusDto {
    private SubscriptionStatus subscriptionStatus;
    private BigDecimal usagePercent;
    private UsageWarningLevel usageWarningLevel;
    private Long daysToExpiry;
    private ExpiryWarningLevel expiryWarningLevel;
}
