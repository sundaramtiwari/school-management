package com.school.backend.fee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeStatsDto {
    private BigDecimal todayCollection;
    private BigDecimal pendingDues;
    private long totalStudents;
}
