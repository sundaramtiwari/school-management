package com.school.backend.testmanagement.entity;

import com.school.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exams", indexes = {
        @Index(name = "idx_exam_class_session", columnList = "class_id,session")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Exam extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(nullable = false)
    private String session;   // 2025-26

    @Column(nullable = false)
    private String name;      // Unit Test, Half Yearly, Annual

    private String examType;  // UT / MID / FINAL

    @Builder.Default
    private boolean active = true;
}
