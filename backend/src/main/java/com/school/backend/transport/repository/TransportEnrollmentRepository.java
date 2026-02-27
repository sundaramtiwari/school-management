package com.school.backend.transport.repository;

import com.school.backend.transport.entity.TransportEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransportEnrollmentRepository extends JpaRepository<TransportEnrollment, Long> {
    Optional<TransportEnrollment> findByStudentIdAndSessionIdAndSchoolId(Long studentId, Long sessionId, Long schoolId);

    Optional<TransportEnrollment> findByStudentIdAndSessionIdAndSchoolIdAndActiveTrue(
            Long studentId,
            Long sessionId,
            Long schoolId);

    boolean existsByStudentIdAndSessionIdAndSchoolIdAndActiveTrue(Long studentId, Long sessionId, Long schoolId);

    List<TransportEnrollment> findByStudentIdInAndSessionIdAndActiveTrue(Collection<Long> studentIds, Long sessionId);

    List<TransportEnrollment> findByPickupPointRouteId(Long routeId);

    @Modifying
    @Query("UPDATE TransportEnrollment e SET e.active = false " +
            "WHERE e.id = :id AND e.active = true AND e.schoolId = :schoolId")
    int deactivateEnrollment(@Param("id") Long id, @Param("schoolId") Long schoolId);

    long countBySchoolId(Long schoolId);

    long countBySchoolIdAndSessionId(Long schoolId, Long sessionId);
}
