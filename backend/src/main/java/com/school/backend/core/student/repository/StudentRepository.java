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

}
