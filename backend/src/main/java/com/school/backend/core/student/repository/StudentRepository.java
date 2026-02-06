package com.school.backend.core.student.repository;

import com.school.backend.core.student.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    @Query("""
            select s from Student s
            join StudentEnrollment e on s.id = e.studentId
            where e.classId = :classId""")
    Page<Student> findByClassId(@Param("classId") Long classId, Pageable pageable);

    Page<Student> findBySchoolId(Long schoolId, Pageable pageable);

    Page<Student> findBySchoolIdAndCurrentStatus(Long schoolId, String status, Pageable pageable);

    Optional<Student> findByAdmissionNumberAndSchoolId(String admissionNumber, Long schoolId);

    Optional<Student> findByIdAndSchoolId(Long id, Long schoolId);

    boolean existsByAdmissionNumberAndSchoolId(String admissionNumber, Long schoolId);

    long countBySchoolId(Long schoolId);
}
