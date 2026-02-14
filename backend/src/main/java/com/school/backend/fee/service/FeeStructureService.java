package com.school.backend.fee.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.fee.dto.FeeStructureCreateRequest;
import com.school.backend.fee.dto.FeeStructureDto;
import com.school.backend.fee.entity.FeeStructure;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.fee.enums.FeeFrequency;
import com.school.backend.fee.repository.FeeStructureRepository;
import com.school.backend.fee.repository.FeeTypeRepository;
import com.school.backend.core.student.entity.StudentEnrollment;
import com.school.backend.core.student.repository.StudentEnrollmentRepository;
import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import com.school.backend.user.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeeStructureService {

    private final FeeStructureRepository feeStructureRepository;
    private final FeeTypeRepository feeTypeRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final StudentFeeAssignmentRepository assignmentRepository;
    private final com.school.backend.school.service.SetupValidationService setupValidationService;

    // ---------------- CREATE ----------------
    @Transactional
    public FeeStructureDto create(FeeStructureCreateRequest req) {
        setupValidationService.ensureAtLeastOneClassExists(SecurityUtil.schoolId(), req.getSessionId());

        FeeType feeType = feeTypeRepository.findById(req.getFeeTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("FeeType not found: " + req.getFeeTypeId()));

        FeeStructure fs = FeeStructure.builder()
                .schoolId(SecurityUtil.schoolId())
                .classId(req.getClassId())
                .sessionId(req.getSessionId())
                .feeType(feeType)
                .amount(req.getAmount())
                .frequency(req.getFrequency() != null ? req.getFrequency() : FeeFrequency.ONE_TIME)
                .active(true)
                .build();

        FeeStructure saved = feeStructureRepository.save(fs);

        // Auto-assign to all students in the class
        if (saved.getClassId() != null) {
            assignFeeToStudents(saved);
        }

        return toDto(saved);
    }

    @Transactional
    public void assignFeeToStudents(FeeStructure fs) {
        List<StudentEnrollment> enrollments = enrollmentRepository.findByClassIdAndSessionId(fs.getClassId(),
                fs.getSessionId());
        for (StudentEnrollment enroll : enrollments) {
            assignFeeToStudent(fs, enroll.getStudentId());
        }
    }

    @Transactional
    public void assignFeeToStudent(FeeStructure fs, Long studentId) {
        // Frequency Rules
        if (fs.getFrequency() == FeeFrequency.ONE_TIME) {
            // ONE_TIME: Check history-wide using studentId + feeStructureId
            if (assignmentRepository.existsByStudentIdAndFeeStructureId(studentId, fs.getId())) {
                return;
            }
        } else {
            // ANNUALLY / MONTHLY: Check current session
            if (assignmentRepository.existsByStudentIdAndFeeStructureIdAndSessionId(studentId, fs.getId(),
                    fs.getSessionId())) {
                return;
            }
        }

        int finalAmount = fs.getAmount();
        if (fs.getFrequency() == FeeFrequency.MONTHLY) {
            finalAmount = finalAmount * 12;
        }

        StudentFeeAssignment assignment = StudentFeeAssignment.builder()
                .schoolId(fs.getSchoolId())
                .studentId(studentId)
                .feeStructureId(fs.getId())
                .sessionId(fs.getSessionId())
                .amount(finalAmount)
                .active(true)
                .build();

        assignmentRepository.save(assignment);
    }

    // ---------------- LIST ----------------
    @Transactional(readOnly = true)
    public List<FeeStructureDto> listByClass(Long classId, Long sessionId, Long schoolId) {

        return feeStructureRepository
                .findByClassIdAndSessionIdAndSchoolId(classId, sessionId, schoolId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    // ---------------- MAPPER ----------------
    private FeeStructureDto toDto(FeeStructure fs) {

        FeeStructureDto dto = new FeeStructureDto();

        dto.setId(fs.getId());
        dto.setSchoolId(fs.getSchoolId());
        dto.setClassId(fs.getClassId());
        dto.setSessionId(fs.getSessionId());

        dto.setFeeTypeId(fs.getFeeType().getId());
        dto.setFeeTypeName(fs.getFeeType().getName());

        dto.setAmount(fs.getAmount());
        dto.setFrequency(fs.getFrequency());
        dto.setActive(fs.isActive());

        return dto;
    }
}
