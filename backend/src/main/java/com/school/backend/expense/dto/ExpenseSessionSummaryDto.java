package com.school.backend.expense.dto;

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
public class ExpenseSessionSummaryDto {
    private Long sessionId;
    private long totalVouchers;
    private BigDecimal totalExpense;
    private List<ExpenseHeadTotalDto> headWiseTotals;
}
