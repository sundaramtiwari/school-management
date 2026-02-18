package com.school.backend.fee.dto;

import java.math.BigDecimal;
import com.school.backend.fee.enums.FeeFrequency;
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
    private com.school.backend.fee.enums.LateFeeType lateFeeType;
    private BigDecimal lateFeeAmountValue;
    private Integer lateFeeGraceDays;
    private com.school.backend.fee.enums.LateFeeCapType lateFeeCapType;
    private BigDecimal lateFeeCapValue;
}
