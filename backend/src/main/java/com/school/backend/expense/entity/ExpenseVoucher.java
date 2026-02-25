package com.school.backend.expense.entity;

import com.school.backend.common.entity.TenantEntity;
import com.school.backend.common.enums.ExpensePaymentMode;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "expense_vouchers", indexes = {
        @Index(name = "idx_expense_voucher_school_session", columnList = "school_id, session_id"),
        @Index(name = "idx_expense_voucher_school_date", columnList = "school_id, expense_date")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_expense_voucher_school_voucher_number", columnNames = { "school_id", "voucher_number" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class ExpenseVoucher extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Version
    private Long version;

    @Column(name = "voucher_number", nullable = false, length = 50)
    private String voucherNumber;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_head_id", nullable = false)
    private ExpenseHead expenseHead;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false, length = 10)
    private ExpensePaymentMode paymentMode;

    @Column(length = 500)
    private String description;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Builder.Default
    private boolean active = true;
}
