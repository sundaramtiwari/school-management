package com.school.backend.school.dto;

import com.school.backend.common.enums.SubscriptionPaymentType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class SubscriptionPaymentDto {
    private Long id;
    private Long subscriptionId;
    private BigDecimal amount;
    private SubscriptionPaymentType type;
    private LocalDate paymentDate;
    private String referenceNumber;
    private String notes;
    private Long recordedByUserId;
    private LocalDateTime createdAt;
}
