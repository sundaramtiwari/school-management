package com.school.backend.expense.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseHeadDto {
    private Long id;
    private String name;
    private String description;
    private boolean active;
}
