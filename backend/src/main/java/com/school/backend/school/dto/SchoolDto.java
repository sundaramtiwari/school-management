package com.school.backend.school.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for School entity.
 * Includes id and relevant public fields. Does not include internal audit
 * timestamps.
 * If you want createdAt/updatedAt later, we can add them.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolDto {
    private Long id;

    private String name;
    private String displayName;
    private String board; // CBSE, ICSE, State, etc.
    private String medium; // English, Hindi, etc.
    private String schoolCode;
    private String affiliationCode; // CBSE/Board affiliation number

    private String address;
    private String city;
    private String state;
    private String pincode;

    private String contactNumber;
    private String contactEmail;

    private String website;
    private String logoUrl;
    private String description;

    private Boolean active; // use Boolean to allow null in partial updates
}
