package com.school.backend.finance.repository;

import com.school.backend.finance.entity.FinanceAccountTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FinanceAccountTransferRepository extends JpaRepository<FinanceAccountTransfer, Long> {
    interface MovementAggregate {
        java.math.BigDecimal getCashRevenue();

        java.math.BigDecimal getBankRevenue();

        java.math.BigDecimal getCashExpense();

        java.math.BigDecimal getBankExpense();

        java.math.BigDecimal getTransferIn();

        java.math.BigDecimal getTransferOut();
    }

    List<FinanceAccountTransfer> findBySchoolIdAndSessionIdAndTransferDateBetween(
            Long schoolId,
            Long sessionId,
            LocalDate fromDate,
            LocalDate toDate);

    @Query(value = """
            SELECT
              COALESCE((
                SELECT SUM(COALESCE(fp.principal_paid, 0) + COALESCE(fp.late_fee_paid, 0))
                FROM fee_payments fp
                WHERE fp.school_id = :schoolId
                  AND fp.session_id = :sessionId
                  AND fp.payment_date BETWEEN :startDate AND :endDate
                  AND UPPER(COALESCE(fp.mode, '')) = 'CASH'
              ), 0) AS cashRevenue,
              COALESCE((
                SELECT SUM(COALESCE(fp.principal_paid, 0) + COALESCE(fp.late_fee_paid, 0))
                FROM fee_payments fp
                WHERE fp.school_id = :schoolId
                  AND fp.session_id = :sessionId
                  AND fp.payment_date BETWEEN :startDate AND :endDate
                  AND UPPER(COALESCE(fp.mode, '')) <> 'CASH'
              ), 0) AS bankRevenue,
              COALESCE((
                SELECT SUM(COALESCE(ev.amount, 0))
                FROM expense_vouchers ev
                WHERE ev.school_id = :schoolId
                  AND ev.session_id = :sessionId
                  AND ev.expense_date BETWEEN :startDate AND :endDate
                  AND ev.active = true
                  AND UPPER(COALESCE(ev.payment_mode, '')) = 'CASH'
              ), 0) AS cashExpense,
              COALESCE((
                SELECT SUM(COALESCE(ev.amount, 0))
                FROM expense_vouchers ev
                WHERE ev.school_id = :schoolId
                  AND ev.session_id = :sessionId
                  AND ev.expense_date BETWEEN :startDate AND :endDate
                  AND ev.active = true
                  AND UPPER(COALESCE(ev.payment_mode, '')) <> 'CASH'
              ), 0) AS bankExpense,
              COALESCE((
                SELECT SUM(COALESCE(ft.amount, 0))
                FROM finance_account_transfers ft
                WHERE ft.school_id = :schoolId
                  AND ft.session_id = :sessionId
                  AND ft.transfer_date BETWEEN :startDate AND :endDate
              ), 0) AS transferIn,
              COALESCE((
                SELECT SUM(COALESCE(ft.amount, 0))
                FROM finance_account_transfers ft
                WHERE ft.school_id = :schoolId
                  AND ft.session_id = :sessionId
                  AND ft.transfer_date BETWEEN :startDate AND :endDate
              ), 0) AS transferOut
            """, nativeQuery = true)
    MovementAggregate aggregateMovements(
            @Param("schoolId") Long schoolId,
            @Param("sessionId") Long sessionId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
