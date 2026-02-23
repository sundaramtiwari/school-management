package com.school.backend.fee.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.school.backend.common.enums.FeeFrequency;
import com.school.backend.common.enums.LateFeeCapType;
import com.school.backend.common.enums.LateFeeType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FeeStructurePatchRequest {
    private BigDecimal amount;
    private FeeFrequency frequency;

    @JsonAlias("dueMonth")
    private Integer dueDayOfMonth;

    // Optional late fee policy payload
    private Long lateFeePolicyId;
    private LateFeeType lateFeeType;
    private BigDecimal lateFeeAmountValue;
    private Integer lateFeeGraceDays;
    private LateFeeCapType lateFeeCapType;
    private BigDecimal lateFeeCapValue;

    // Immutable fields: if present with different value -> reject
    private Long schoolId;
    private Long sessionId;
    private Long classId;
    private Long feeTypeId;
}
