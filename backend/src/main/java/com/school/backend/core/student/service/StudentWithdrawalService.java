package com.school.backend.core.student.service;

import com.school.backend.common.exception.InvalidOperationException;
import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.core.student.dto.StudentWithdrawalRequest;
import com.school.backend.core.student.dto.StudentWithdrawalResponse;
import com.school.backend.core.student.entity.StudentEnrollment;
import com.school.backend.core.student.repository.StudentEnrollmentRepository;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.repository.AcademicSessionRepository;
import com.school.backend.transport.repository.TransportEnrollmentRepository;
import com.school.backend.transport.service.TransportEnrollmentService;
import com.school.backend.user.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentWithdrawalService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final AcademicSessionRepository academicSessionRepository;
    private final StudentFeeAssignmentRepository assignmentRepository;
    private final TransportEnrollmentRepository transportEnrollmentRepository;
    private final TransportEnrollmentService transportEnrollmentService;

    @Transactional
    public StudentWithdrawalResponse withdrawStudent(Long studentId, StudentWithdrawalRequest request) {
        Long schoolId = TenantContext.getSchoolId();

        var student = studentRepository.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new InvalidOperationException("Student does not belong to this school."));

        List<StudentEnrollment> lockedEnrollments = enrollmentRepository.findByStudentIdAndSessionIdAndSchoolIdForUpdate(
                studentId, request.getSessionId(), schoolId);
        if (lockedEnrollments.isEmpty()) {
            throw new ResourceNotFoundException("Enrollment not found for student/session.");
        }

        StudentEnrollment enrollment = lockedEnrollments.get(0);
        if (!enrollment.isActive()) {
            return StudentWithdrawalResponse.builder()
                    .enrollmentClosed(false)
                    .futureAssignmentsDeactivated(0)
                    .futureAssignmentsSkippedDueToPayment(0)
                    .skippedAssignmentIds(List.of())
                    .transportUnenrolled(false)
                    .build();
        }

        validateWithdrawalDate(request.getSessionId(), schoolId, request.getWithdrawalDate());

        enrollment.setActive(false);
        enrollment.setEndDate(request.getWithdrawalDate());
        enrollmentRepository.save(enrollment);

        student.setReasonForLeaving(request.getReason());
        student.setDateOfLeaving(request.getWithdrawalDate());
        student.setCurrentStatus(request.getStatus());
        studentRepository.save(student);

        List<StudentFeeAssignment> futureAssignments = assignmentRepository
                .findByStudentIdAndSessionIdAndSchoolIdAndActiveTrueAndDueDateIsNotNullAndDueDateAfter(
                        enrollment.getStudentId(),
                        enrollment.getSessionId(),
                        schoolId,
                        request.getWithdrawalDate());

        long deactivatedCount = 0;
        long skippedCount = 0;
        List<Long> skippedAssignmentIds = new ArrayList<>();

        for (StudentFeeAssignment assignment : futureAssignments) {
            BigDecimal principalPaid = nz(assignment.getPrincipalPaid());
            BigDecimal lateFeePaid = nz(assignment.getLateFeePaid());

            if (principalPaid.compareTo(ZERO) == 0 && lateFeePaid.compareTo(ZERO) == 0) {
                assignment.setActive(false);
                deactivatedCount++;
            } else {
                skippedCount++;
                skippedAssignmentIds.add(assignment.getId());
            }
        }
        assignmentRepository.saveAll(futureAssignments);

        boolean transportUnenrolled = false;
        if (transportEnrollmentRepository.existsByStudentIdAndSessionIdAndSchoolIdAndActiveTrue(
                enrollment.getStudentId(), enrollment.getSessionId(), schoolId)) {
            transportEnrollmentService.unenrollStudent(enrollment.getStudentId(), enrollment.getSessionId());
            transportUnenrolled = true;
        }

        String actorId = resolveActorId();
        log.info(
                "student_withdrawal schoolId={} studentId={} sessionId={} actorId={} withdrawalDate={} reason=\"{}\" deactivatedCount={} skippedCount={} timestamp={}",
                schoolId,
                enrollment.getStudentId(),
                enrollment.getSessionId(),
                actorId,
                request.getWithdrawalDate(),
                request.getReason() != null ? request.getReason().trim() : "",
                deactivatedCount,
                skippedCount,
                LocalDateTime.now());

        return StudentWithdrawalResponse.builder()
                .enrollmentClosed(true)
                .futureAssignmentsDeactivated(deactivatedCount)
                .futureAssignmentsSkippedDueToPayment(skippedCount)
                .skippedAssignmentIds(skippedAssignmentIds)
                .transportUnenrolled(transportUnenrolled)
                .build();
    }

    private void validateWithdrawalDate(Long sessionId, Long schoolId, java.time.LocalDate withdrawalDate) {
        AcademicSession session = academicSessionRepository.findById(sessionId)
                .orElseThrow(() -> new InvalidOperationException("Session not found."));
        if (!schoolId.equals(session.getSchoolId())) {
            throw new InvalidOperationException("Session does not belong to this school.");
        }
        if (withdrawalDate == null) {
            throw new InvalidOperationException("withdrawalDate is required.");
        }
        if (session.getStartDate() == null || session.getEndDate() == null) {
            throw new InvalidOperationException("Session date range is not configured.");
        }
        if (withdrawalDate.isBefore(session.getStartDate()) || withdrawalDate.isAfter(session.getEndDate())) {
            throw new InvalidOperationException("withdrawalDate must be within session date range.");
        }
    }

    private String resolveActorId() {
        try {
            Long userId = SecurityUtil.userId();
            if (userId != null) {
                return String.valueOf(userId);
            }
        } catch (Exception ignored) {
        }
        try {
            return SecurityUtil.current().getUsername();
        } catch (Exception ignored) {
            return "UNKNOWN";
        }
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : ZERO;
    }
}
