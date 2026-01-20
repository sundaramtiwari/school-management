package com.school.backend.core.student.repository;

import com.school.backend.core.student.entity.StudentEnrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, Long> {

    Page<StudentEnrollment> findByClassId(Long classId, Pageable pageable);

    Page<StudentEnrollment> findByStudentId(Long studentId, Pageable pageable);

    Page<StudentEnrollment> findBySession(String session, Pageable pageable);

    List<StudentEnrollment> findByStudentIdOrderBySessionAsc(Long studentId);

}
