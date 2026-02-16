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
                        "WHERE r.id = :routeId AND r.school.id = :schoolId " +
                        "AND r.currentStrength < r.capacity " +
                        "AND r.active = true")
        int incrementStrengthIfCapacityAvailable(@Param("routeId") Long routeId, @Param("schoolId") Long schoolId);

        /**
         * Atomically decrements route strength (ensures non-negative).
         * Returns 0 if strength already 0 or route not found/mismatched.
         */
        @Modifying
        @Query("UPDATE TransportRoute r " +
                        "SET r.currentStrength = r.currentStrength - 1 " +
                        "WHERE r.id = :routeId " +
                        "AND r.school.id = :schoolId " +
                        "AND r.currentStrength > 0")
        int decrementStrength(@Param("routeId") Long routeId, @Param("schoolId") Long schoolId);
}
