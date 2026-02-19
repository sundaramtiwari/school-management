package com.school.backend.fee.entity;

import com.school.backend.common.entity.TenantEntity;
import com.school.backend.common.enums.FundingCoverageMode;
import com.school.backend.common.enums.FundingCoverageType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "student_funding_arrangements", indexes = {
        @Index(name = "idx_funding_student_session_active", columnList = "student_id, session_id, active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class StudentFundingArrangement extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FundingCoverageType coverageType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FundingCoverageMode coverageMode;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal coverageValue;

    private LocalDate validFrom;
    private LocalDate validTo;

    @Builder.Default
    private boolean active = true;
}
