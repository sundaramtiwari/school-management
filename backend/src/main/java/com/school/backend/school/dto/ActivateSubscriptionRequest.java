package com.school.backend.school.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ActivateSubscriptionRequest {
    @NotNull
    private LocalDate paymentDate;
    @NotNull
    private String referenceNumber;
    private String notes;
}
