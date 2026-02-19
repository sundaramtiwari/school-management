package com.school.backend.fee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentLedgerDto {
    private Long studentId;
    private String studentName;
    private List<FeeSummaryDto> sessionSummaries;
    private BigDecimal grandTotalFee;
    private BigDecimal grandTotalPaid;
    private BigDecimal grandTotalPending;
}
