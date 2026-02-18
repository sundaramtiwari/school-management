package com.school.backend.core.guardian.repository;

import com.school.backend.core.guardian.entity.Guardian;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GuardianRepository extends JpaRepository<Guardian, Long> {
    Page<Guardian> findBySchoolId(Long schoolId, Pageable pageable);

    Optional<Guardian> findBySchoolIdAndContactNumber(Long schoolId, String contactNumber);
}
