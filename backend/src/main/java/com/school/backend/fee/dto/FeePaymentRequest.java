package com.school.backend.fee.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class FeePaymentRequest {

    @NotNull
    private Long studentId;
    private Long sessionId;

    @NotEmpty
    @Valid
    private List<FeePaymentAllocationRequest> allocations;

    // Optional (defaults to today)
    private LocalDate paymentDate;

    // CASH / UPI / BANK / CHEQUE
    private String mode;

    private String transactionReference;

    private String remarks;
}
