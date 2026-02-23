package com.school.backend.fee.repository;

import com.school.backend.fee.entity.FeeStructure;
import com.school.backend.common.enums.FeeFrequency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface FeeStructureRepository extends JpaRepository<FeeStructure, Long> {
    List<FeeStructure> findByClassIdAndSessionIdAndSchoolId(Long classId, Long sessionId, Long schoolId);
    List<FeeStructure> findByClassIdAndSessionIdAndSchoolIdAndActiveTrue(Long classId, Long sessionId, Long schoolId);

    Optional<FeeStructure> findByIdAndSchoolId(Long id, Long schoolId);

    List<FeeStructure> findByFeeTypeIdAndSessionIdAndClassIdIsNull(Long feeTypeId, Long sessionId);

    Optional<FeeStructure> findByFeeTypeIdAndSessionIdAndClassIdIsNullAndSchoolIdAndAmountAndFrequency(
            Long feeTypeId,
            Long sessionId,
            Long schoolId,
            BigDecimal amount,
            FeeFrequency frequency);

    @Query(value = "SELECT COUNT(1) > 0 FROM fee_structures WHERE id = :id", nativeQuery = true)
    boolean existsAnyById(@Param("id") Long id);
}
