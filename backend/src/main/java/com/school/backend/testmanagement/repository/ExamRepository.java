package com.school.backend.testmanagement.repository;

import com.school.backend.testmanagement.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    List<Exam> findByClassIdAndSessionId(Long classId, Long sessionId);

    List<Exam> findBySchoolIdAndStartDateAfter(Long schoolId, java.time.LocalDate date);

    List<Exam> findBySchoolIdAndSessionIdAndStartDateAfter(Long schoolId, Long sessionId, java.time.LocalDate date);

    @Query("""
            select count(e) from Exam e
            where e.sessionId = :sessionId
              and e.classId = :classId
              and e.status <> com.school.backend.common.enums.ExamStatus.LOCKED
            """)
    long countNonLockedBySessionIdAndClassId(@Param("sessionId") Long sessionId, @Param("classId") Long classId);
}
