package com.school.backend.core.student.repository;

import com.school.backend.core.student.entity.StudentDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentDocumentRepository extends JpaRepository<StudentDocument, Long> {
    Page<StudentDocument> findByStudentId(Long studentId, Pageable pageable);
}
