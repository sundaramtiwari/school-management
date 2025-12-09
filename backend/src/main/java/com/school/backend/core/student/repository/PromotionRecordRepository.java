package com.school.backend.core.student.repository;

import com.school.backend.core.student.entity.PromotionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionRecordRepository extends JpaRepository<PromotionRecord, Long> {
    Page<PromotionRecord> findByStudentId(Long studentId, Pageable pageable);

    Page<PromotionRecord> findBySession(String session, Pageable pageable);
}
