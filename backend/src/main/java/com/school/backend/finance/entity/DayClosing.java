package com.school.backend.finance.entity;

import com.school.backend.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "day_closing", uniqueConstraints = {
        @UniqueConstraint(name = "uk_day_closing_school_date", columnNames = {"school_id", "date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class DayClosing extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "opening_cash", nullable = false, precision = 19, scale = 2)
    private BigDecimal openingCash;

    @Column(name = "opening_bank", nullable = false, precision = 19, scale = 2)
    private BigDecimal openingBank;

    @Column(name = "cash_revenue", nullable = false, precision = 19, scale = 2)
    private BigDecimal cashRevenue;

    @Column(name = "bank_revenue", nullable = false, precision = 19, scale = 2)
    private BigDecimal bankRevenue;

    @Column(name = "cash_expense", nullable = false, precision = 19, scale = 2)
    private BigDecimal cashExpense;

    @Column(name = "bank_expense", nullable = false, precision = 19, scale = 2)
    private BigDecimal bankExpense;

    @Column(name = "transfer_out", nullable = false, precision = 19, scale = 2)
    private BigDecimal transferOut;

    @Column(name = "transfer_in", nullable = false, precision = 19, scale = 2)
    private BigDecimal transferIn;

    @Column(name = "closing_cash", nullable = false, precision = 19, scale = 2)
    private BigDecimal closingCash;

    @Column(name = "closing_bank", nullable = false, precision = 19, scale = 2)
    private BigDecimal closingBank;

    @Builder.Default
    @Column(name = "override_allowed", nullable = false)
    private boolean overrideAllowed = false;

    @Column(name = "closed_by")
    private Long closedBy;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;
}
