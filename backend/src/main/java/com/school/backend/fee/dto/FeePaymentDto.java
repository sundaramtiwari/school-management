package com.school.backend.fee.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class FeePaymentDto {

    private Long id;

    private Long studentId;
    private Long sessionId;

    private java.math.BigDecimal amountPaid; // Total
    private java.math.BigDecimal principalPaid;
    private java.math.BigDecimal lateFeePaid;

    private LocalDate paymentDate;

    private String transactionReference; // Optional field

    private String mode;

    private String remarks;
}
