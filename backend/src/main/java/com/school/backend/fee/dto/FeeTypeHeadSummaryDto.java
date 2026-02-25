package com.school.backend.fee.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeeTypeHeadSummaryDto {
    private Long feeTypeId;
    private String feeTypeName;
    private BigDecimal totalPrincipal;
    private BigDecimal totalLateFee;
    private BigDecimal totalCollected;
}
