package com.school.backend.fee.service;

import com.school.backend.common.enums.LateFeeType;
import com.school.backend.common.exception.BusinessException;
import com.school.backend.common.exception.InvalidOperationException;
import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.common.tenant.SessionContext;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.core.student.entity.Student;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.fee.dto.FeePaymentAllocationRequest;
import com.school.backend.fee.dto.FeePaymentDto;
import com.school.backend.fee.dto.FeePaymentRequest;
import com.school.backend.fee.dto.FeeTypeHeadSummaryDto;
import com.school.backend.fee.entity.*;
import com.school.backend.fee.repository.FeePaymentAllocationRepository;
import com.school.backend.fee.repository.FeePaymentRepository;
import com.school.backend.fee.repository.FeeStructureRepository;
import com.school.backend.fee.repository.LateFeeLogRepository;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FeePaymentService {

    private final FeePaymentRepository paymentRepository;
    private final StudentRepository studentRepository;
    private final StudentFeeAssignmentRepository assignmentRepository;
    private final LateFeeLogRepository lateFeeLogRepository;
    private final LateFeeCalculator lateFeeCalculator;
    private final FeeStructureRepository feeStructureRepository;
    private final FeePaymentAllocationRepository feePaymentAllocationRepository;

    // ---------------- PAY ----------------
    @Transactional
    public FeePaymentDto pay(FeePaymentRequest req) {

        if (!studentRepository.existsById(req.getStudentId())) {
            throw new ResourceNotFoundException("Student not found: " + req.getStudentId());
        }

        if (req.getAllocations() == null || req.getAllocations().isEmpty()) {
            throw new BusinessException("At least one allocation is required.");
        }

        Long sessionId = requireSessionId();
        if (req.getSessionId() != null && !req.getSessionId().equals(sessionId)) {
            throw new InvalidOperationException("Session mismatch between request and context");
        }
        Long schoolId = TenantContext.getSchoolId();
        LocalDate effectivePaymentDate = req.getPaymentDate() != null ? req.getPaymentDate() : LocalDate.now();

        BigDecimal totalPrincipalPaid = BigDecimal.ZERO;
        BigDecimal totalLateFeePaid = BigDecimal.ZERO;

        Map<Long, AssignmentAllocation> allocationByAssignmentId = new LinkedHashMap<>();
        Set<Long> processedAssignmentIds = new HashSet<>();

        List<StudentFeeAssignment> assignmentsToSave = new ArrayList<>();

        for (FeePaymentAllocationRequest allocReq : req.getAllocations()) {
            Long assignmentId = allocReq.getAssignmentId();
            BigDecimal amountProvided = allocReq.getPrincipalAmount();

            if (amountProvided.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(
                        "Allocation amount must be greater than zero for assignment ID: " + assignmentId);
            }

            if (!processedAssignmentIds.add(assignmentId)) {
                throw new BusinessException("Duplicate allocation for assignment ID: " + assignmentId);
            }

            // Fetch with PESSIMISTIC_WRITE lock
            StudentFeeAssignment assignment = assignmentRepository.findByIdWithLock(assignmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Fee assignment not found: " + assignmentId));

            // Security check: must belong to student and school
            if (!assignment.getStudentId().equals(req.getStudentId()) || !assignment.getSchoolId().equals(schoolId)) {
                throw new BusinessException("Unauthorized or mismatched assignment: " + assignmentId);
            }

            hydrateDefaults(assignment);

            BigDecimal principalDueBeforeLate = assignment.getAmount()
                    .subtract(assignment.getPrincipalPaid())
                    .subtract(assignment.getTotalDiscountAmount())
                    .subtract(assignment.getSponsorCoveredAmount());

            // 1. Accrue any pending late fees up to today/payment date
            BigDecimal incrementalLateFee = lateFeeCalculator.calculateLateFee(assignment, principalDueBeforeLate,
                    effectivePaymentDate);
            if (incrementalLateFee.compareTo(BigDecimal.ZERO) > 0) {
                assignment.setLateFeeAccrued(assignment.getLateFeeAccrued().add(incrementalLateFee));

                if (assignment.getLateFeeType() == LateFeeType.FLAT
                        || assignment.getLateFeeType() == LateFeeType.PERCENTAGE) {
                    assignment.setLateFeeApplied(true);
                }

                lateFeeLogRepository.save(LateFeeLog.builder()
                        .schoolId(schoolId)
                        .assignmentId(assignment.getId())
                        .computedAmount(incrementalLateFee)
                        .appliedDate(effectivePaymentDate)
                        .reason("Payment-time late fee accrual (Head-wise)")
                        .build());
            }

            BigDecimal lateFeeDue = assignment.getLateFeeAccrued()
                    .subtract(assignment.getLateFeePaid())
                    .subtract(assignment.getLateFeeWaived());

            if (lateFeeDue.compareTo(BigDecimal.ZERO) < 0)
                lateFeeDue = BigDecimal.ZERO;

            // 2. Strict Late Fee Policy: Must clear full late fee if pending
            if (lateFeeDue.compareTo(BigDecimal.ZERO) > 0 && amountProvided.compareTo(lateFeeDue) < 0) {
                throw new BusinessException(
                        "Late fee (₹ " + lateFeeDue + ") must be fully cleared first for assignment: " + assignmentId);
            }

            // 3. Overpayment Guard
            BigDecimal principalDue = assignment.getAmount()
                    .subtract(assignment.getPrincipalPaid())
                    .subtract(assignment.getTotalDiscountAmount())
                    .subtract(assignment.getSponsorCoveredAmount());
            if (principalDue.compareTo(BigDecimal.ZERO) < 0)
                principalDue = BigDecimal.ZERO;

            BigDecimal totalPending = lateFeeDue.add(principalDue);
            if (amountProvided.compareTo(totalPending) > 0) {
                throw new BusinessException("Payment (₹ " + amountProvided + ") exceeds total outstanding (₹ "
                        + totalPending + ") for assignment: " + assignmentId);
            }

            // 4. Perform Allocation
            BigDecimal lateFeeAllocation = amountProvided.min(lateFeeDue);
            BigDecimal principalAllocation = amountProvided.subtract(lateFeeAllocation);

            assignment.setLateFeePaid(assignment.getLateFeePaid().add(lateFeeAllocation));
            assignment.setPrincipalPaid(assignment.getPrincipalPaid().add(principalAllocation));

            totalLateFeePaid = totalLateFeePaid.add(lateFeeAllocation);
            totalPrincipalPaid = totalPrincipalPaid.add(principalAllocation);

            addAllocationPortion(allocationByAssignmentId, assignment.getId(), principalAllocation, lateFeeAllocation);
            assignmentsToSave.add(assignment);
        }

        // Save updated assignments
        assignmentRepository.saveAll(assignmentsToSave);

        FeePayment savedPayment = paymentRepository.save(FeePayment.builder()
                .studentId(req.getStudentId())
                .sessionId(sessionId)
                .principalPaid(totalPrincipalPaid)
                .lateFeePaid(totalLateFeePaid)
                .paymentDate(effectivePaymentDate)
                .mode(req.getMode())
                .transactionReference(req.getTransactionReference())
                .remarks(req.getRemarks())
                .schoolId(schoolId)
                .build());

        savePaymentAllocations(savedPayment, assignmentsToSave, allocationByAssignmentId, sessionId, schoolId);

        return toDto(savedPayment);
    }

    @Transactional(readOnly = true)
    public List<FeePaymentDto> getStudentPayments(Long studentId) {
        return getStudentPayments(studentId, requireSessionId());
    }

    @Transactional(readOnly = true)
    public List<FeePaymentDto> getStudentPayments(Long studentId, Long sessionId) {
        Long schoolId = TenantContext.getSchoolId();
        Long effectiveSessionId = sessionId != null ? sessionId : requireSessionId();

        Optional<Student> student = studentRepository.findByIdAndSchoolId(studentId, schoolId);
        if (student.isEmpty()) {
            throw new ResourceNotFoundException("Student not found: " + studentId);
        }
        String studentName = buildStudentName(student.get().getFirstName(), student.get().getLastName());

        List<FeePayment> payments = paymentRepository
                .findByStudentIdAndSessionIdAndSchoolIdOrderByPaymentDateDescIdDesc(
                        studentId, effectiveSessionId, schoolId);

        return payments
                .stream()
                .map(payment -> toDto(payment, studentName))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeePaymentDto> getRecentPayments(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        Pageable pageable = PageRequest.of(0, safeLimit);
        return paymentRepository.findRecentPayments(TenantContext.getSchoolId(), pageable)
                .stream()
                .map(p -> toDto(p.getPayment(), buildStudentName(p.getFirstName(), p.getLastName())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeeTypeHeadSummaryDto> getHeadSummaryByDate(LocalDate date) {
        return feePaymentAllocationRepository.findHeadSummaryBySchoolSessionAndDate(
                TenantContext.getSchoolId(),
                requireSessionId(),
                date);
    }

    // ---------------- MAPPER ----------------
    private FeePaymentDto toDto(FeePayment p) {
        return toDto(p, null);
    }

    private FeePaymentDto toDto(FeePayment p, String studentName) {

        FeePaymentDto dto = new FeePaymentDto();

        dto.setId(p.getId());
        dto.setStudentId(p.getStudentId());
        dto.setStudentName(studentName);
        dto.setSessionId(p.getSessionId());
        dto.setPrincipalPaid(p.getPrincipalPaid());
        dto.setLateFeePaid(p.getLateFeePaid());
        dto.setAmountPaid(p.getPrincipalPaid().add(p.getLateFeePaid()));
        dto.setPaymentDate(p.getPaymentDate());
        dto.setMode(p.getMode());
        dto.setTransactionReference(p.getTransactionReference());
        dto.setRemarks(p.getRemarks());

        return dto;
    }

    private String buildStudentName(String firstName, String lastName) {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        String fullName = (first + " " + last).trim();
        return fullName.isEmpty() ? null : fullName;
    }

    private void hydrateDefaults(StudentFeeAssignment assignment) {
        if (assignment.getLateFeeAccrued() == null) {
            assignment.setLateFeeAccrued(BigDecimal.ZERO);
        }
        if (assignment.getLateFeePaid() == null) {
            assignment.setLateFeePaid(BigDecimal.ZERO);
        }
        if (assignment.getLateFeeWaived() == null) {
            assignment.setLateFeeWaived(BigDecimal.ZERO);
        }
        if (assignment.getPrincipalPaid() == null) {
            assignment.setPrincipalPaid(BigDecimal.ZERO);
        }
        if (assignment.getTotalDiscountAmount() == null) {
            assignment.setTotalDiscountAmount(BigDecimal.ZERO);
        }
        if (assignment.getAmount() == null) {
            assignment.setAmount(BigDecimal.ZERO);
        }
    }

    private void addAllocationPortion(
            Map<Long, AssignmentAllocation> allocationByAssignmentId,
            Long assignmentId,
            BigDecimal principal,
            BigDecimal lateFee) {
        AssignmentAllocation allocation = allocationByAssignmentId.computeIfAbsent(assignmentId,
                ignored -> new AssignmentAllocation());
        allocation.principal = allocation.principal.add(nz(principal));
        allocation.lateFee = allocation.lateFee.add(nz(lateFee));
    }

    private void savePaymentAllocations(
            FeePayment payment,
            List<StudentFeeAssignment> assignments,
            Map<Long, AssignmentAllocation> allocationByAssignmentId,
            Long sessionId,
            Long schoolId) {
        if (allocationByAssignmentId.isEmpty()) {
            if (payment.getPrincipalPaid().add(payment.getLateFeePaid()).compareTo(BigDecimal.ZERO) > 0) {
                throw new IllegalStateException("No allocation rows generated for fee payment " + payment.getId());
            }
            return;
        }

        List<Long> feeStructureIds = assignments.stream()
                .map(StudentFeeAssignment::getFeeStructureId)
                .distinct()
                .toList();

        Map<Long, FeeStructure> feeStructureById = feeStructureRepository
                .findByIdInAndSchoolId(feeStructureIds, schoolId)
                .stream()
                .collect(java.util.stream.Collectors.toMap(FeeStructure::getId, fs -> fs));

        List<FeePaymentAllocation> allocationRows = new ArrayList<>();
        for (StudentFeeAssignment assignment : assignments) {
            AssignmentAllocation rowAllocation = allocationByAssignmentId.get(assignment.getId());
            if (rowAllocation == null) {
                continue;
            }
            BigDecimal principalAmount = nz(rowAllocation.principal);
            BigDecimal lateFeeAmount = nz(rowAllocation.lateFee);

            if (principalAmount.compareTo(BigDecimal.ZERO) < 0 || lateFeeAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException("Negative allocation generated for assignment " + assignment.getId());
            }
            if (principalAmount.compareTo(BigDecimal.ZERO) == 0 && lateFeeAmount.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            FeeStructure feeStructure = feeStructureById.get(assignment.getFeeStructureId());
            if (feeStructure == null || feeStructure.getFeeType() == null) {
                throw new ResourceNotFoundException(
                        "Fee structure or fee type not found for assignment " + assignment.getId());
            }

            allocationRows.add(FeePaymentAllocation.builder()
                    .feePaymentId(payment.getId())
                    .assignmentId(assignment.getId())
                    .feeType(feeStructure.getFeeType())
                    .principalAmount(principalAmount)
                    .lateFeeAmount(lateFeeAmount)
                    .sessionId(sessionId)
                    .schoolId(schoolId)
                    .build());
        }

        BigDecimal totalPrincipalAllocated = allocationRows.stream()
                .map(FeePaymentAllocation::getPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalLateFeeAllocated = allocationRows.stream()
                .map(FeePaymentAllocation::getLateFeeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPrincipalAllocated.compareTo(payment.getPrincipalPaid()) != 0
                || totalLateFeeAllocated.compareTo(payment.getLateFeePaid()) != 0) {
            throw new IllegalStateException("Payment allocation mismatch for fee payment " + payment.getId());
        }

        if (!allocationRows.isEmpty()) {
            feePaymentAllocationRepository.saveAll(allocationRows);
        }
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private Long requireSessionId() {
        Long sessionId = SessionContext.getSessionId();
        if (sessionId == null) {
            throw new InvalidOperationException("Session context is missing in request");
        }
        return sessionId;
    }

    private static final class AssignmentAllocation {
        private BigDecimal principal = BigDecimal.ZERO;
        private BigDecimal lateFee = BigDecimal.ZERO;
    }
}
