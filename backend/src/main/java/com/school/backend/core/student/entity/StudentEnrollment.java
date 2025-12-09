package com.school.backend.core.student.entity;

import com.school.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "student_enrollments", indexes = {
        @Index(name = "idx_enroll_student_session", columnList = "student_id,session")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class StudentEnrollment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    private String section;
    private String session; // e.g., "2025-26"

    private Integer rollNumber;
    private LocalDate enrollmentDate;

    @Builder.Default
    private boolean active = true;
    private String remarks;
}
