package com.school.backend.fee.repository;

import com.school.backend.fee.entity.DiscountDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiscountDefinitionRepository extends JpaRepository<DiscountDefinition, Long> {
    List<DiscountDefinition> findBySchoolIdAndActiveTrue(Long schoolId);
}
