package com.school.backend.expense.entity;

import com.school.backend.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "expense_heads", uniqueConstraints = {
        @UniqueConstraint(name = "uk_expense_head_school_normalized_name", columnNames = { "school_id", "normalized_name" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class ExpenseHead extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 150)
    private String normalizedName;

    @Column(length = 500)
    private String description;

    @Builder.Default
    private boolean active = true;
}
