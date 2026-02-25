package com.school.backend.core.classsubject.repository;

import com.school.backend.core.classsubject.entity.SchoolClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {

    Page<SchoolClass> findByActiveTrue(Pageable pageable);

    Page<SchoolClass> findBySchoolId(Long schoolId, Pageable pageable);
    Page<SchoolClass> findBySchoolIdAndActiveTrue(Long schoolId, Pageable pageable);

    Page<SchoolClass> findBySchoolIdAndSessionId(Long schoolId, Long sessionId, Pageable pageable);
    Page<SchoolClass> findBySchoolIdAndSessionIdAndActiveTrue(Long schoolId, Long sessionId, Pageable pageable);

    Optional<SchoolClass> findByIdAndSchoolId(Long id, Long schoolId);
    Optional<SchoolClass> findByIdAndSchoolIdAndActiveTrue(Long id, Long schoolId);

    boolean existsByNameAndSectionAndSessionIdAndSchoolId(String name, String section, Long sessionId, Long schoolId);
    Optional<SchoolClass> findByNameAndSectionAndSessionIdAndSchoolId(String name, String section, Long sessionId, Long schoolId);

    Page<SchoolClass> findByClassTeacherIdAndSchoolId(Long teacherId, Long schoolId, Pageable pageable);
    Page<SchoolClass> findByClassTeacherIdAndSchoolIdAndActiveTrue(Long teacherId, Long schoolId, Pageable pageable);

    long countBySchoolIdAndSessionId(Long schoolId, Long sessionId);
    long countBySchoolIdAndSessionIdAndActiveTrue(Long schoolId, Long sessionId);

    boolean existsByIdAndSchoolId(Long id, Long schoolId);
}
