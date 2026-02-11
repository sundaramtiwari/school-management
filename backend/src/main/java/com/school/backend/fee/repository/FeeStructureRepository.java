package com.school.backend.fee.repository;

import com.school.backend.fee.entity.FeeStructure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeeStructureRepository extends JpaRepository<FeeStructure, Long> {
    List<FeeStructure> findByClassIdAndSessionAndSchoolId(Long classId, String session, Long schoolId);

    List<FeeStructure> findByFeeTypeIdAndSessionAndClassIdIsNull(Long feeTypeId, String session);
}
