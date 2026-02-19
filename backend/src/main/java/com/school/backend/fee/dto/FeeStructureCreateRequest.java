package com.school.backend.fee.dto;

import java.math.BigDecimal;

import com.school.backend.common.enums.FeeFrequency;
import com.school.backend.common.enums.LateFeeCapType;
import com.school.backend.common.enums.LateFeeType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FeeStructureCreateRequest {

    @NotNull
    private Long classId;
    @NotNull
    private Long sessionId;
    @NotNull
    private Long feeTypeId;
    @NotNull
    private BigDecimal amount;

    private FeeFrequency frequency;
    private Integer dueDayOfMonth;

    // Late Fee Policy fields
    private LateFeeType lateFeeType;
    private BigDecimal lateFeeAmountValue;
    private Integer lateFeeGraceDays;
    private LateFeeCapType lateFeeCapType;
    private BigDecimal lateFeeCapValue;
}
