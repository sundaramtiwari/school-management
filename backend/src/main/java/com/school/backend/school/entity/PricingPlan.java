package com.school.backend.school.entity;

import com.school.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "pricing_plans",
        indexes = {
                @Index(name = "idx_pricing_plan_active", columnList = "active")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class PricingPlan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "yearly_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal yearlyPrice;

    @Column(name = "student_cap", nullable = false)
    private Integer studentCap;

    @Column(name = "trial_days_default", nullable = false)
    private Integer trialDaysDefault;

    @Column(name = "grace_period_days_default", nullable = false)
    private Integer gracePeriodDaysDefault;

    @Column(name = "warning_threshold_percent", nullable = false)
    private Integer warningThresholdPercent;

    @Column(name = "critical_threshold_percent", nullable = false)
    private Integer criticalThresholdPercent;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @PrePersist
    @PreUpdate
    private void normalizeMoney() {
        if (yearlyPrice != null) {
            yearlyPrice = yearlyPrice.setScale(2, RoundingMode.HALF_UP);
        }
    }
}
