package com.school.backend.core.student.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StudentWithdrawalRequest {

    @NotNull
    private Long sessionId;

    @NotNull
    private LocalDate withdrawalDate;

    private String reason;
}
