package com.school.backend.core.teacher.entity;

import com.school.backend.common.entity.TenantEntity;
import com.school.backend.core.classsubject.entity.SchoolClass;
import com.school.backend.core.classsubject.entity.Subject;
import com.school.backend.core.teacher.entity.Teacher;
import com.school.backend.school.entity.AcademicSession;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "teacher_assignments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_teacher_assignment", columnNames = { "teacher_id", "session_id", "school_class_id",
                "subject_id" })
}, indexes = {
        @Index(name = "idx_teacher_assignment_session_teacher", columnList = "session_id, teacher_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TeacherAssignment extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private AcademicSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_class_id", nullable = false)
    private SchoolClass schoolClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Builder.Default
    private boolean active = true;

    @Column(nullable = false)
    private LocalDateTime assignedAt;

    private Long assignedBy;
}
