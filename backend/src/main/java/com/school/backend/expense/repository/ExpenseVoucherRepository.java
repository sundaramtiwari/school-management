package com.school.backend.expense.repository;

import com.school.backend.expense.dto.ExpenseHeadTotalDto;
import com.school.backend.expense.entity.ExpenseVoucher;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseVoucherRepository extends JpaRepository<ExpenseVoucher, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ExpenseVoucher> findFirstBySchoolIdAndSessionIdOrderByIdDesc(Long schoolId, Long sessionId);

    List<ExpenseVoucher> findBySchoolIdAndSessionIdAndActiveTrueOrderByExpenseDateDescIdDesc(Long schoolId, Long sessionId);

    List<ExpenseVoucher> findBySchoolIdAndSessionIdAndExpenseDateAndActiveTrueOrderByExpenseDateDescIdDesc(
            Long schoolId,
            Long sessionId,
            LocalDate date);

    @Query("""
            SELECT v
            FROM ExpenseVoucher v
            WHERE v.schoolId = :schoolId
              AND v.sessionId = :sessionId
              AND v.active = true
              AND YEAR(v.expenseDate) = :year
              AND MONTH(v.expenseDate) = :month
            ORDER BY v.expenseDate DESC, v.id DESC
            """)
    List<ExpenseVoucher> findMonthly(
            @Param("schoolId") Long schoolId,
            @Param("sessionId") Long sessionId,
            @Param("year") int year,
            @Param("month") int month);

    @Query("""
            SELECT COALESCE(SUM(v.amount), 0)
            FROM ExpenseVoucher v
            WHERE v.schoolId = :schoolId
              AND v.sessionId = :sessionId
              AND v.active = true
            """)
    BigDecimal sumSessionExpense(@Param("schoolId") Long schoolId, @Param("sessionId") Long sessionId);

    @Query("""
            SELECT COALESCE(SUM(v.amount), 0)
            FROM ExpenseVoucher v
            WHERE v.schoolId = :schoolId
              AND v.sessionId = :sessionId
              AND v.expenseDate = :date
              AND v.active = true
              AND v.paymentMode = :paymentMode
            """)
    BigDecimal sumExpenseBySchoolSessionDateAndMode(
            @Param("schoolId") Long schoolId,
            @Param("sessionId") Long sessionId,
            @Param("date") LocalDate date,
            @Param("paymentMode") com.school.backend.common.enums.ExpensePaymentMode paymentMode);

    @Query("""
            SELECT new com.school.backend.expense.dto.ExpenseHeadTotalDto(
                v.expenseHead.id,
                v.expenseHead.name,
                COALESCE(SUM(v.amount), 0)
            )
            FROM ExpenseVoucher v
            WHERE v.schoolId = :schoolId
              AND v.sessionId = :sessionId
              AND v.active = true
            GROUP BY v.expenseHead.id, v.expenseHead.name
            ORDER BY v.expenseHead.name ASC
            """)
    List<ExpenseHeadTotalDto> sumSessionExpenseGroupedByHead(
            @Param("schoolId") Long schoolId,
            @Param("sessionId") Long sessionId);

    @Query("""
            SELECT new com.school.backend.expense.dto.ExpenseHeadTotalDto(
                v.expenseHead.id,
                v.expenseHead.name,
                COALESCE(SUM(v.amount), 0)
            )
            FROM ExpenseVoucher v
            WHERE v.schoolId = :schoolId
              AND v.sessionId = :sessionId
              AND v.expenseDate = :date
              AND v.active = true
              AND v.paymentMode = :paymentMode
            GROUP BY v.expenseHead.id, v.expenseHead.name
            ORDER BY v.expenseHead.name ASC
            """)
    List<ExpenseHeadTotalDto> sumExpenseByHeadForDateAndMode(
            @Param("schoolId") Long schoolId,
            @Param("sessionId") Long sessionId,
            @Param("date") LocalDate date,
            @Param("paymentMode") com.school.backend.common.enums.ExpensePaymentMode paymentMode);

    long countBySchoolIdAndSessionIdAndActiveTrue(Long schoolId, Long sessionId);

    Optional<ExpenseVoucher> findByIdAndSchoolId(Long id, Long schoolId);
}
