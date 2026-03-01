package com.school.backend.core.student.service;

import com.school.backend.common.enums.StudentStatus;
import com.school.backend.common.exception.InvalidOperationException;
import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.common.tenant.SessionContext;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import com.school.backend.core.student.dto.StudentEnrollmentDto;
import com.school.backend.core.student.dto.StudentEnrollmentRequest;
import com.school.backend.core.student.entity.StudentEnrollment;
import com.school.backend.core.student.mapper.StudentEnrollmentMapper;
import com.school.backend.core.student.repository.PromotionRecordRepository;
import com.school.backend.core.student.repository.StudentEnrollmentRepository;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.fee.entity.FeeStructure;
import com.school.backend.fee.repository.FeeStructureRepository;
import com.school.backend.fee.service.FeeStructureService;
import com.school.backend.school.service.SubscriptionAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final StudentEnrollmentRepository enrollmentRepository;
    private final StudentEnrollmentMapper enrollmentMapper;
    private final StudentRepository studentRepository;
    private final SchoolClassRepository classRepository;
    private final PromotionRecordRepository promotionRepo;
    private final FeeStructureService feeStructureService;
    private final FeeStructureRepository feeStructureRepository;
    private final SubscriptionAccessService subscriptionAccessService;

    @Transactional
    public StudentEnrollmentDto enroll(StudentEnrollmentRequest req) {
        Long schoolId = TenantContext.getSchoolId();
        Long sessionId = requireSessionId();
        if (req.getSessionId() != null && !req.getSessionId().equals(sessionId)) {
            throw new InvalidOperationException("Session mismatch between request and context");
        }
        req.setSessionId(sessionId);

        // verify student & class
        studentRepository.findById(req.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        classRepository.findById(req.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        long activeCount = enrollmentRepository.countByStudentIdAndSessionIdAndSchoolIdAndActiveTrue(
                req.getStudentId(), sessionId, schoolId);
        if (activeCount > 1) {
            throw new InvalidOperationException(
                    "Data integrity violation: multiple active enrollments found for student/session.");
        }
        if (activeCount == 1) {
            throw new InvalidOperationException("Student already has an active enrollment for this session.");
        }

        // Cap enforcement for active students (active + enrolled in current session).
        subscriptionAccessService.validateStudentCreationAllowed(schoolId);

        StudentEnrollment ent = enrollmentMapper.toEntity(req);
        ent.setEnrollmentDate(req.getEnrollmentDate() != null ? req.getEnrollmentDate() : LocalDate.now());
        StudentEnrollment saved = enrollmentRepository.save(ent);

        // update student's currentClass reference
        studentRepository.findById(req.getStudentId()).ifPresent(s -> {
            s.setCurrentClass(classRepository.findById(req.getClassId()).orElse(null));
            s.setCurrentStatus(StudentStatus.ENROLLED);
            studentRepository.save(s);
        });

        // Trigger Auto-Assignment of Fees
        List<FeeStructure> existingFees = feeStructureRepository.findByClassIdAndSessionIdAndSchoolIdAndActiveTrue(
                req.getClassId(), sessionId, schoolId);

        for (FeeStructure fs : existingFees) {
            feeStructureService.assignFeeToStudent(fs, req.getStudentId());
        }

        return enrollmentMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<StudentEnrollmentDto> listByClass(Long classId, Pageable pageable) {
        return enrollmentRepository.findByClassIdAndSessionId(classId, requireSessionId(), pageable)
                .map(enrollmentMapper::toDto);
    }

    private Long requireSessionId() {
        Long sessionId = SessionContext.getSessionId();
        if (sessionId == null) {
            throw new InvalidOperationException("Session context is missing in request");
        }
        return sessionId;
    }
}
