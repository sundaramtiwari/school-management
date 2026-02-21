package com.school.backend.core.student.dto;

import com.school.backend.common.enums.PromotionType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PromotionRecordDto {
    private Long id;
    private Long studentId;

    private Long sourceSessionId;
    private String sourceSessionName;

    private Long targetSessionId;
    private String targetSessionName;

    private Long sourceClassId;
    private String sourceClassName;

    private Long targetClassId;
    private String targetClassName;

    private PromotionType promotionType;
    private String remarks;
    private String promotedBy;
    private LocalDateTime promotedAt;
}
