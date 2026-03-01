package com.school.backend.school.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class UpgradePlanResponse {
    private SubscriptionDto subscription;
    private SubscriptionPaymentDto prorationPayment;
    private BigDecimal proratedAmount;
}
