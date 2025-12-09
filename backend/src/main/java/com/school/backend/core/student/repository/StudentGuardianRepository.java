package com.school.backend.core.student.repository;

import com.school.backend.core.student.entity.StudentGuardian;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentGuardianRepository extends JpaRepository<StudentGuardian, Long> {
    List<StudentGuardian> findByStudentId(Long studentId);

    List<StudentGuardian> findByGuardianId(Long guardianId);

    boolean existsByStudentIdAndGuardianId(Long studentId, Long guardianId);
}
