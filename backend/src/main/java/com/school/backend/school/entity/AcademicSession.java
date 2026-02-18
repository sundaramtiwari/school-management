package com.school.backend.school.entity;

import com.school.backend.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Table(name = "academic_sessions", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "name", "school_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AcademicSession extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // e.g., "2024-25"

    @Column
    private LocalDate startDate;

    @Column
    private LocalDate endDate;

    @Builder.Default
    private boolean active = true;

}
