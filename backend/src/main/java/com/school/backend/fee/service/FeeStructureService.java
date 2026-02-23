package com.school.backend.fee.service;

import com.school.backend.common.enums.FeeFrequency;
import com.school.backend.common.enums.LateFeeCapType;
import com.school.backend.common.enums.LateFeeType;
import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.core.student.entity.StudentEnrollment;
import com.school.backend.core.student.repository.StudentEnrollmentRepository;
import com.school.backend.fee.dto.FeeStructureCreateRequest;
import com.school.backend.fee.dto.FeeStructurePatchRequest;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FeeStructureService {
    private static final Logger log = LoggerFactory.getLogger(FeeStructureService.class);

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

        FeeType feeType = feeTypeRepository.findByIdAndSchoolId(req.getFeeTypeId(), SecurityUtil.schoolId())
                .orElseThrow(() -> new ResourceNotFoundException("FeeType not found: " + req.getFeeTypeId()));
        if (!feeType.isActive()) {
            throw new IllegalStateException("Cannot create fee structure with inactive fee type: " + feeType.getId());
        }

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
    public FeeStructureDto update(Long id, Long schoolId, FeeStructurePatchRequest req) {
        FeeStructure fs = feeStructureRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> {
                    if (feeStructureRepository.existsAnyById(id)) {
                        throw new AccessDeniedException("Access denied for fee structure: " + id);
                    }
                    return new ResourceNotFoundException("FeeStructure not found: " + id);
                });

        validateImmutableFields(fs, req);

        Map<String, Object> oldValues = captureStructureState(fs);

        if (req.getAmount() != null) {
            fs.setAmount(req.getAmount());
        }
        if (req.getFrequency() != null) {
            fs.setFrequency(req.getFrequency());
        }
        if (req.getDueDayOfMonth() != null) {
            fs.setDueDayOfMonth(req.getDueDayOfMonth());
        }

        syncLateFeePolicy(fs, req);

        FeeStructure saved = feeStructureRepository.save(fs);
        Map<String, Object> newValues = captureStructureState(saved);
        log.info(
                "event=fee_structure_updated schoolId={} structureId={} actorId={} oldValues={} newValues={} timestamp={}",
                schoolId,
                saved.getId(),
                SecurityUtil.userId(),
                oldValues,
                newValues,
                Instant.now());

        return toDto(saved);
    }

    @Transactional
    public FeeStructureDto toggleActive(Long id, Long schoolId) {
        FeeStructure fs = feeStructureRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> {
                    if (feeStructureRepository.existsAnyById(id)) {
                        throw new AccessDeniedException("Access denied for fee structure: " + id);
                    }
                    return new ResourceNotFoundException("FeeStructure not found: " + id);
                });

        boolean oldActive = fs.isActive();
        fs.setActive(!oldActive);
        FeeStructure saved = feeStructureRepository.save(fs);

        log.info("event=fee_structure_toggled schoolId={} structureId={} actorId={} oldActive={} newActive={} timestamp={}",
                schoolId,
                saved.getId(),
                SecurityUtil.userId(),
                oldActive,
                saved.isActive(),
                Instant.now());

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

        // Fallback: If session or dates are missing, charge the full year amount
        if (session == null || session.getStartDate() == null || session.getEndDate() == null) {
            return fs.getAmount().multiply(BigDecimal.valueOf(fs.getFrequency().getPeriodsPerYear()));
        }

        // Use enrollment date if student joined mid-session, otherwise session start
        LocalDate effectiveStart = enrollmentRepository
                .findFirstByStudentIdAndSessionIdAndActiveTrue(studentId, fs.getSessionId())
                .map(StudentEnrollment::getEnrollmentDate)
                .filter(d -> d.isAfter(session.getStartDate()))
                .orElse(session.getStartDate());

        // Calculate total months remaining in session from effective start date
        long monthsRemaining = ChronoUnit.MONTHS.between(
                effectiveStart.withDayOfMonth(1), // normalize to month start
                session.getEndDate().plusDays(1));

        if (monthsRemaining <= 0) {
            monthsRemaining = 1; // minimum 1 month
        }

        return switch (fs.getFrequency()) {
            case MONTHLY -> fs.getAmount().multiply(BigDecimal.valueOf(monthsRemaining));
            case QUARTERLY -> fs.getAmount().multiply(BigDecimal.valueOf(Math.ceil(monthsRemaining / 3.0)));
            case HALF_YEARLY -> fs.getAmount().multiply(BigDecimal.valueOf(Math.ceil(monthsRemaining / 6.0)));
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

    private void validateImmutableFields(FeeStructure fs, FeeStructurePatchRequest req) {
        if (req.getSchoolId() != null && !req.getSchoolId().equals(fs.getSchoolId())) {
            throw new IllegalArgumentException("schoolId is immutable");
        }
        if (req.getSessionId() != null && !req.getSessionId().equals(fs.getSessionId())) {
            throw new IllegalArgumentException("sessionId is immutable");
        }
        if (req.getClassId() != null && !req.getClassId().equals(fs.getClassId())) {
            throw new IllegalArgumentException("classId is immutable");
        }
        if (req.getFeeTypeId() != null && !req.getFeeTypeId().equals(fs.getFeeType().getId())) {
            throw new IllegalArgumentException("feeTypeId is immutable");
        }
    }

    private void syncLateFeePolicy(FeeStructure fs, FeeStructurePatchRequest req) {
        LateFeePolicy existingPolicy = lateFeePolicyRepository.findByFeeStructureId(fs.getId()).orElse(null);
        if (req.getLateFeePolicyId() != null) {
            if (existingPolicy == null || !req.getLateFeePolicyId().equals(existingPolicy.getId())) {
                throw new IllegalArgumentException("lateFeePolicyId does not belong to this fee structure");
            }
        }

        if (req.getLateFeeType() == null &&
                req.getLateFeeAmountValue() == null &&
                req.getLateFeeGraceDays() == null &&
                req.getLateFeeCapType() == null &&
                req.getLateFeeCapValue() == null) {
            return;
        }

        LateFeeType nextType = req.getLateFeeType() != null
                ? req.getLateFeeType()
                : (existingPolicy != null ? existingPolicy.getType() : LateFeeType.NONE);

        if (nextType == LateFeeType.NONE) {
            if (existingPolicy != null) {
                existingPolicy.setType(LateFeeType.NONE);
                existingPolicy.setAmountValue(BigDecimal.ZERO);
                existingPolicy.setGraceDays(0);
                existingPolicy.setCapType(LateFeeCapType.NONE);
                existingPolicy.setCapValue(BigDecimal.ZERO);
                existingPolicy.setActive(false);
                lateFeePolicyRepository.save(existingPolicy);
            }
            return;
        }

        LateFeePolicy target = existingPolicy != null ? existingPolicy : LateFeePolicy.builder()
                .schoolId(fs.getSchoolId())
                .feeStructure(fs)
                .build();

        target.setType(nextType);
        target.setAmountValue(req.getLateFeeAmountValue() != null ? req.getLateFeeAmountValue()
                : (target.getAmountValue() != null ? target.getAmountValue() : BigDecimal.ZERO));
        target.setGraceDays(req.getLateFeeGraceDays() != null ? req.getLateFeeGraceDays()
                : (target.getGraceDays() != null ? target.getGraceDays() : 0));
        target.setCapType(req.getLateFeeCapType() != null ? req.getLateFeeCapType()
                : (target.getCapType() != null ? target.getCapType() : LateFeeCapType.NONE));
        target.setCapValue(req.getLateFeeCapValue() != null ? req.getLateFeeCapValue()
                : (target.getCapValue() != null ? target.getCapValue() : BigDecimal.ZERO));
        target.setActive(true);
        lateFeePolicyRepository.save(target);
    }

    private Map<String, Object> captureStructureState(FeeStructure fs) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("amount", fs.getAmount());
        values.put("frequency", fs.getFrequency());
        values.put("dueDayOfMonth", fs.getDueDayOfMonth());
        values.put("active", fs.isActive());

        LateFeePolicy policy = lateFeePolicyRepository.findByFeeStructureId(fs.getId()).orElse(null);
        values.put("lateFeePolicyId", policy != null ? policy.getId() : null);
        values.put("lateFeeType", policy != null ? policy.getType() : null);
        values.put("lateFeeAmountValue", policy != null ? policy.getAmountValue() : null);
        values.put("lateFeeGraceDays", policy != null ? policy.getGraceDays() : null);
        values.put("lateFeeCapType", policy != null ? policy.getCapType() : null);
        values.put("lateFeeCapValue", policy != null ? policy.getCapValue() : null);
        return values;
    }
}
