package com.school.backend.testmanagement.repository;

import com.school.backend.testmanagement.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    List<Exam> findByClassIdAndSessionId(Long classId, Long sessionId);

    List<Exam> findBySchoolIdAndStartDateAfter(Long schoolId, java.time.LocalDate date);

    List<Exam> findBySchoolIdAndSessionIdAndStartDateAfter(Long schoolId, Long sessionId, java.time.LocalDate date);
}
