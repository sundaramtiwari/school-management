package com.school.backend.fee.repository;

import com.school.backend.fee.entity.DiscountDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DiscountDefinitionRepository extends JpaRepository<DiscountDefinition, Long> {
    List<DiscountDefinition> findBySchoolIdAndActiveTrue(Long schoolId);

    Optional<DiscountDefinition> findByIdAndSchoolId(Long id, Long schoolId);

    @Query(value = "SELECT COUNT(1) > 0 FROM discount_definitions WHERE id = :id", nativeQuery = true)
    boolean existsAnyById(@Param("id") Long id);
}
