package com.school.backend.core.student.entity;

import com.school.backend.common.entity.TenantEntity;
import com.school.backend.common.enums.AdmissionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Table(name = "student_enrollments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_student_session",
                        columnNames = {"student_id", "session_id", "active"}
                )
        },
        indexes = {
                @Index(name = "idx_enroll_student_session", columnList = "student_id,session_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class StudentEnrollment extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    private String section;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    private Integer rollNumber;

    private LocalDate enrollmentDate;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "admission_type", length = 20)
    private AdmissionType admissionType;

    @Builder.Default
    private boolean active = true;

    private String remarks;
}
