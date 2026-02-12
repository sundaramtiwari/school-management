package com.school.backend.core.student.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PromotionRecordDto {
    private Long id;
    private Long studentId;

    private Long fromClassId;
    private String fromSection;

    private Long toClassId;
    private String toSection;

    private Long sessionId;
    private LocalDate promotedOn;

    private boolean promoted;
    private boolean feePending;

    private String remarks;
}
