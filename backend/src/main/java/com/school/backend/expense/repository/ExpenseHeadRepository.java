package com.school.backend.expense.repository;

import com.school.backend.expense.entity.ExpenseHead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExpenseHeadRepository extends JpaRepository<ExpenseHead, Long> {
    Optional<ExpenseHead> findByIdAndSchoolId(Long id, Long schoolId);
    List<ExpenseHead> findBySchoolIdOrderByNameAsc(Long schoolId);

    boolean existsBySchoolIdAndNormalizedName(Long schoolId, String normalizedName);
}
