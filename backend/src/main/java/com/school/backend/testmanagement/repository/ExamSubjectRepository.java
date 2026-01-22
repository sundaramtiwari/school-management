package com.school.backend.testmanagement.repository;

import com.school.backend.testmanagement.entity.ExamSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamSubjectRepository
        extends JpaRepository<ExamSubject, Long> {

    boolean existsByExamIdAndSubjectId(Long examId, Long subjectId);
}
