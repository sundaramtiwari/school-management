package com.school.backend.transport.repository;

import com.school.backend.transport.entity.TransportRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransportRouteRepository extends JpaRepository<TransportRoute, Long> {

    /**
     * Atomically increments route strength if capacity allows.
     * Returns number of rows updated (0 if capacity exceeded or route not found).
     */
    @Modifying
    @Query("UPDATE TransportRoute r " +
            "SET r.currentStrength = r.currentStrength + 1 " +
            "WHERE r.id = :routeId " +
            "AND r.currentStrength < r.capacity " +
            "AND r.active = true")
    int incrementStrengthIfCapacityAvailable(@Param("routeId") Long routeId);

    /**
     * Atomically decrements route strength (ensures non-negative).
     */
    @Modifying
    @Query("UPDATE TransportRoute r " +
            "SET r.currentStrength = CASE WHEN r.currentStrength > 0 " +
            "THEN r.currentStrength - 1 ELSE 0 END " +
            "WHERE r.id = :routeId")
    int decrementStrength(@Param("routeId") Long routeId);
}
