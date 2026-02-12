package com.school.backend.fee.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.fee.dto.StudentFeeAssignRequest;
import com.school.backend.fee.dto.StudentFeeAssignmentDto;
import com.school.backend.fee.entity.FeeStructure;
import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.fee.repository.FeeStructureRepository;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentFeeAssignmentService {

    private final StudentFeeAssignmentRepository assignmentRepository;
    private final FeeStructureRepository feeStructureRepository;
    private final StudentRepository studentRepository;

    // ---------------- ASSIGN ----------------
    @Transactional
    public StudentFeeAssignmentDto assign(StudentFeeAssignRequest req) {

        if (!studentRepository.existsById(req.getStudentId())) {
            throw new ResourceNotFoundException("Student not found: " + req.getStudentId());
        }

        FeeStructure fs = feeStructureRepository.findById(req.getFeeStructureId())
                .orElseThrow(() -> new ResourceNotFoundException("FeeStructure not found: " + req.getFeeStructureId()));

        // Prevent duplicate assignment
        boolean exists = assignmentRepository
                .existsByStudentIdAndFeeStructureIdAndSessionId(
                        req.getStudentId(),
                        req.getFeeStructureId(),
                        req.getSessionId());

        if (exists) {
            throw new IllegalStateException("Fee already assigned to student for this session");
        }

        StudentFeeAssignment assignment = StudentFeeAssignment.builder()
                .studentId(req.getStudentId())
                .feeStructureId(fs.getId())
                .sessionId(req.getSessionId())
                .schoolId(TenantContext.getSchoolId())
                .active(true)
                .build();

        return toDto(assignmentRepository.save(assignment));
    }

    // ---------------- LIST ----------------
    @Transactional(readOnly = true)
    public List<StudentFeeAssignmentDto> listByStudent(Long studentId, Long sessionId) {

        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found: " + studentId);
        }

        return assignmentRepository
                .findByStudentIdAndSessionId(studentId, sessionId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    // ---------------- MAPPER ----------------
    private StudentFeeAssignmentDto toDto(StudentFeeAssignment sfa) {

        StudentFeeAssignmentDto dto = new StudentFeeAssignmentDto();

        dto.setId(sfa.getId());
        dto.setStudentId(sfa.getStudentId());
        dto.setFeeStructureId(sfa.getFeeStructureId());
        dto.setSessionId(sfa.getSessionId());
        dto.setActive(sfa.isActive());

        return dto;
    }
}
