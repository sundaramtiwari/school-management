package com.school.backend.fee.entity;

import com.school.backend.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "fee_types", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "name", "school_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class FeeType extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String name; // TUITION, EXAM, TRANSPORT

    private String description;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean transportBased = false;
}
