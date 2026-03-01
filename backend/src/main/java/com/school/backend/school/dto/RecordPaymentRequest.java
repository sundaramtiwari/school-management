package com.school.backend.school.dto;

import com.school.backend.common.enums.SubscriptionPaymentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RecordPaymentRequest {
    @NotNull
    @DecimalMin("0.00")
    private BigDecimal amount;
    @NotNull
    private SubscriptionPaymentType type;
    @NotNull
    private LocalDate paymentDate;
    @NotBlank
    private String referenceNumber;
    private String notes;
}
