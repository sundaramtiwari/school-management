package com.school.backend.core.student.entity;

import com.school.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "promotion_records", indexes = {
        @Index(name = "idx_promotion_student_session", columnList = "student_id,session")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class PromotionRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "from_class_id")
    private Long fromClassId;

    @Column(name = "to_class_id")
    private Long toClassId;

    private String session;
    private LocalDate promotedOn;
    private String remarks;
    private boolean feePending;
}
