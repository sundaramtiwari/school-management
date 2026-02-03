package com.school.backend.core.student.dto;

import com.school.backend.common.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    @NotNull
    private Gender gender;
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
    @NotNull
    private Long schoolId;
    private String remarks;
    // Previous School Details
    private String previousSchoolName;
    private String previousSchoolBoard;
    private String previousClass;
    private Integer previousYearOfPassing;
    private String transferCertificateNumber;
    private String previousSchoolAddress;
    private String previousSchoolContact;
    private String reasonForLeavingPreviousSchool;

}
