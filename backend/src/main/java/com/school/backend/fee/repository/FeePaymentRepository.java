package com.school.backend.fee.repository;

import com.school.backend.fee.entity.FeePayment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FeePaymentRepository extends JpaRepository<FeePayment, Long> {
        List<FeePayment> findByStudentId(Long studentId);

        Optional<FeePayment> findTopByStudentIdOrderByPaymentDateDesc(Long studentId);

        @Query("SELECT SUM(f.principalPaid + f.lateFeePaid) FROM FeePayment f WHERE f.schoolId = :schoolId AND f.paymentDate = :date")
        BigDecimal sumTotalPaidBySchoolIdAndPaymentDate(@Param("schoolId") Long schoolId,
                        @Param("date") LocalDate date);

        @Query("SELECT SUM(f.principalPaid + f.lateFeePaid) FROM FeePayment f WHERE f.schoolId = :schoolId")
        BigDecimal sumTotalPaidBySchoolId(@Param("schoolId") Long schoolId);

        @Query("SELECT f FROM FeePayment f WHERE f.schoolId = :schoolId ORDER BY f.paymentDate DESC")
        List<FeePayment> findRecentPayments(@Param("schoolId") Long schoolId, Pageable pageable);

        @Query("""
                            SELECT COALESCE(SUM(p.principalPaid + p.lateFeePaid), 0)
                            FROM FeePayment p
                            WHERE p.schoolId = :schoolId
                              AND p.sessionId = :sessionId
                        """)
        BigDecimal sumTotalPaidBySchoolAndSession(
                        @Param("schoolId") Long schoolId,
                        @Param("sessionId") Long sessionId);

        @Query("""
                            SELECT p.studentId, SUM(p.principalPaid + p.lateFeePaid)
                            FROM FeePayment p
                            WHERE p.schoolId = :schoolId
                              AND p.sessionId = :sessionId
                            GROUP BY p.studentId
                        """)
        List<Object[]> sumPaidGroupedByStudent(
                        @Param("schoolId") Long schoolId,
                        @Param("sessionId") Long sessionId);

        @Query("""
                            SELECT p.studentId, MAX(p.paymentDate)
                            FROM FeePayment p
                            WHERE p.schoolId = :schoolId
                              AND p.sessionId = :sessionId
                            GROUP BY p.studentId
                        """)
        List<Object[]> findLastPaymentDateGroupedByStudent(
                        @Param("schoolId") Long schoolId,
                        @Param("sessionId") Long sessionId);

        @Query("""
                            SELECT p.sessionId, COALESCE(SUM(p.principalPaid + p.lateFeePaid), 0)
                            FROM FeePayment p
                            WHERE p.studentId = :studentId
                            GROUP BY p.sessionId
                        """)
        List<Object[]> sumPaidByStudentGroupedBySession(@Param("studentId") Long studentId);

}
