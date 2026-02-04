package com.school.backend.core.student.entity;

import com.school.backend.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Table(name = "promotion_records", indexes = {
        @Index(name = "idx_promotion_student_session", columnList = "student_id,session")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class PromotionRecord extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private Long studentId;

    private Long fromClassId;
    private String fromSection;

    @Column(nullable = false)
    private Long toClassId;
    private String toSection;

    @Column(nullable = false)
    private String session;

    @Builder.Default
    private LocalDate promotedOn = LocalDate.now();

    @Builder.Default
    private boolean promoted = true;

    @Builder.Default
    private boolean feePending = false;

    private String remarks;
}
