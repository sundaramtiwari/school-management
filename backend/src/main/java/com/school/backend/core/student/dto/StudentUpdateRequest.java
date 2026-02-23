package com.school.backend.core.student.dto;

import com.school.backend.common.enums.Gender;
import com.school.backend.common.enums.StudentStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StudentUpdateRequest {

    private String firstName;
    private String lastName;
    private LocalDate dob;
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

    private LocalDate dateOfLeaving;
    private String reasonForLeaving;

    // Previous School
    private String previousSchoolName;
    private String previousSchoolContact;
    private String previousSchoolAddress;
    private String previousSchoolBoard;
    private String previousClass;
    private Integer previousYearOfPassing;
    private String transferCertificateNumber;
    private String reasonForLeavingPreviousSchool;

    private Boolean active;
    private StudentStatus currentStatus;

    private String remarks;
}
