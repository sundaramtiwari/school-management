package com.school.backend.testmanagement.repository;

import com.school.backend.testmanagement.entity.StudentMark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentMarkRepository
        extends JpaRepository<StudentMark, Long> {

    boolean existsByExamSubjectIdAndStudentId(Long examSubjectId, Long studentId);
}
