package com.school.backend.fee.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.fee.dto.StudentFeeAssignRequest;
import com.school.backend.fee.dto.StudentFeeAssignmentDto;
import com.school.backend.fee.entity.FeeStructure;
import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.fee.enums.FeeFrequency;
import com.school.backend.fee.repository.FeeStructureRepository;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.repository.AcademicSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.school.backend.fee.entity.LateFeePolicy;
import com.school.backend.fee.repository.LateFeePolicyRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentFeeAssignmentService {

    private final StudentFeeAssignmentRepository assignmentRepository;
    private final FeeStructureRepository feeStructureRepository;
    private final StudentRepository studentRepository;
    private final LateFeePolicyRepository lateFeePolicyRepository;
    private final AcademicSessionRepository academicSessionRepository;

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

        BigDecimal finalAmount = fs.getAmount();
        if (fs.getFrequency() == FeeFrequency.MONTHLY) {
            finalAmount = finalAmount.multiply(new BigDecimal(12));
        }

        // --- Derive Due Date ---
        int sessionStartYear = resolveSessionStartYear(req.getSessionId(), fs.getSchoolId());
        int dueDay = Math.min(fs.getDueDayOfMonth() != null ? fs.getDueDayOfMonth() : 10, 28);
        LocalDate dueDate = LocalDate.of(sessionStartYear, 4, dueDay);

        // --- Snapshot Late Fee Policy ---
        LateFeePolicy policy = lateFeePolicyRepository.findByFeeStructureId(fs.getId()).orElse(null);

        StudentFeeAssignment assignment = StudentFeeAssignment.builder()
                .studentId(req.getStudentId())
                .feeStructureId(fs.getId())
                .sessionId(req.getSessionId())
                .amount(finalAmount)
                .dueDate(dueDate)
                .lateFeeType(policy != null ? policy.getType() : null)
                .lateFeeValue(policy != null ? policy.getAmountValue() : BigDecimal.ZERO)
                .lateFeeGraceDays(policy != null ? policy.getGraceDays() : 0)
                .lateFeeCapType(policy != null ? policy.getCapType() : com.school.backend.fee.enums.LateFeeCapType.NONE)
                .lateFeeCapValue(policy != null ? policy.getCapValue() : BigDecimal.ZERO)
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
        dto.setAmount(sfa.getAmount());

        dto.setDueDate(sfa.getDueDate());
        dto.setLateFeeType(sfa.getLateFeeType());
        dto.setLateFeeValue(sfa.getLateFeeValue());
        dto.setLateFeeGraceDays(sfa.getLateFeeGraceDays());
        dto.setLateFeeCapType(sfa.getLateFeeCapType());
        dto.setLateFeeCapValue(sfa.getLateFeeCapValue());

        dto.setLateFeeApplied(sfa.isLateFeeApplied());
        dto.setLateFeeAccrued(sfa.getLateFeeAccrued());
        dto.setLateFeePaid(sfa.getLateFeePaid());
        dto.setLateFeeWaived(sfa.getLateFeeWaived());
        dto.setTotalDiscountAmount(sfa.getTotalDiscountAmount());

        dto.setActive(sfa.isActive());

        return dto;
    }

    private int resolveSessionStartYear(Long sessionId, Long schoolId) {
        return academicSessionRepository.findById(sessionId)
                .filter(s -> schoolId.equals(s.getSchoolId()))
                .map(AcademicSession::getName)
                .map(this::extractYearFromSessionName)
                .orElse(LocalDate.now().getYear());
    }

    private int extractYearFromSessionName(String sessionName) {
        if (sessionName == null) {
            return LocalDate.now().getYear();
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d{4})").matcher(sessionName);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return LocalDate.now().getYear();
    }
}
