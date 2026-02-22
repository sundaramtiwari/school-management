package com.school.backend.fee.entity;

import com.school.backend.common.entity.TenantEntity;
import com.school.backend.common.enums.DiscountType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "fee_adjustments", indexes = {
        @Index(name = "idx_fee_adjustment_assignment", columnList = "assignment_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class FeeAdjustment extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdjustmentType type;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "discount_definition_id")
    private Long discountDefinitionId;

    @Column(name = "discount_name_snapshot", length = 100)
    private String discountNameSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type_snapshot", length = 20)
    private DiscountType discountTypeSnapshot;

    @Column(name = "discount_value_snapshot", precision = 15, scale = 2)
    private BigDecimal discountValueSnapshot;

    private String reason;

    @Column(name = "created_by_staff")
    private String createdByStaff;

    public enum AdjustmentType {
        DISCOUNT,
        LATE_FEE_WAIVER
    }
}
