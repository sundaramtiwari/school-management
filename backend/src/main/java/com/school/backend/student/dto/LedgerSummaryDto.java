package com.school.backend.student.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LedgerSummaryDto {
    private Long sessionId;
    private String sessionName;
    private BigDecimal totalAssigned = BigDecimal.ZERO;
    private BigDecimal totalDiscount = BigDecimal.ZERO;
    private BigDecimal totalFunding = BigDecimal.ZERO;
    private BigDecimal totalLateFee = BigDecimal.ZERO;
    private BigDecimal totalPaid = BigDecimal.ZERO;
    private BigDecimal totalPending = BigDecimal.ZERO;
}
