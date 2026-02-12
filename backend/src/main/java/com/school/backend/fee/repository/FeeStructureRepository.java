package com.school.backend.fee.repository;

import com.school.backend.fee.entity.FeeStructure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeeStructureRepository extends JpaRepository<FeeStructure, Long> {
    List<FeeStructure> findByClassIdAndSessionIdAndSchoolId(Long classId, Long sessionId, Long schoolId);

    List<FeeStructure> findByFeeTypeIdAndSessionIdAndClassIdIsNull(Long feeTypeId, Long sessionId);
}
