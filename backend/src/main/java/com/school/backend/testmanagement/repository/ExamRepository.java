package com.school.backend.testmanagement.repository;

import com.school.backend.testmanagement.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    List<Exam> findByClassIdAndSession(Long classId, String session);

    List<Exam> findBySchoolIdAndStartDateAfter(Long schoolId, java.time.LocalDate date);
}
