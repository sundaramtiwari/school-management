package com.school.backend.core.classsubject.entity;

import com.school.backend.common.entity.TenantEntity;
import com.school.backend.core.teacher.entity.Teacher;
import com.school.backend.school.entity.School;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "school_classes", indexes = {
        @Index(name = "idx_schoolclass_schoolid_name_session", columnList = "school_id,name,session")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class SchoolClass extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * Class name: "Nursery", "LKG", "1", "10", etc.
     */
    @Column(nullable = false)
    private String name;

    /**
     * Section (optional). We keep a simple single-letter/label.
     */
    private String section;

    /**
     * Academic session like "2025-26"
     */
    @Column(nullable = false)
    private String session;

    /**
     * Capacity of the class (optional)
     */
    private Integer capacity;

    /**
     * Optional reference to the class teacher (User / Teacher id)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_teacher_id")
    private Teacher classTeacher;

    /**
     * Many classes belong to a School (multi-tenant)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", insertable = false, updatable = false)
    private School school;

    @Builder.Default
    private boolean active = true;

    private String remarks;
}
