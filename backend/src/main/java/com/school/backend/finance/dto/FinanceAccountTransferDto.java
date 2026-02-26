package com.school.backend.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceAccountTransferDto {
    private Long id;
    private Long sessionId;
    private LocalDate transferDate;
    private BigDecimal amount;
    private String fromAccount;
    private String toAccount;
    private String referenceNumber;
    private String remarks;
    private Long createdBy;
    private LocalDateTime createdAt;
}
