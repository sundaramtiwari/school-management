package com.school.backend.expense.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExpenseHeadCreateRequest {
    @NotBlank
    private String name;
    private String description;
}
