package com.school.backend.fee.dto;

import com.school.backend.common.enums.DiscountType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DiscountDefinitionDto {

    private Long id;
    private String name;
    private DiscountType type;
    private BigDecimal amountValue;
    private boolean active;
}
