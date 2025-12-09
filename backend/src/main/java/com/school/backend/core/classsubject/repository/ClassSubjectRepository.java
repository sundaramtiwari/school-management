package com.school.backend.core.classsubject.repository;

import com.school.backend.core.classsubject.entity.ClassSubject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClassSubjectRepository extends JpaRepository<ClassSubject, Long> {

    Page<ClassSubject> findBySchoolClassId(Long classId, Pageable pageable);

    Page<ClassSubject> findBySchoolId(Long schoolId, Pageable pageable);

    Optional<ClassSubject> findBySchoolClassIdAndSubjectId(Long classId, Long subjectId);

    boolean existsBySchoolClassIdAndSubjectId(Long classId, Long subjectId);
}
