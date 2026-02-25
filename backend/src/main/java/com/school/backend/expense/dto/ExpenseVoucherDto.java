package com.school.backend.expense.dto;

import com.school.backend.common.enums.ExpensePaymentMode;
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
public class ExpenseVoucherDto {
    private Long id;
    private String voucherNumber;
    private LocalDate expenseDate;
    private Long expenseHeadId;
    private String expenseHeadName;
    private BigDecimal amount;
    private ExpensePaymentMode paymentMode;
    private String description;
    private String referenceNumber;
    private Long sessionId;
    private Long createdBy;
    private boolean active;
}
