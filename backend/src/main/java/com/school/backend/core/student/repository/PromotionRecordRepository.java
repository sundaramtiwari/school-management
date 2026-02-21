package com.school.backend.core.student.repository;

import com.school.backend.core.student.entity.PromotionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromotionRecordRepository extends JpaRepository<PromotionRecord, Long> {

    List<PromotionRecord> findByStudentIdOrderByPromotedAtAsc(Long studentId);

    List<PromotionRecord> findByStudentIdAndSchoolIdOrderByPromotedAtAsc(Long studentId, Long schoolId);
}
