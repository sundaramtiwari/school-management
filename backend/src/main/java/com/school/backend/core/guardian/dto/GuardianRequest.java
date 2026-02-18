package com.school.backend.core.guardian.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuardianRequest {
    private String name;
    private String relation; // FATHER, MOTHER, GUARDIAN, OTHER
    private String contactNumber;
    private String email;
    private String address;
    private String aadharNumber;
    private String occupation;
    private String qualification;
    private boolean primaryGuardian;
    private boolean whatsappEnabled;
}
