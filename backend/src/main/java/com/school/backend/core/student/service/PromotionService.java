package com.school.backend.core.student.service;

import com.school.backend.common.enums.AdmissionType;
import com.school.backend.common.enums.PromotionType;
import com.school.backend.common.exception.InvalidOperationException;
import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.core.classsubject.entity.SchoolClass;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import com.school.backend.core.student.dto.PromotionRequest;
import com.school.backend.core.student.entity.PromotionRecord;
import com.school.backend.core.student.entity.StudentEnrollment;
import com.school.backend.core.student.repository.PromotionRecordRepository;
import com.school.backend.core.student.repository.StudentEnrollmentRepository;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.school.repository.AcademicSessionRepository;
import com.school.backend.testmanagement.repository.ExamRepository;
import com.school.backend.user.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final PromotionRecordRepository promotionRecordRepository;
    private final AcademicSessionRepository academicSessionRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ExamRepository examRepository;

    @Transactional
    public List<PromotionRecord> promoteStudents(PromotionRequest request) {
        if (request.getStudentIds() == null || request.getStudentIds().isEmpty()) {
            throw new IllegalArgumentException("studentIds must not be empty");
        }

        if (!academicSessionRepository.existsById(request.getTargetSessionId())) {
            throw new ResourceNotFoundException("Target session not found: " + request.getTargetSessionId());
        }

        SchoolClass targetClass = schoolClassRepository.findById(request.getTargetClassId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Target class not found: " + request.getTargetClassId()));

        if (!request.getTargetSessionId().equals(targetClass.getSessionId())) {
            throw new InvalidOperationException("targetClass must belong to targetSession");
        }

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        String promotedBy = SecurityUtil.current().getUsername();
        AdmissionType admissionType = mapAdmissionType(request.getPromotionType());

        Set<Long> uniqueStudentIds = new LinkedHashSet<>(request.getStudentIds());
        List<PromotionRecord> promotionRecords = new ArrayList<>();
        for (Long studentId : uniqueStudentIds) {
            PromotionRecord record = processSingleStudent(studentId, request, targetClass, today, now, promotedBy,
                    admissionType);
            if (record != null) {
                promotionRecords.add(record);
            }
        }
        return promotionRecords;
    }

    private PromotionRecord processSingleStudent(Long studentId,
            PromotionRequest request,
            SchoolClass targetClass,
            LocalDate today,
            LocalDateTime now,
            String promotedBy,
            AdmissionType admissionType) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found: " + studentId);
        }

        List<StudentEnrollment> activeEnrollments = studentEnrollmentRepository
                .findActiveByStudentIdForUpdate(studentId);
        if (activeEnrollments.size() != 1) {
            throw new InvalidOperationException(
                    "Student must have exactly one active enrollment. Please verify the student's enrollment status.");
        }

        StudentEnrollment currentEnrollment = activeEnrollments.get(0);

        boolean alreadyAtTarget = request.getTargetSessionId().equals(currentEnrollment.getSessionId())
                && request.getTargetClassId().equals(currentEnrollment.getClassId());
        if (alreadyAtTarget) {
            return null;
        }

        long nonLockedExamCount = examRepository.countNonLockedBySessionIdAndClassId(
                currentEnrollment.getSessionId(), currentEnrollment.getClassId());
        if (nonLockedExamCount > 0) {
            throw new InvalidOperationException(
                    "All exams must be LOCKED for the current session/class before a student can be promoted. Please lock all exams first.");
        }

        currentEnrollment.setActive(false);
        currentEnrollment.setEndDate(today);
        studentEnrollmentRepository.save(currentEnrollment);

        StudentEnrollment newEnrollment = StudentEnrollment.builder()
                .studentId(studentId)
                .classId(request.getTargetClassId())
                .section(targetClass.getSection())
                .sessionId(request.getTargetSessionId())
                .rollNumber(null)
                .enrollmentDate(today)
                .startDate(today)
                .active(true)
                .admissionType(admissionType)
                .remarks(request.getRemarks())
                .schoolId(currentEnrollment.getSchoolId())
                .build();
        studentEnrollmentRepository.save(newEnrollment);

        PromotionRecord promotionRecord = PromotionRecord.builder()
                .studentId(studentId)
                .sourceSessionId(currentEnrollment.getSessionId())
                .targetSessionId(request.getTargetSessionId())
                .sourceClassId(currentEnrollment.getClassId())
                .targetClassId(request.getTargetClassId())
                .promotionType(request.getPromotionType())
                .remarks(request.getRemarks())
                .promotedBy(promotedBy)
                .promotedAt(now)
                .schoolId(currentEnrollment.getSchoolId())
                .build();
        return promotionRecordRepository.save(promotionRecord);
    }

    private AdmissionType mapAdmissionType(PromotionType promotionType) {
        return promotionType == PromotionType.REPEAT ? AdmissionType.REPEAT : AdmissionType.PROMOTION;
    }
}
