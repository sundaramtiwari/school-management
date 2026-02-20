package com.school.backend.school.entity;

import com.school.backend.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
@Table(name = "subscription_payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class SubscriptionPayment extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(precision = 15, scale = 2)
    private BigDecimal amountPaid;
    private LocalDate paymentDate;
    private String paymentMode; // ONLINE, OFFLINE
    private String transactionReference;
    private String invoiceUrl;

    @PrePersist
    @PreUpdate
    private void normalizeMoney() {
        if (amountPaid != null) {
            amountPaid = amountPaid.setScale(2, RoundingMode.HALF_UP);
        }
    }
}
