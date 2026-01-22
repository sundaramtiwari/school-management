package com.school.backend.testmanagement.repository;

import com.school.backend.testmanagement.entity.GradePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GradePolicyRepository
        extends JpaRepository<GradePolicy, Long> {

    List<GradePolicy> findBySchoolIdOrderByMinPercentDesc(Long schoolId);
}
