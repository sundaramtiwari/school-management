package com.school.backend.fee.repository;

import com.school.backend.fee.dto.FeeTypeHeadSummaryDto;
import com.school.backend.fee.entity.FeePaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FeePaymentAllocationRepository extends JpaRepository<FeePaymentAllocation, Long> {

        List<FeePaymentAllocation> findByFeePaymentIdOrderByIdAsc(Long feePaymentId);

        @Query("""
                        SELECT new com.school.backend.fee.dto.FeeTypeHeadSummaryDto(
                            ft.id,
                            ft.name,
                            COALESCE(SUM(a.principalAmount), 0),
                            COALESCE(SUM(a.lateFeeAmount), 0),
                            COALESCE(SUM(a.principalAmount + a.lateFeeAmount), 0)
                        )
                        FROM FeePaymentAllocation a
                        JOIN a.feeType ft
                        JOIN FeePayment p ON p.id = a.feePaymentId
                        WHERE a.schoolId = :schoolId
                          AND p.paymentDate = :paymentDate
                        GROUP BY ft.id, ft.name
                        ORDER BY ft.name ASC
                        """)
        List<FeeTypeHeadSummaryDto> findHeadSummaryBySchoolIdAndDate(
                        @Param("schoolId") Long schoolId,
                        @Param("paymentDate") LocalDate paymentDate);

        @Query("""
                        SELECT new com.school.backend.fee.dto.FeeTypeHeadSummaryDto(
                            ft.id,
                            ft.name,
                            COALESCE(SUM(a.principalAmount), 0),
                            COALESCE(SUM(a.lateFeeAmount), 0),
                            COALESCE(SUM(a.principalAmount + a.lateFeeAmount), 0)
                        )
                        FROM FeePaymentAllocation a
                        JOIN a.feeType ft
                        JOIN FeePayment p ON p.id = a.feePaymentId
                        WHERE a.schoolId = :schoolId
                          AND p.paymentDate = :paymentDate
                          AND UPPER(COALESCE(p.mode, '')) = UPPER(:mode)
                        GROUP BY ft.id, ft.name
                        ORDER BY ft.name ASC
                        """)
        List<FeeTypeHeadSummaryDto> findHeadSummaryBySchoolIdDateAndMode(
                        @Param("schoolId") Long schoolId,
                        @Param("paymentDate") LocalDate paymentDate,
                        @Param("mode") String mode);

        @Query("""
                        SELECT new com.school.backend.fee.dto.FeeTypeHeadSummaryDto(
                            ft.id,
                            ft.name,
                            COALESCE(SUM(a.principalAmount), 0),
                            COALESCE(SUM(a.lateFeeAmount), 0),
                            COALESCE(SUM(a.principalAmount + a.lateFeeAmount), 0)
                        )
                        FROM FeePaymentAllocation a
                        JOIN a.feeType ft
                        JOIN FeePayment p ON p.id = a.feePaymentId
                        WHERE a.schoolId = :schoolId
                          AND a.sessionId = :sessionId
                          AND p.paymentDate = :paymentDate
                        GROUP BY ft.id, ft.name
                        ORDER BY ft.name ASC
                        """)
        List<FeeTypeHeadSummaryDto> findHeadSummaryBySchoolSessionAndDate(
                        @Param("schoolId") Long schoolId,
                        @Param("sessionId") Long sessionId,
                        @Param("paymentDate") LocalDate paymentDate);

        @Query("""
                        SELECT new com.school.backend.fee.dto.FeeTypeHeadSummaryDto(
                            ft.id,
                            ft.name,
                            COALESCE(SUM(a.principalAmount), 0),
                            COALESCE(SUM(a.lateFeeAmount), 0),
                            COALESCE(SUM(a.principalAmount + a.lateFeeAmount), 0)
                        )
                        FROM FeePaymentAllocation a
                        JOIN a.feeType ft
                        JOIN FeePayment p ON p.id = a.feePaymentId
                        WHERE a.schoolId = :schoolId
                          AND a.sessionId = :sessionId
                          AND p.paymentDate = :paymentDate
                          AND UPPER(COALESCE(p.mode, '')) = UPPER(:mode)
                        GROUP BY ft.id, ft.name
                        ORDER BY ft.name ASC
                        """)
        List<FeeTypeHeadSummaryDto> findHeadSummaryBySchoolSessionDateAndMode(
                        @Param("schoolId") Long schoolId,
                        @Param("sessionId") Long sessionId,
                        @Param("paymentDate") LocalDate paymentDate,
                        @Param("mode") String mode);
}
