package com.school.backend.fee.repository;

import com.school.backend.fee.entity.FeeStructure;
import com.school.backend.common.enums.FeeFrequency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface FeeStructureRepository extends JpaRepository<FeeStructure, Long> {
    List<FeeStructure> findByClassIdAndSessionIdAndSchoolId(Long classId, Long sessionId, Long schoolId);

    List<FeeStructure> findByFeeTypeIdAndSessionIdAndClassIdIsNull(Long feeTypeId, Long sessionId);

    Optional<FeeStructure> findByFeeTypeIdAndSessionIdAndClassIdIsNullAndSchoolIdAndAmountAndFrequency(
            Long feeTypeId,
            Long sessionId,
            Long schoolId,
            BigDecimal amount,
            FeeFrequency frequency);
}
