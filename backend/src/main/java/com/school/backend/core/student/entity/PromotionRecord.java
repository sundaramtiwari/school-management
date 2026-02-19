package com.school.backend.core.student.entity;

import com.school.backend.common.entity.TenantEntity;
import com.school.backend.student.enums.PromotionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "promotion_records", indexes = {
        @Index(name = "idx_promotion_student_target_session", columnList = "student_id,target_session_id")
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

    @Column(name = "source_session_id", nullable = false)
    private Long sourceSessionId;

    @Column(name = "target_session_id", nullable = false)
    private Long targetSessionId;

    @Column(name = "source_class_id", nullable = false)
    private Long sourceClassId;

    @Column(name = "target_class_id", nullable = false)
    private Long targetClassId;

    @Enumerated(EnumType.STRING)
    @Column(name = "promotion_type", nullable = false, length = 20)
    private PromotionType promotionType;

    @Column(name = "promoted_by", nullable = false, length = 255)
    private String promotedBy;

    @Column(name = "promoted_at", nullable = false)
    private LocalDateTime promotedAt;

    private String remarks;
}
