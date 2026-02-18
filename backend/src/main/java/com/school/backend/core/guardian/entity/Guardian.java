package com.school.backend.core.guardian.entity;

import com.school.backend.common.entity.TenantEntity;
import com.school.backend.school.entity.School;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "guardians", uniqueConstraints = {
        @UniqueConstraint(name = "uk_guardian_school_contact", columnNames = { "school_id", "contactNumber" })
}, indexes = {
        @Index(name = "idx_guardian_school_aadhar", columnList = "school_id,aadhar_number")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Guardian extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "aadhar_number")
    private String aadharNumber;

    private String relation;

    @Column(nullable = false)
    private String contactNumber;

    private String email;

    private String address;

    private String occupation;

    private String qualification;

    @Builder.Default
    private boolean whatsappEnabled = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", insertable = false, updatable = false)
    private School school;

    private String photoUrl;

    @Builder.Default
    private boolean active = true;
}
