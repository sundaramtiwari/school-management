package com.school.backend.school.repository;

import com.school.backend.school.entity.AcademicSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcademicSessionRepository extends JpaRepository<AcademicSession, Long> {
    List<AcademicSession> findBySchoolId(Long schoolId);

    List<AcademicSession> findBySchoolIdAndActiveTrue(Long schoolId);

    long countByActiveTrue();
}
