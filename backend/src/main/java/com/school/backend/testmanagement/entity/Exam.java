package com.school.backend.testmanagement.entity;

import com.school.backend.common.entity.TenantEntity;
import com.school.backend.common.enums.ExamStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "exams", indexes = {
        @Index(name = "idx_exam_class_session", columnList = "class_id,session_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Exam extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(nullable = false)
    private String name; // Unit Test, Half Yearly, Annual

    private String examType; // UT / MID / FINAL

    private java.time.LocalDate startDate;
    private java.time.LocalDate endDate;

    @Builder.Default
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    private ExamStatus status;

}
