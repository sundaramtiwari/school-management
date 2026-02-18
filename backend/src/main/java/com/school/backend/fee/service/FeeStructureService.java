package com.school.backend.fee.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.core.student.entity.StudentEnrollment;
import com.school.backend.core.student.repository.StudentEnrollmentRepository;
import com.school.backend.fee.dto.FeeStructureCreateRequest;
import com.school.backend.fee.dto.FeeStructureDto;
import com.school.backend.fee.entity.FeeStructure;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.fee.enums.FeeFrequency;
import com.school.backend.fee.entity.LateFeePolicy;
import com.school.backend.fee.repository.FeeStructureRepository;
import com.school.backend.fee.repository.FeeTypeRepository;
import com.school.backend.fee.repository.LateFeePolicyRepository;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.repository.AcademicSessionRepository;
import com.school.backend.school.service.SetupValidationService;
import com.school.backend.user.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeeStructureService {

    private final FeeStructureRepository feeStructureRepository;
    private final FeeTypeRepository feeTypeRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final StudentFeeAssignmentRepository assignmentRepository;
    private final SetupValidationService setupValidationService;
    private final LateFeePolicyRepository lateFeePolicyRepository;
    private final AcademicSessionRepository academicSessionRepository;

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
                .dueDayOfMonth(req.getDueDayOfMonth() != null ? req.getDueDayOfMonth() : 10)
                .active(true)
                .build();

        FeeStructure saved = feeStructureRepository.save(fs);

        // Create Late Fee Policy if provided
        if (req.getLateFeeType() != null && req.getLateFeeType() != com.school.backend.fee.enums.LateFeeType.NONE) {
            LateFeePolicy policy = LateFeePolicy.builder()
                    .schoolId(saved.getSchoolId())
                    .feeStructure(saved)
                    .type(req.getLateFeeType())
                    .amountValue(req.getLateFeeAmountValue() != null ? req.getLateFeeAmountValue() : BigDecimal.ZERO)
                    .graceDays(req.getLateFeeGraceDays() != null ? req.getLateFeeGraceDays() : 0)
                    .capType(req.getLateFeeCapType() != null ? req.getLateFeeCapType() : com.school.backend.fee.enums.LateFeeCapType.NONE)
                    .capValue(req.getLateFeeCapValue() != null ? req.getLateFeeCapValue() : BigDecimal.ZERO)
                    .active(true)
                    .build();
            lateFeePolicyRepository.save(policy);
        }

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

        BigDecimal finalAmount = fs.getAmount();
        if (fs.getFrequency() == FeeFrequency.MONTHLY) {
            finalAmount = finalAmount.multiply(new BigDecimal(12));
        }

        // --- Snapshot Late Fee Policy ---
        LateFeePolicy policy = lateFeePolicyRepository.findByFeeStructureId(fs.getId()).orElse(null);

        // Derive Due Date (copy logic from StudentFeeAssignmentService or centralize)
        int sessionStartYear = resolveSessionStartYear(fs.getSessionId(), fs.getSchoolId());
        java.time.LocalDate dueDate = java.time.LocalDate.of(sessionStartYear, 4,
                Math.min(fs.getDueDayOfMonth() != null ? fs.getDueDayOfMonth() : 10, 28));

        StudentFeeAssignment assignment = StudentFeeAssignment.builder()
                .schoolId(fs.getSchoolId())
                .studentId(studentId)
                .feeStructureId(fs.getId())
                .sessionId(fs.getSessionId())
                .amount(finalAmount)
                .dueDate(dueDate)
                .lateFeeType(policy != null ? policy.getType() : null)
                .lateFeeValue(policy != null ? policy.getAmountValue() : BigDecimal.ZERO)
                .lateFeeGraceDays(policy != null ? policy.getGraceDays() : 0)
                .lateFeeCapType(policy != null ? policy.getCapType() : com.school.backend.fee.enums.LateFeeCapType.NONE)
                .lateFeeCapValue(policy != null ? policy.getCapValue() : BigDecimal.ZERO)
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
        dto.setDueDayOfMonth(fs.getDueDayOfMonth());
        dto.setActive(fs.isActive());

        // Map Late Fee Policy if exists
        lateFeePolicyRepository.findByFeeStructureId(fs.getId()).ifPresent(policy -> {
            dto.setLateFeeType(policy.getType());
            dto.setLateFeeAmountValue(policy.getAmountValue());
            dto.setLateFeeGraceDays(policy.getGraceDays());
            dto.setLateFeeCapType(policy.getCapType());
            dto.setLateFeeCapValue(policy.getCapValue());
        });

        return dto;
    }

    private int resolveSessionStartYear(Long sessionId, Long schoolId) {
        return academicSessionRepository.findById(sessionId)
                .filter(s -> schoolId.equals(s.getSchoolId()))
                .map(AcademicSession::getName)
                .map(this::extractYearFromSessionName)
                .orElse(java.time.LocalDate.now().getYear());
    }

    private int extractYearFromSessionName(String sessionName) {
        if (sessionName == null) {
            return java.time.LocalDate.now().getYear();
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d{4})").matcher(sessionName);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return java.time.LocalDate.now().getYear();
    }
}
