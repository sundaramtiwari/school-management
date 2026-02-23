package com.school.backend.fee.repository;

import com.school.backend.fee.entity.FeeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FeeTypeRepository extends JpaRepository<FeeType, Long> {
    Optional<FeeType> findByNameAndSchoolId(String name, Long schoolId);

    Optional<FeeType> findByIdAndSchoolId(Long id, Long schoolId);

    boolean existsByNameIgnoreCaseAndSchoolId(String name, Long schoolId);

    @Query(value = "SELECT COUNT(1) > 0 FROM fee_types WHERE id = :id", nativeQuery = true)
    boolean existsAnyById(@Param("id") Long id);

}
