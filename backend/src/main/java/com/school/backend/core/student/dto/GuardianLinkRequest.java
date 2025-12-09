package com.school.backend.core.student.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GuardianLinkRequest {
    @NotNull
    private Long guardianId;

    /**
     * mark this guardian as primary contact for the student
     */
    private boolean primaryGuardian = false;
}
