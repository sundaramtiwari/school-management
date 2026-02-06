package com.school.backend.fee.dto;

import lombok.Data;

@Data
public class FeeStructureDto {

    private Long id;

    private Long schoolId;
    private Long classId;
    private String session;

    private Long feeTypeId;
    private String feeTypeName;

    private Integer amount;
    private com.school.backend.fee.enums.FeeFrequency frequency;

    private boolean active;
}
