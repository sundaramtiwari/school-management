package com.school.backend.core.classsubject.repository;

import com.school.backend.core.classsubject.entity.SchoolClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {

    Page<SchoolClass> findBySchoolId(Long schoolId, Pageable pageable);

    Page<SchoolClass> findBySchoolIdAndSession(Long schoolId, String session, Pageable pageable);

    Optional<SchoolClass> findByIdAndSchoolId(Long id, Long schoolId);

    boolean existsByNameAndSectionAndSessionAndSchoolId(String name, String section, String session, Long schoolId);

    Page<SchoolClass> findByClassTeacherIdAndSchoolId(Long teacherId, Long schoolId, Pageable pageable);
}
