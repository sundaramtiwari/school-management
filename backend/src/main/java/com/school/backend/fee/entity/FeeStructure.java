package com.school.backend.fee.entity;

import com.school.backend.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "fee_structures", indexes = {
        @Index(name = "idx_fee_structure_school_class_session",
                columnList = "school_id,class_id,session")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class FeeStructure extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(nullable = false)
    private String session; // 2025-26

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_type_id", nullable = false)
    private FeeType feeType;

    @Column(nullable = false)
    private Integer amount; // in INR

    @Builder.Default
    private boolean active = true;
}
