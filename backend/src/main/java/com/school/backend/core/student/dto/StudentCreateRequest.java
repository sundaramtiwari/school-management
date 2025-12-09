package com.school.backend.core.student.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StudentCreateRequest {
    @NotBlank
    private String admissionNumber;
    @NotBlank
    private String firstName;
    private String lastName;
    private LocalDate dob;
    @NotBlank
    private String gender;
    private String pen;
    private String aadharNumber;
    private String religion;
    private String caste;
    private String category;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String contactNumber;
    private String email;
    private String bloodGroup;
    private String photoUrl;
    private LocalDate dateOfAdmission;
    private Long schoolId;
    private String remarks;
}
