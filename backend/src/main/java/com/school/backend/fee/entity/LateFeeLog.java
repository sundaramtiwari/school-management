package com.school.backend.fee.entity;

import com.school.backend.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "late_fee_logs", indexes = {
        @Index(name = "idx_late_fee_logs_assignment", columnList = "assignment_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class LateFeeLog extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @Column(name = "computed_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal computedAmount;

    @Column(name = "applied_date", nullable = false)
    private LocalDate appliedDate;

    @Column(length = 255)
    private String reason;
}
