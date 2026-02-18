package com.school.backend.fee.repository;

import com.school.backend.fee.entity.LateFeeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LateFeeLogRepository extends JpaRepository<LateFeeLog, Long> {
    List<LateFeeLog> findByAssignmentId(Long assignmentId);
}
