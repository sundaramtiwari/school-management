package com.school.backend.expense.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseHeadTotalDto {
    private Long expenseHeadId;
    private String expenseHeadName;
    private BigDecimal totalAmount;
}
