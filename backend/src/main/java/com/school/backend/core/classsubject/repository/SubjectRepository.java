package com.school.backend.core.classsubject.repository;

import com.school.backend.core.classsubject.entity.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    Page<Subject> findBySchoolId(Long schoolId, Pageable pageable);

    Page<Subject> findBySchoolIdAndActiveTrue(Long schoolId, Pageable pageable);

    boolean existsByNameIgnoreCaseAndSchoolId(String name, Long schoolId);

    Optional<Subject> findByNameIgnoreCaseAndSchoolId(String name, Long schoolId);
}
