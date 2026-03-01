package com.school.backend.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialOverviewDto {
    private String periodName;
    private LocalDate startDate;
    private LocalDate endDate;

    @Builder.Default
    private BigDecimal totalRevenue = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalExpense = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal netProfit = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal cashRevenue = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal bankRevenue = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal cashExpense = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal bankExpense = BigDecimal.ZERO;
}
