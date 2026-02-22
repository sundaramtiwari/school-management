package com.school.backend.fee.dto;

import com.school.backend.common.enums.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DiscountDefinitionCreateRequest {

    @NotBlank
    private String name;

    @NotNull
    private DiscountType type;

    @NotNull
    @Positive
    private BigDecimal amountValue;

    private Boolean active;
}
