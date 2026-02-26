package com.school.backend.finance.repository;

import com.school.backend.finance.entity.DayClosing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DayClosingRepository extends JpaRepository<DayClosing, Long> {
    Optional<DayClosing> findBySchoolIdAndDate(Long schoolId, LocalDate date);

    boolean existsBySchoolIdAndDate(Long schoolId, LocalDate date);

    boolean existsBySchoolIdAndDateAndOverrideAllowedFalse(Long schoolId, LocalDate date);

    Optional<DayClosing> findFirstBySchoolIdAndDateLessThanOrderByDateDesc(Long schoolId, LocalDate date);
}
