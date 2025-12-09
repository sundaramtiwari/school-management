package com.school.backend.core.classsubject.entity;

import com.school.backend.common.entity.BaseEntity;
import com.school.backend.core.teacher.entity.Teacher;
import com.school.backend.school.entity.School;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "class_subjects", uniqueConstraints = {
        @UniqueConstraint(name = "uk_class_subject", columnNames = {"class_id", "subject_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class ClassSubject extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * Owning class
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass schoolClass;

    /**
     * Subject
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    /**
     * Optional teacher assigned to this subject in the class (teacher_id from Teacher module)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;


    /**
     * Sequence/order of subject in timetable or reports (optional)
     */
    private Integer displayOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    private boolean active = true;
}
