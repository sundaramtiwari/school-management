package com.school.backend.fee.repository;

import com.school.backend.fee.entity.StudentFeeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentFeeAssignmentRepository
                extends JpaRepository<StudentFeeAssignment, Long> {

        boolean existsByStudentIdAndFeeStructureIdAndSessionId(
                        Long studentId,
                        Long feeStructureId,
                        Long sessionId);

        boolean existsByStudentIdAndFeeStructureId(Long studentId, Long feeStructureId);

        List<StudentFeeAssignment> findByStudentIdAndSessionId(Long studentId, Long sessionId);
}
