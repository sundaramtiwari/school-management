package com.school.backend.finance.repository;

import com.school.backend.finance.entity.FinanceAccountTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FinanceAccountTransferRepository extends JpaRepository<FinanceAccountTransfer, Long> {
    List<FinanceAccountTransfer> findBySchoolIdAndSessionIdAndTransferDateBetween(
            Long schoolId,
            Long sessionId,
            LocalDate fromDate,
            LocalDate toDate);
}
