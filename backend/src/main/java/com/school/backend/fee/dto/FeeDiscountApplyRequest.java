package com.school.backend.fee.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FeeDiscountApplyRequest {

    @NotNull
    private Long discountDefinitionId;

    private String remarks;
}
