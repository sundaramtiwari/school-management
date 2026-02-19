package com.school.backend.student.dto;

import com.school.backend.student.enums.PromotionType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PromotionRequest {

    @NotEmpty
    private List<Long> studentIds;

    @NotNull
    private Long targetSessionId;

    @NotNull
    private Long targetClassId;

    @NotNull
    private PromotionType promotionType;

    private String remarks;
}
