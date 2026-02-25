package com.school.backend.fee.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeePaymentAllocationRequest {

    @NotNull
    private Long assignmentId;

    @NotNull
    @Positive
    private BigDecimal principalAmount;
}
