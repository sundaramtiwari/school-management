package com.school.backend.fee.repository;

import com.school.backend.fee.entity.FeeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeeTypeRepository extends JpaRepository<FeeType, Long> {
    Optional<FeeType> findByNameAndSchoolId(String name, Long schoolId);

    boolean existsByNameIgnoreCase(String name);

}
