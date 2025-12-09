package com.school.backend.core.guardian.repository;

import com.school.backend.core.guardian.entity.Guardian;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuardianRepository extends JpaRepository<Guardian, Long> {
    Page<Guardian> findBySchoolId(Long schoolId, Pageable pageable);
}
