package com.school.backend.fee.repository;

import com.school.backend.fee.entity.FeeType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeeTypeRepository extends JpaRepository<FeeType, Long> {
    boolean existsByNameIgnoreCase(String name);

}
