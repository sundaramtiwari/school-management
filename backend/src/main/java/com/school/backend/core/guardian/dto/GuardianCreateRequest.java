package com.school.backend.core.guardian.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GuardianCreateRequest {
    @NotBlank
    private String name;
    private String aadharNumber;
    private String relation;
    private String contactNumber;
    private String email;
    private String address;
    @NotNull
    private Long schoolId;
    private String photoUrl;
}
