package com.school.backend.core.guardian.dto;

import lombok.Data;

@Data
public class GuardianDto {
    private Long id;
    private String name;
    private String aadharNumber;
    private String relation;
    private String contactNumber;
    private String email;
    private String address;
    private Long schoolId;
    private String photoUrl;
    private boolean active;
}
