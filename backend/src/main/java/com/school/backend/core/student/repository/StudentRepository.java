package com.school.backend.core.student.repository;

import com.school.backend.core.student.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

  @Query("""
      select s from Student s
      join StudentEnrollment e on s.id = e.studentId
      where e.classId = :classId
        and e.sessionId = :sessionId""")
  Page<Student> findByClassIdAndSessionId(@Param("classId") Long classId,
      @Param("sessionId") Long sessionId,
      Pageable pageable);

  Page<Student> findBySchoolId(Long schoolId, Pageable pageable);

  @Query("""
      select distinct s from Student s
      join StudentEnrollment e on s.id = e.studentId
      where s.schoolId = :schoolId
        and e.sessionId = :sessionId""")
  Page<Student> findBySchoolIdAndSessionId(@Param("schoolId") Long schoolId,
      @Param("sessionId") Long sessionId,
      Pageable pageable);

  @Query("""
      select count(distinct s.id) from Student s
      join StudentEnrollment e on s.id = e.studentId
      where s.schoolId = :schoolId
        and e.sessionId = :sessionId""")
  long countBySchoolIdAndSessionId(@Param("schoolId") Long schoolId, @Param("sessionId") Long sessionId);

  Page<Student> findBySchoolIdAndCurrentStatus(Long schoolId, String status, Pageable pageable);

  Optional<Student> findByAdmissionNumberAndSchoolId(String admissionNumber, Long schoolId);

  Optional<Student> findByIdAndSchoolId(Long id, Long schoolId);

  boolean existsByAdmissionNumberAndSchoolId(String admissionNumber, Long schoolId);

  @Query("""
      SELECT COUNT(DISTINCT s.id)
      FROM Student s
      JOIN StudentEnrollment e ON s.id = e.studentId
      WHERE s.schoolId = :schoolId
        AND e.sessionId = :sessionId
        AND (:classId IS NULL OR e.classId = :classId)
        AND (:search IS NULL OR LOWER(s.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(s.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(s.admissionNumber) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (
             (SELECT COALESCE(SUM(a.amount), 0) FROM StudentFeeAssignment a WHERE a.studentId = s.id AND a.sessionId = :sessionId AND a.active = true)
             + (SELECT COALESCE(SUM(a.lateFeeAccrued), 0) FROM StudentFeeAssignment a WHERE a.studentId = s.id AND a.sessionId = :sessionId AND a.active = true)
             - (SELECT COALESCE(SUM(a.totalDiscountAmount), 0) FROM StudentFeeAssignment a WHERE a.studentId = s.id AND a.sessionId = :sessionId AND a.active = true)
             - (SELECT COALESCE(SUM(a.sponsorCoveredAmount), 0) FROM StudentFeeAssignment a WHERE a.studentId = s.id AND a.sessionId = :sessionId AND a.active = true)
             - (SELECT COALESCE(SUM(a.lateFeeWaived), 0) FROM StudentFeeAssignment a WHERE a.studentId = s.id AND a.sessionId = :sessionId AND a.active = true)
             - (SELECT COALESCE(SUM(a.principalPaid), 0) FROM StudentFeeAssignment a WHERE a.studentId = s.id AND a.sessionId = :sessionId AND a.active = true)
             - (SELECT COALESCE(SUM(a.lateFeePaid), 0) FROM StudentFeeAssignment a WHERE a.studentId = s.id AND a.sessionId = :sessionId AND a.active = true)
            ) >= :minAmountDue
        AND (:maxPaymentDate IS NULL OR COALESCE((SELECT MAX(p.paymentDate) FROM FeePayment p WHERE p.studentId = s.id AND p.sessionId = :sessionId), :sessionStart) <= :maxPaymentDate)
      """)
  long countDefaulters(
      @Param("schoolId") Long schoolId,
      @Param("sessionId") Long sessionId,
      @Param("classId") Long classId,
      @Param("search") String search,
      @Param("minAmountDue") BigDecimal minAmountDue,
      @Param("maxPaymentDate") LocalDate maxPaymentDate,
      @Param("sessionStart") LocalDate sessionStart);

  @Query("""
      SELECT s.id, s.firstName, s.lastName, s.admissionNumber, s.contactNumber, c.name, c.section,
             (SELECT COALESCE(SUM(a.amount), 0) FROM StudentFeeAssignment a WHERE a.studentId = s.id AND a.sessionId = :sessionId AND a.active = true),
             (SELECT COALESCE(SUM(a.lateFeeAccrued), 0) FROM StudentFeeAssignment a WHERE a.studentId = s.id AND a.sessionId = :sessionId AND a.active = true),
             (SELECT COALESCE(SUM(a.totalDiscountAmount), 0) FROM StudentFeeAssignment a WHERE a.studentId = s.id AND a.sessionId = :sessionId AND a.active = true),
             (SELECT COALESCE(SUM(a.sponsorCoveredAmount), 0) FROM StudentFeeAssignment a WHERE a.studentId = s.id AND a.sessionId = :sessionId AND a.active = true),
             (SELECT COALESCE(SUM(a.lateFeeWaived), 0) FROM StudentFeeAssignment a WHERE a.studentId = s.id AND a.sessionId = :sessionId AND a.active = true),
             (SELECT COALESCE(SUM(a.principalPaid), 0) FROM StudentFeeAssignment a WHERE a.studentId = s.id AND a.sessionId = :sessionId AND a.active = true),
             (SELECT COALESCE(SUM(a.lateFeePaid), 0) FROM StudentFeeAssignment a WHERE a.studentId = s.id AND a.sessionId = :sessionId AND a.active = true),
             (SELECT MAX(p.paymentDate) FROM FeePayment p WHERE p.studentId = s.id AND p.sessionId = :sessionId)
      FROM Student s
      JOIN StudentEnrollment e ON s.id = e.studentId
      LEFT JOIN SchoolClass c ON e.classId = c.id
      WHERE s.schoolId = :schoolId
        AND e.sessionId = :sessionId
        AND (:classId IS NULL OR e.classId = :classId)
        AND (:search IS NULL OR LOWER(s.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(s.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(s.admissionNumber) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (
             (SELECT COALESCE(SUM(a.amount), 0) FROM StudentFeeAssignment a WHERE a.studentId = s.id AND a.sessionId = :sessionId AND a.active = true)
             + (SELECT COALESCE(SUM(a.lateFeeAccrued), 0) FROM StudentFeeAssignment a WHERE a.studentId = s.id AND a.sessionId = :sessionId AND a.active = true)
             - (SELECT COALESCE(SUM(a.totalDiscountAmount), 0) FROM StudentFeeAssignment a WHERE a.studentId = s.id AND a.sessionId = :sessionId AND a.active = true)
             - (SELECT COALESCE(SUM(a.sponsorCoveredAmount), 0) FROM StudentFeeAssignment a WHERE a.studentId = s.id AND a.sessionId = :sessionId AND a.active = true)
             - (SELECT COALESCE(SUM(a.lateFeeWaived), 0) FROM StudentFeeAssignment a WHERE a.studentId = s.id AND a.sessionId = :sessionId AND a.active = true)
             - (SELECT COALESCE(SUM(a.principalPaid), 0) FROM StudentFeeAssignment a WHERE a.studentId = s.id AND a.sessionId = :sessionId AND a.active = true)
             - (SELECT COALESCE(SUM(a.lateFeePaid), 0) FROM StudentFeeAssignment a WHERE a.studentId = s.id AND a.sessionId = :sessionId AND a.active = true)
            ) >= :minAmountDue
        AND (:maxPaymentDate IS NULL OR COALESCE((SELECT MAX(p.paymentDate) FROM FeePayment p WHERE p.studentId = s.id AND p.sessionId = :sessionId), :sessionStart) <= :maxPaymentDate)
      """)
  Page<Object[]> findDefaulterDetails(
      @Param("schoolId") Long schoolId,
      @Param("sessionId") Long sessionId,
      @Param("classId") Long classId,
      @Param("search") String search,
      @Param("minAmountDue") BigDecimal minAmountDue,
      @Param("maxPaymentDate") LocalDate maxPaymentDate,
      @Param("sessionStart") LocalDate sessionStart,
      Pageable pageable);

}
