package com.school.backend.fee.repository;

import com.school.backend.fee.entity.StudentFeeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentFeeAssignmentRepository
        extends JpaRepository<StudentFeeAssignment, Long> {

    boolean existsByStudentIdAndFeeStructureIdAndSession(
            Long studentId,
            Long feeStructureId,
            String session);

    List<StudentFeeAssignment> findByStudentIdAndSession(Long studentId, String session);
}
