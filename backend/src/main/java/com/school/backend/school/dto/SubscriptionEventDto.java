package com.school.backend.school.dto;

import com.school.backend.common.enums.SubscriptionEventType;
import com.school.backend.common.enums.SubscriptionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class SubscriptionEventDto {
    private Long id;
    private Long subscriptionId;
    private SubscriptionEventType type;
    private Integer daysAdded;
    private LocalDate previousExpiryDate;
    private LocalDate newExpiryDate;
    private SubscriptionStatus previousStatus;
    private SubscriptionStatus newStatus;
    private String reason;
    private Long performedByUserId;
    private String performedBy;
    private LocalDateTime createdAt;
}
