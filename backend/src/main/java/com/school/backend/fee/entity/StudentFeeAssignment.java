package com.school.backend.fee.entity;

import com.school.backend.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "student_fee_assignments", indexes = {
        @Index(name = "idx_student_fee_student_session",
                columnList = "student_id,session")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class StudentFeeAssignment extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "fee_structure_id", nullable = false)
    private Long feeStructureId;

    @Column(nullable = false)
    private String session;

    @Builder.Default
    private boolean active = true;
}
