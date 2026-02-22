package com.school.backend.fee.dto;

import com.school.backend.common.enums.LateFeeType;
import com.school.backend.common.enums.LateFeeCapType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class StudentFeeAssignmentDto {

    private Long id;

    private Long studentId;
    private Long feeStructureId;
    private Long sessionId;

    private BigDecimal amount;
    private String feeTypeName;

    // Snapshot
    private LocalDate dueDate;
    private LateFeeType lateFeeType;
    private BigDecimal lateFeeValue;
    private Integer lateFeeGraceDays;
    private LateFeeCapType lateFeeCapType;
    private BigDecimal lateFeeCapValue;

    // Aggregates
    private boolean lateFeeApplied;
    private BigDecimal lateFeeAccrued;
    private BigDecimal lateFeePaid;
    private BigDecimal lateFeeWaived;
    private BigDecimal totalDiscountAmount;
    private BigDecimal sponsorCoveredAmount;
    private BigDecimal principalPaid;
    private String status;

    private boolean active;
}
