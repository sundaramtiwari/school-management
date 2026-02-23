package com.school.backend.core.student.dto;

import com.school.backend.common.enums.StudentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StudentWithdrawalRequest {
    @NotNull
    private Long sessionId;

    @NotNull
    private LocalDate withdrawalDate;

    @NotNull
    private StudentStatus status;

    private String reason;
}
