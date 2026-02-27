package com.school.backend.fee.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class FeeDiscountApplyResponse {
    private BigDecimal appliedAmount;
    private boolean capped;
    private String message;
}
