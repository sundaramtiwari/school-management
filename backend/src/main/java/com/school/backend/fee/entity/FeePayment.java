package com.school.backend.fee.entity;

import com.school.backend.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fee_payments", indexes = {
        @Index(name = "idx_fee_payment_student", columnList = "student_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class FeePayment extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Version
    private Long version;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal principalPaid = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal lateFeePaid = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDate paymentDate;

    @Column(name = "transaction_reference", length = 100)
    private String transactionReference; // For UPI/online payments

    private String mode; // CASH / UPI / BANK
    private String remarks;

    public BigDecimal getAmountPaid() {
        return (principalPaid != null ? principalPaid : BigDecimal.ZERO)
                .add(lateFeePaid != null ? lateFeePaid : BigDecimal.ZERO);
    }
}
