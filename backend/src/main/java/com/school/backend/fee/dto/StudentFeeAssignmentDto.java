package com.school.backend.fee.dto;

import lombok.Data;

@Data
public class StudentFeeAssignmentDto {

    private Long id;

    private Long studentId;
    private Long feeStructureId;
    private Long sessionId;

    private boolean active;
}
