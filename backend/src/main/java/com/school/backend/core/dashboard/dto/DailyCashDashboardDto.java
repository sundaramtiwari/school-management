package com.school.backend.core.dashboard.dto;

import com.school.backend.expense.dto.ExpenseHeadTotalDto;
import com.school.backend.fee.dto.FeeTypeHeadSummaryDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyCashDashboardDto {
    private BigDecimal totalFeeCollected;
    private BigDecimal totalExpense;
    private BigDecimal netCash;
    private List<FeeTypeHeadSummaryDto> headWiseCollection;
    private List<ExpenseHeadTotalDto> expenseBreakdown;
}
