package com.school.backend.transport.repository;

import com.school.backend.transport.entity.PickupPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PickupPointRepository extends JpaRepository<PickupPoint, Long> {
    List<PickupPoint> findByRouteId(Long routeId);
}
