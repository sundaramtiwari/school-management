package com.school.backend.fee.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StudentFeeAssignRequest {

    @NotNull
    private Long studentId;

    @NotNull
    private Long feeStructureId;

    @NotNull
    private Long sessionId;

    private java.time.LocalDate dueDate;
}
