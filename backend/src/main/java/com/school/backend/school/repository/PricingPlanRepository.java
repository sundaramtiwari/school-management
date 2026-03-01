package com.school.backend.school.repository;

import com.school.backend.school.entity.PricingPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PricingPlanRepository extends JpaRepository<PricingPlan, Long> {
    Optional<PricingPlan> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
