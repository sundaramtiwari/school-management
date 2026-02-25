package com.school.backend.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionPLResponseDto {
    private Long sessionId;
    private String sessionName;

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
    @Builder.Default
    private BigDecimal netCash = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal netBank = BigDecimal.ZERO;
}
