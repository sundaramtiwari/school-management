package com.school.backend.expense.dto;

import com.school.backend.common.enums.ExpensePaymentMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ExpenseVoucherCreateRequest {

    @NotNull
    private LocalDate expenseDate;

    @NotNull
    private Long expenseHeadId;

    @NotNull
    @DecimalMin(value = "0.01", inclusive = true)
    private BigDecimal amount;

    @NotNull
    private ExpensePaymentMode paymentMode;

    private String description;
    private String referenceNumber;
}
