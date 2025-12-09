package com.school.backend.core.student.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class StudentDto {
    private Long id;
    private String admissionNumber;
    private String firstName;
    private String lastName;
    private LocalDate dob;
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
    private LocalDate dateOfLeaving;
    private String reasonForLeaving;
    private boolean active;
    private String currentStatus;
    private Long currentClassId;
    private Long schoolId;
    private String remarks;
}
