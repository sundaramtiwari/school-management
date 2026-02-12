package com.school.backend.school.entity;

import com.school.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "schools")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class School extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "display_name")
    private String displayName;

    private String board; // CBSE, ICSE, State, etc.
    private String medium; // English, Hindi, etc.

    @Column(unique = true, nullable = false)
    private String schoolCode;

    @Column(name = "affiliation_code")
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

    @Column(name = "current_session_id")
    private Long currentSessionId;

    @Builder.Default
    private boolean active = true;
}
