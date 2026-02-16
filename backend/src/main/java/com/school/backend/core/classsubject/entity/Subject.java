package com.school.backend.core.classsubject.entity;

import com.school.backend.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subjects", uniqueConstraints = {
        @UniqueConstraint(name = "uk_subject_school_name", columnNames = { "school_id", "name" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Subject extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * Subject name like "Mathematics", "English"
     */
    @Column(nullable = false)
    private String name;

    /**
     * Code (optional) like "MATH", "ENG"
     */
    private String code;

    /**
     * Type (THEORY/PRACTICAL/LANGUAGE) - keep string for now or use enum later
     */
    private String type;

    /**
     * Max and min marks default for subject tests
     */
    private Integer maxMarks;
    private Integer minMarks;

    @Builder.Default
    private boolean active = true;

    private String remarks;
}
