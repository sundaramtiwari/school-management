package com.school.backend.fee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FeeStructureCreateRequest {

    @NotNull
    private Long classId;
    @NotBlank
    private String session;
    @NotNull
    private Long feeTypeId;
    @NotNull
    private Integer amount;
}
