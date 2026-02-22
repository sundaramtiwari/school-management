package com.school.backend.fee.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LateFeeWaiverRequest {

    @NotNull
    @DecimalMin(value = "0.01", inclusive = true, message = "Waiver amount must be greater than zero.")
    private BigDecimal waiverAmount;

    private String remarks;
}
