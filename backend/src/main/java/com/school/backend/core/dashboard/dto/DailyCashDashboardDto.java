package com.school.backend.core.dashboard.dto;

import com.school.backend.expense.dto.ExpenseHeadTotalDto;
import com.school.backend.fee.dto.FeeTypeHeadSummaryDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyCashDashboardDto {
    @Builder.Default
    private BigDecimal totalFeeCollected = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalExpense = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal netCash = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal cashRevenue = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal bankRevenue = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal cashExpense = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal bankExpense = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal netBank = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal transferOut = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal transferIn = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal netAmount = BigDecimal.ZERO;
    @Builder.Default
    private boolean closed = false;
    @Builder.Default
    private List<FeeTypeHeadSummaryDto> headWiseCollection = new ArrayList<>();
    @Builder.Default
    private List<ExpenseHeadTotalDto> expenseBreakdown = new ArrayList<>();
}
