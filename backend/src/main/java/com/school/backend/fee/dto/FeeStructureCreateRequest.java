package com.school.backend.fee.dto;

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
    private Integer amount;

    private FeeFrequency frequency;
}
