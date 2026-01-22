package com.school.backend.fee.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.fee.dto.FeeSummaryDto;
import com.school.backend.fee.entity.FeePayment;
import com.school.backend.fee.entity.FeeStructure;
import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.fee.repository.FeePaymentRepository;
import com.school.backend.fee.repository.FeeStructureRepository;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeeSummaryService {

    private final StudentRepository studentRepository;
    private final FeeStructureRepository feeStructureRepository;
    private final StudentFeeAssignmentRepository assignmentRepository;
    private final FeePaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public FeeSummaryDto getStudentFeeSummary(Long studentId, String session) {

        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found: " + studentId);
        }

        // 1. Fetch assigned fee structures
        List<StudentFeeAssignment> assignments =
                assignmentRepository.findByStudentIdAndSession(studentId, session);

        if (assignments.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No fee structure assigned for student " + studentId + " in session " + session);
        }

        // 2. Calculate total fee
        int totalFee = assignments.stream()
                .map(a -> feeStructureRepository.findById(a.getFeeStructureId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "FeeStructure not found: " + a.getFeeStructureId())))
                .mapToInt(FeeStructure::getAmount)
                .sum();

        // 3. Calculate total paid
        int totalPaid = paymentRepository.findByStudentId(studentId)
                .stream()
                .mapToInt(FeePayment::getAmountPaid)
                .sum();

        // 4. Pending
        int pending = Math.max(totalFee - totalPaid, 0);

        // 5. Build response
        FeeSummaryDto dto = new FeeSummaryDto();
        dto.setStudentId(studentId);
        dto.setSession(session);
        dto.setTotalFee(totalFee);
        dto.setTotalPaid(totalPaid);
        dto.setPendingFee(pending);
        dto.setFeePending(pending > 0);

        return dto;
    }
}
