package com.school.backend.fee.repository;

import com.school.backend.fee.entity.LateFeePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LateFeePolicyRepository extends JpaRepository<LateFeePolicy, Long> {
    Optional<LateFeePolicy> findByFeeStructureId(Long feeStructureId);
}
