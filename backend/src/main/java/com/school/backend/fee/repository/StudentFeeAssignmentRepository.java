package com.school.backend.fee.repository;

import com.school.backend.fee.entity.StudentFeeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface StudentFeeAssignmentRepository
        extends JpaRepository<StudentFeeAssignment, Long> {

    /**
     * Find fee assignment for student, structure, and session.
     * Used for checking if assignment exists and reactivating if needed.
     *
     * @param studentId      Student ID
     * @param feeStructureId Fee Structure ID
     * @param sessionId      Session ID
     * @return Optional containing assignment if found
     */
    Optional<StudentFeeAssignment> findByStudentIdAndFeeStructureIdAndSessionId(
            Long studentId, Long feeStructureId, Long sessionId);

    boolean existsByStudentIdAndFeeStructureIdAndSessionId(
            Long studentId,
            Long feeStructureId,
            Long sessionId);

    boolean existsByStudentIdAndFeeStructureId(Long studentId, Long feeStructureId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM StudentFeeAssignment a WHERE a.studentId = :studentId AND a.sessionId = :sessionId AND a.active = true")
    List<StudentFeeAssignment> findByStudentIdAndSessionIdWithLock(
            @Param("studentId") Long studentId,
            @Param("sessionId") Long sessionId);

    List<StudentFeeAssignment> findByStudentIdAndSessionId(Long studentId, Long sessionId);

    List<StudentFeeAssignment> findByStudentId(Long studentId);

    @Query("""
                SELECT COALESCE(SUM(a.amount), 0)
                FROM StudentFeeAssignment a
                WHERE a.schoolId = :schoolId
                  AND a.sessionId = :sessionId
                  AND a.active = true
            """)
    BigDecimal sumTotalAssignedBySchoolAndSession(
            @Param("schoolId") Long schoolId,
            @Param("sessionId") Long sessionId);

    @Query("""
                SELECT a.studentId, SUM(a.amount), SUM(a.lateFeeAccrued)
                FROM StudentFeeAssignment a
                WHERE a.schoolId = :schoolId
                  AND a.sessionId = :sessionId
                  AND a.active = true
                GROUP BY a.studentId
            """)
    List<Object[]> sumAssignedGroupedByStudent(
            @Param("schoolId") Long schoolId,
            @Param("sessionId") Long sessionId);

    @Query("""
                SELECT a.sessionId,
                       COALESCE(SUM(a.amount), 0),
                       COALESCE(SUM(a.totalDiscountAmount), 0),
                       COALESCE(SUM(a.sponsorCoveredAmount), 0),
                       COALESCE(SUM(a.lateFeeAccrued), 0),
                       COALESCE(SUM(a.lateFeePaid), 0),
                       COALESCE(SUM(a.lateFeeWaived), 0),
                       COALESCE(SUM(a.principalPaid), 0)
                FROM StudentFeeAssignment a
                WHERE a.studentId = :studentId
                GROUP BY a.sessionId
            """)
    List<Object[]> sumFinancialSummaryByStudentGroupedBySession(@Param("studentId") Long studentId);

}
