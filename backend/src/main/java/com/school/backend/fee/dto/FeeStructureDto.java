package com.school.backend.fee.dto;

import lombok.Data;

@Data
public class FeeStructureDto {

    private Long id;

    private Long schoolId;
    private Long classId;
    private Long sessionId;

    private Long feeTypeId;
    private String feeTypeName;

    private java.math.BigDecimal amount;
    private com.school.backend.fee.enums.FeeFrequency frequency;

    private Integer dueDayOfMonth;
    private com.school.backend.fee.enums.LateFeeType lateFeeType;
    private java.math.BigDecimal lateFeeAmountValue;
    private Integer lateFeeGraceDays;
    private com.school.backend.fee.enums.LateFeeCapType lateFeeCapType;
    private java.math.BigDecimal lateFeeCapValue;

    private boolean active;
}
