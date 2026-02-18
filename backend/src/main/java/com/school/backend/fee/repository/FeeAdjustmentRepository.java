package com.school.backend.fee.repository;

import com.school.backend.fee.entity.FeeAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeeAdjustmentRepository extends JpaRepository<FeeAdjustment, Long> {
    List<FeeAdjustment> findByAssignmentId(Long assignmentId);
}
