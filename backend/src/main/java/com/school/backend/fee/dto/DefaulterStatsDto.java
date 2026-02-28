package com.school.backend.fee.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DefaulterStatsDto {
    private BigDecimal totalAmountDue;
    private long criticalCount;
}
