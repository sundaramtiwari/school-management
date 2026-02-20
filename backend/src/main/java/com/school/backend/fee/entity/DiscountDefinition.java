package com.school.backend.fee.entity;

import com.school.backend.common.entity.TenantEntity;
import com.school.backend.common.enums.DiscountType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "discount_definitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class DiscountDefinition extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType type;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amountValue;

    @Builder.Default
    private boolean active = true;
}
