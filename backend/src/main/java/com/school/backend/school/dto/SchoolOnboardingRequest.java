package com.school.backend.school.dto;

import com.school.backend.common.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SchoolOnboardingRequest {

    // School Details
    @NotBlank(message = "School name is required")
    private String name;

    // Optional: If not provided, will be auto-generated (e.g., SCH001)
    private String schoolCode;

    private String displayName;

    @NotBlank(message = "Board is required")
    private String board;

    @NotBlank(message = "Medium is required")
    private String medium;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @Email(message = "Invalid email")
    private String contactEmail;

    private String contactNumber;
    private String address;
    private String pincode;
    private String website;
    private String description;

    // Admin User Details
    @NotBlank(message = "Admin email is required")
    @Email(message = "Invalid admin email")
    private String adminEmail;

    @NotBlank(message = "Admin password is required")
    private String adminPassword;

    private String adminName;
}
