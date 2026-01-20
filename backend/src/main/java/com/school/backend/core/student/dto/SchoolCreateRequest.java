package com.school.backend.core.student.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SchoolCreateRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String displayName;

    @NotBlank
    private String board;

    @NotBlank
    private String address;

    // optional fields (safe for MVP)
    private String website;
    private String contactNumber;
}
