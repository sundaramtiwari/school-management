package com.school.backend.fee.repository;

import com.school.backend.fee.entity.FeeAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeeAdjustmentRepository extends JpaRepository<FeeAdjustment, Long> {
    List<FeeAdjustment> findByAssignmentId(Long assignmentId);

    List<FeeAdjustment> findByAssignmentIdOrderByCreatedAtAsc(Long assignmentId);

    boolean existsByAssignmentIdAndDiscountDefinitionIdAndType(
            Long assignmentId,
            Long discountDefinitionId,
            FeeAdjustment.AdjustmentType type);
}
