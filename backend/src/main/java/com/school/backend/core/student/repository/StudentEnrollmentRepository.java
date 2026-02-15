package com.school.backend.core.student.repository;

import com.school.backend.core.student.entity.StudentEnrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, Long> {

    Page<StudentEnrollment> findByClassId(Long classId, Pageable pageable);

    Page<StudentEnrollment> findByStudentId(Long studentId, Pageable pageable);

    Page<StudentEnrollment> findBySessionId(Long sessionId, Pageable pageable);

    List<StudentEnrollment> findByStudentIdOrderBySessionIdAsc(Long studentId);

    List<StudentEnrollment> findByStudentIdAndSessionId(Long studentId, Long sessionId);

    List<StudentEnrollment> findByClassIdAndSessionId(Long classId, Long sessionId);

    long countBySchoolIdAndSessionIdAndActiveTrue(Long schoolId, Long sessionId);

}
