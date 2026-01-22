package com.school.backend.fee.dto;

import lombok.Data;

@Data
public class FeeSummaryDto {

    private Long studentId;
    private String session;

    private int totalFee;     // sum of fee structure
    private int totalPaid;    // sum of payments
    private int pendingFee;   // totalFee - totalPaid

    private boolean feePending;
}
