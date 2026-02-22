package com.school.backend.fee.dto;

import com.school.backend.fee.entity.FeeAdjustment;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FeeAdjustmentDto {

    private Long id;
    private Long assignmentId;
    private String discountName;
    private FeeAdjustment.AdjustmentType type;
    private BigDecimal amount;
    private String remarks;
    private String createdByName;
    private LocalDateTime createdAt;
}
