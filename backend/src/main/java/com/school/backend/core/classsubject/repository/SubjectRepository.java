package com.school.backend.core.classsubject.repository;

import com.school.backend.core.classsubject.entity.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    Page<Subject> findByActiveTrue(Pageable pageable);

    boolean existsByNameIgnoreCase(String name);
}
