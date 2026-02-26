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
public class DayClosingDto {
    private Long id;
    private Long sessionId;
    private LocalDate date;
    private BigDecimal openingCash;
    private BigDecimal openingBank;
    private BigDecimal cashRevenue;
    private BigDecimal bankRevenue;
    private BigDecimal cashExpense;
    private BigDecimal bankExpense;
    private BigDecimal transferOut;
    private BigDecimal transferIn;
    private BigDecimal closingCash;
    private BigDecimal closingBank;
    private boolean overrideAllowed;
    private Long closedBy;
    private LocalDateTime closedAt;
}
