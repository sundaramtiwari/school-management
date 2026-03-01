package com.school.backend.school.entity;

import com.school.backend.common.enums.SubscriptionPaymentType;
import com.school.backend.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
@Table(name = "subscription_payments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_subscription_payment_reference", columnNames = {"subscription_id", "reference_number"})
        },
        indexes = {
                @Index(name = "idx_subscription_payment_sub_payment_date", columnList = "subscription_id,payment_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class SubscriptionPayment extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubscriptionPaymentType type;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "reference_number")
    private String referenceNumber;

    private String notes;

    @Column(name = "recorded_by_user_id", nullable = false)
    private Long recordedByUserId;

    @PrePersist
    @PreUpdate
    private void normalizeMoney() {
        if (amount != null) {
            amount = amount.setScale(2, RoundingMode.HALF_UP);
        }
    }
}
