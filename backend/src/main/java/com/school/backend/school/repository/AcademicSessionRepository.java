package com.school.backend.school.repository;

import com.school.backend.school.entity.AcademicSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AcademicSessionRepository extends JpaRepository<AcademicSession, Long> {
    List<AcademicSession> findBySchoolId(Long schoolId);

    List<AcademicSession> findBySchoolIdAndActiveTrue(Long schoolId);

    Optional<AcademicSession> findFirstBySchoolIdAndActiveTrueOrderByStartDateDesc(Long schoolId);

    long countByActiveTrue();

    boolean existsBySchoolIdAndName(Long schoolId, String name);

    boolean existsByIdAndSchoolId(Long id, Long schoolId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AcademicSession> findByIdAndSchoolId(Long id, Long schoolId);

    @Modifying
    @Query("""
            UPDATE AcademicSession s
               SET s.active = false
             WHERE s.schoolId = :schoolId
               AND s.id <> :activeSessionId
               AND s.active = true
            """)
    int deactivateOtherActiveSessions(@Param("schoolId") Long schoolId, @Param("activeSessionId") Long activeSessionId);
}
