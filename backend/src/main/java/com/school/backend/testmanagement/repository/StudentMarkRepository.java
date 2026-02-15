package com.school.backend.testmanagement.repository;

import com.school.backend.testmanagement.entity.StudentMark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentMarkRepository
        extends JpaRepository<StudentMark, Long> {

    boolean existsByExamSubjectIdAndStudentId(Long examSubjectId, Long studentId);

    Optional<StudentMark> findByExamSubjectIdAndStudentId(Long examSubjectId, Long studentId);

    List<StudentMark> findByExamSubjectIdIn(List<Long> subjectIds);

    long countByExamId(Long examId);
}
