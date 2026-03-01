package com.school.backend.school.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ManualSuspendRequest {
    @NotBlank
    private String reason;
}
