package com.school.backend.fee.repository;

import com.school.backend.fee.entity.StudentFeeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    List<StudentFeeAssignment> findByStudentIdAndSessionId(Long studentId, Long sessionId);

    @Query("""
                SELECT COALESCE(SUM(a.amount), 0)
                FROM StudentFeeAssignment a
                WHERE a.schoolId = :schoolId
                  AND a.sessionId = :sessionId
                  AND a.active = true
            """)
    Long sumTotalAssignedBySchoolAndSession(
            @Param("schoolId") Long schoolId,
            @Param("sessionId") Long sessionId);

    @Query("""
                SELECT a.studentId, SUM(a.amount)
                FROM StudentFeeAssignment a
                WHERE a.schoolId = :schoolId
                  AND a.sessionId = :sessionId
                  AND a.active = true
                GROUP BY a.studentId
            """)
    List<Object[]> sumAssignedGroupedByStudent(
            @Param("schoolId") Long schoolId,
            @Param("sessionId") Long sessionId);

}
