package com.school.backend.testmanagement.repository;

import com.school.backend.testmanagement.entity.ExamSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamSubjectRepository
        extends JpaRepository<ExamSubject, Long> {

    boolean existsByExamIdAndSubjectId(Long examId, Long subjectId);

    List<ExamSubject> findByExamId(Long examId);

    long countByExamId(Long examId);

    boolean existsByExamIdAndMaxMarksLessThanEqual(Long examId, int maxMarks);
}
