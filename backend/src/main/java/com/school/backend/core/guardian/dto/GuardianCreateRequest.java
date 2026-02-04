package com.school.backend.core.guardian.dto;

import jakarta.validation.constraints.NotBlank;
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
    private String photoUrl;
}
