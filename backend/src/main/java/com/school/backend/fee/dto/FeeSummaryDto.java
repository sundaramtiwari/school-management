package com.school.backend.fee.dto;

import lombok.Data;

@Data
public class FeeSummaryDto {

    private Long studentId;
    private String studentName;
    private String session;

    private java.math.BigDecimal totalFee; // sum of fee structure principal
    private java.math.BigDecimal totalDiscount;
    private java.math.BigDecimal totalFunding;
    private java.math.BigDecimal totalPaid; // sum of principal + late fee payments
    private java.math.BigDecimal pendingFee; // total (Principal + Accrued Late Fee) - Total Paid
    private java.math.BigDecimal totalLateFeeAccrued;
    private java.math.BigDecimal totalLateFeePaid;

    private boolean feePending;
}
