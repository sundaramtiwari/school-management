package com.school.backend.fee.entity;

import com.school.backend.common.entity.TenantEntity;
import com.school.backend.common.enums.LateFeeCapType;
import com.school.backend.common.enums.LateFeeType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "student_fee_assignments", uniqueConstraints = {
                @UniqueConstraint(columnNames = { "student_id", "fee_structure_id", "session_id" })
}, indexes = {
                @Index(name = "idx_student_fee_student_session", columnList = "student_id,session_id"),
                @Index(name = "idx_student_fee_school_session", columnList = "school_id,session_id,active"),
                @Index(name = "idx_student_fee_student_active", columnList = "student_id,active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class StudentFeeAssignment extends TenantEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @EqualsAndHashCode.Include
        private Long id;

        @Column(name = "student_id", nullable = false)
        private Long studentId;

        @Column(name = "fee_structure_id", nullable = false)
        private Long feeStructureId;

        @Column(name = "session_id", nullable = false)
        private Long sessionId;

        @Column(nullable = false, precision = 19, scale = 2)
        private BigDecimal amount;

        @Version
        private Long version;

        // --- Snapshot Fields ---
        private LocalDate dueDate;

        @Enumerated(EnumType.STRING)
        @Column(length = 20)
        private LateFeeType lateFeeType;

        @Column(precision = 19, scale = 2)
        private BigDecimal lateFeeValue;

        private Integer lateFeeGraceDays;

        @Enumerated(EnumType.STRING)
        @Column(length = 20)
        @Builder.Default
        private LateFeeCapType lateFeeCapType = LateFeeCapType.NONE;

        @Column(precision = 19, scale = 2)
        private BigDecimal lateFeeCapValue;

        // --- Aggregate / Tracking Fields ---
        @Builder.Default
        private boolean lateFeeApplied = false;

        @Column(precision = 19, scale = 2)
        @Builder.Default
        private BigDecimal lateFeeAccrued = BigDecimal.ZERO;

        @Column(precision = 19, scale = 2)
        @Builder.Default
        private BigDecimal principalPaid = BigDecimal.ZERO;

        @Column(precision = 19, scale = 2)
        @Builder.Default
        private BigDecimal lateFeePaid = BigDecimal.ZERO;

        @Column(precision = 19, scale = 2)
        @Builder.Default
        private BigDecimal lateFeeWaived = BigDecimal.ZERO;

        @Column(precision = 19, scale = 2)
        @Builder.Default
        private BigDecimal totalDiscountAmount = BigDecimal.ZERO;

        @Builder.Default
        private boolean active = true;
}
