package com.school.backend.transport.repository;

import com.school.backend.transport.entity.TransportEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransportEnrollmentRepository extends JpaRepository<TransportEnrollment, Long> {
    Optional<TransportEnrollment> findByStudentIdAndSessionId(Long studentId, Long sessionId);

    List<TransportEnrollment> findByPickupPointRouteId(Long routeId);

    long countBySchoolId(Long schoolId);

    long countBySchoolIdAndSessionId(Long schoolId, Long sessionId);
}
