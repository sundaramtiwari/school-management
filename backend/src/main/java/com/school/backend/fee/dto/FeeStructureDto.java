package com.school.backend.fee.dto;

import com.school.backend.common.enums.FeeFrequency;
import com.school.backend.common.enums.LateFeeCapType;
import com.school.backend.common.enums.LateFeeType;
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
    private FeeFrequency frequency;

    private Integer dueDayOfMonth;
    private LateFeeType lateFeeType;
    private java.math.BigDecimal lateFeeAmountValue;
    private Integer lateFeeGraceDays;
    private LateFeeCapType lateFeeCapType;
    private java.math.BigDecimal lateFeeCapValue;

    private boolean active;
}
