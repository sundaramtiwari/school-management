package com.school.backend.fee.service;

import com.school.backend.common.enums.FeeFrequency;
import com.school.backend.common.enums.LateFeeCapType;
import com.school.backend.common.enums.LateFeeType;
import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.core.student.entity.StudentEnrollment;
import com.school.backend.core.student.repository.StudentEnrollmentRepository;
import com.school.backend.fee.dto.FeeStructureCreateRequest;
import com.school.backend.fee.dto.FeeStructureDto;
import com.school.backend.fee.entity.FeeStructure;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.fee.entity.LateFeePolicy;
import com.school.backend.fee.entity.StudentFeeAssignment;
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
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
        if (req.getLateFeeType() != null && req.getLateFeeType() != LateFeeType.NONE) {
            LateFeePolicy policy = LateFeePolicy.builder()
                    .schoolId(saved.getSchoolId())
                    .feeStructure(saved)
                    .type(req.getLateFeeType())
                    .amountValue(req.getLateFeeAmountValue() != null ? req.getLateFeeAmountValue() : BigDecimal.ZERO)
                    .graceDays(req.getLateFeeGraceDays() != null ? req.getLateFeeGraceDays() : 0)
                    .capType(req.getLateFeeCapType() != null ? req.getLateFeeCapType() : LateFeeCapType.NONE)
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

        BigDecimal finalAmount = computeAmount(fs, studentId);

        // --- Snapshot Late Fee Policy ---
        LateFeePolicy policy = lateFeePolicyRepository.findByFeeStructureId(fs.getId()).orElse(null);

        // Derive due date from actual session date range.
        java.time.LocalDate dueDate = resolveDerivedDueDate(
                fs.getSessionId(),
                fs.getSchoolId(),
                fs.getDueDayOfMonth() != null ? fs.getDueDayOfMonth() : 10);

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
                .lateFeeCapType(policy != null ? policy.getCapType() : LateFeeCapType.NONE)
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

    private BigDecimal computeAmount(FeeStructure fs, Long studentId) {
        if (fs.getFrequency() == null || fs.getFrequency() == FeeFrequency.ONE_TIME
                || fs.getFrequency() == FeeFrequency.ANNUALLY) {
            return fs.getAmount();
        }

        AcademicSession session = academicSessionRepository.findById(fs.getSessionId())
                .filter(s -> fs.getSchoolId().equals(s.getSchoolId()))
                .orElse(null);

        if (session == null || session.getStartDate() == null || session.getEndDate() == null) {
            return fs.getAmount().multiply(BigDecimal.valueOf(fs.getFrequency().getPeriodsPerYear()));
        }

        // Use enrollment date if student joined mid-session, otherwise session start
        LocalDate effectiveStart = enrollmentRepository
                .findFirstByStudentIdAndSessionIdAndActiveTrue(studentId, fs.getSessionId())
                .map(StudentEnrollment::getEnrollmentDate)
                .filter(d -> d.isAfter(session.getStartDate()))
                .orElse(session.getStartDate());

        long months = ChronoUnit.MONTHS.between(
                effectiveStart.withDayOfMonth(1), // normalize to month start
                session.getEndDate().plusDays(1));

        if (months <= 0)
            months = 1; // minimum 1 month

        return switch (fs.getFrequency()) {
            case MONTHLY -> fs.getAmount().multiply(BigDecimal.valueOf(months));
            case QUARTERLY -> fs.getAmount().multiply(BigDecimal.valueOf(Math.ceil(months / 3.0)));
            case HALF_YEARLY -> fs.getAmount().multiply(BigDecimal.valueOf(Math.ceil(months / 6.0)));
            default -> fs.getAmount();
        };
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

    private java.time.LocalDate resolveDerivedDueDate(Long sessionId, Long schoolId, Integer dueDayOfMonth) {
        AcademicSession session = academicSessionRepository.findById(sessionId)
                .filter(s -> schoolId.equals(s.getSchoolId()))
                .orElse(null);

        if (session == null || session.getStartDate() == null) {
            return java.time.LocalDate.now().plusDays(10);
        }

        java.time.LocalDate start = session.getStartDate();
        java.time.LocalDate end = session.getEndDate() != null ? session.getEndDate() : start.plusYears(1).minusDays(1);
        int targetDay = Math.max(1, Math.min(dueDayOfMonth != null ? dueDayOfMonth : 10, 31));

        java.time.LocalDate candidate = clampDay(start.withDayOfMonth(1), targetDay);
        if (candidate.isBefore(start)) {
            candidate = clampDay(start.plusMonths(1).withDayOfMonth(1), targetDay);
        }
        if (candidate.isAfter(end)) {
            return end;
        }
        return candidate;
    }

    private java.time.LocalDate clampDay(java.time.LocalDate baseMonth, int day) {
        int lastDay = baseMonth.lengthOfMonth();
        return baseMonth.withDayOfMonth(Math.min(day, lastDay));
    }
}
