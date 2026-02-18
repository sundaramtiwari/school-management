package com.school.backend.fee.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

@Data
public class FeePaymentRequest {

    @NotNull
    private Long studentId;
    private Long sessionId;

    @NotNull
    @Positive
    private java.math.BigDecimal amountPaid; // Total

    private java.math.BigDecimal principalPaid;
    private java.math.BigDecimal lateFeePaid;

    // Optional (defaults to today)
    private LocalDate paymentDate;

    // CASH / UPI / BANK / CHEQUE
    private String mode;

    private String remarks;
}
