package com.school.backend.fee.entity;

import com.school.backend.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "fee_payment_allocations", indexes = {
        @Index(name = "idx_fee_payment_allocation_payment", columnList = "fee_payment_id"),
        @Index(name = "idx_fee_payment_allocation_session", columnList = "session_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class FeePaymentAllocation extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "fee_payment_id", nullable = false)
    private Long feePaymentId;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_type_id", nullable = false)
    private FeeType feeType;

    @Column(name = "principal_amount", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal principalAmount = BigDecimal.ZERO;

    @Column(name = "late_fee_amount", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal lateFeeAmount = BigDecimal.ZERO;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;
}
