package com.school.backend.core.student.repository;

import com.school.backend.core.student.entity.StudentEnrollment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, Long> {

  Optional<StudentEnrollment> findFirstByStudentIdAndSessionIdAndActiveTrue(Long studentId, Long sessionId);

  Page<StudentEnrollment> findByClassId(Long classId, Pageable pageable);

  Page<StudentEnrollment> findByClassIdAndSessionId(Long classId, Long sessionId, Pageable pageable);

  Page<StudentEnrollment> findByStudentId(Long studentId, Pageable pageable);

  Page<StudentEnrollment> findBySessionId(Long sessionId, Pageable pageable);

  List<StudentEnrollment> findByStudentIdOrderBySessionIdAsc(Long studentId);

  List<StudentEnrollment> findByStudentIdAndSessionId(Long studentId, Long sessionId);

  List<StudentEnrollment> findByClassIdAndSessionId(Long classId, Long sessionId);

  long countBySchoolIdAndSessionIdAndActiveTrue(Long schoolId, Long sessionId);

  long countByClassIdAndSessionIdAndActiveTrue(Long classId, Long sessionId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      select e from StudentEnrollment e
      where e.studentId = :studentId
        and e.active = true
      """)
  List<StudentEnrollment> findActiveByStudentIdForUpdate(@Param("studentId") Long studentId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      select e from StudentEnrollment e
      where e.studentId = :studentId
        and e.sessionId = :sessionId
        and e.schoolId = :schoolId
      order by e.id desc
      """)
  List<StudentEnrollment> findByStudentIdAndSessionIdAndSchoolIdForUpdate(
      @Param("studentId") Long studentId,
      @Param("sessionId") Long sessionId,
      @Param("schoolId") Long schoolId);

  boolean existsByStudentIdAndSessionIdAndActiveTrue(Long studentId, Long sessionId);

  long countByStudentIdAndSessionIdAndSchoolIdAndActiveTrue(Long studentId, Long sessionId, Long schoolId);

}
