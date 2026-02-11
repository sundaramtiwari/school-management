package com.school.backend.school.repository;

import com.school.backend.school.entity.AcademicSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AcademicSessionRepository extends JpaRepository<AcademicSession, Long> {
    List<AcademicSession> findBySchoolId(Long schoolId);

    List<AcademicSession> findBySchoolIdAndActiveTrue(Long schoolId);

    Optional<AcademicSession> findBySchoolIdAndCurrentTrue(Long schoolId);
}
