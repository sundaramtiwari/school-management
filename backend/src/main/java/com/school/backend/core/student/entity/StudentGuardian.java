package com.school.backend.core.student.entity;

import com.school.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "student_guardians", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "guardian_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class StudentGuardian extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "guardian_id", nullable = false)
    private Long guardianId;

    @Builder.Default
    private boolean primaryGuardian = false; // one primary per student
}
