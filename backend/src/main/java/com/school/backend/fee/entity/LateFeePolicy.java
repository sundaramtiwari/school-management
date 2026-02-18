package com.school.backend.fee.entity;

import com.school.backend.common.entity.TenantEntity;
import com.school.backend.fee.enums.LateFeeCapType;
import com.school.backend.fee.enums.LateFeeType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "late_fee_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class LateFeePolicy extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_structure_id", nullable = false)
    private FeeStructure feeStructure;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private LateFeeType type = LateFeeType.NONE;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal amountValue = BigDecimal.ZERO;

    @Builder.Default
    private Integer graceDays = 0;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal capValue = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private LateFeeCapType capType = LateFeeCapType.NONE;

    @Builder.Default
    private boolean active = true;
}
