package com.school.backend.fee.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class FeePaymentDto {

    private Long id;

    private Long studentId;

    private Integer amountPaid;

    private LocalDate paymentDate;

    private String transactionReference; // Optional field

    private String mode;

    private String remarks;
}
