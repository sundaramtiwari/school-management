package com.school.backend.school.entity;

import com.school.backend.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
@Table(name = "subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Subscription extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", insertable = false, updatable = false)
    private School school;

    @Column(nullable = false)
    private String planName; // e.g., Basic, Pro, Premium

    private Integer studentLimit;
    @Column(precision = 15, scale = 2)
    private BigDecimal monthlyPrice;

    private LocalDate startDate;
    private LocalDate endDate;

    private boolean active;
    private boolean autoRenew;

    @PrePersist
    @PreUpdate
    private void normalizeMoney() {
        if (monthlyPrice != null) {
            monthlyPrice = monthlyPrice.setScale(2, RoundingMode.HALF_UP);
        }
    }
}
