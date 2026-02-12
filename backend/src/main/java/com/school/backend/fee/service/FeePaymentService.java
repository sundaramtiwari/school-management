package com.school.backend.fee.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.fee.dto.FeePaymentDto;
import com.school.backend.fee.dto.FeePaymentRequest;
import com.school.backend.fee.entity.FeePayment;
import com.school.backend.fee.repository.FeePaymentRepository;
import com.school.backend.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeePaymentService {

    private final FeePaymentRepository paymentRepository;
    private final StudentRepository studentRepository;
    private final SchoolRepository schoolRepository;

    // ---------------- PAY ----------------
    @Transactional
    public FeePaymentDto pay(FeePaymentRequest req) {

        if (!studentRepository.existsById(req.getStudentId())) {
            throw new ResourceNotFoundException("Student not found: " + req.getStudentId());
        }

        Long sessionId = req.getSessionId();
        if (sessionId == null) {
            sessionId = schoolRepository.findById(TenantContext.getSchoolId())
                    .map(com.school.backend.school.entity.School::getCurrentSessionId)
                    .orElse(null);
        }

        FeePayment payment = FeePayment.builder()
                .studentId(req.getStudentId())
                .sessionId(sessionId)
                .amountPaid(req.getAmountPaid())
                .paymentDate(
                        req.getPaymentDate() != null
                                ? req.getPaymentDate()
                                : LocalDate.now())
                .mode(req.getMode())
                .remarks(req.getRemarks())
                .schoolId(TenantContext.getSchoolId())
                .build();

        return toDto(paymentRepository.save(payment));
    }

    // ---------------- HISTORY ----------------
    @Transactional(readOnly = true)
    public List<FeePaymentDto> getHistory(Long studentId) {

        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found: " + studentId);
        }

        return paymentRepository.findByStudentId(studentId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeePaymentDto> getRecentPayments(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return paymentRepository.findRecentPayments(TenantContext.getSchoolId(), pageable)
                .stream()
                .map(this::toDto)
                .toList();
    }

    // ---------------- MAPPER ----------------
    private FeePaymentDto toDto(FeePayment p) {

        FeePaymentDto dto = new FeePaymentDto();

        dto.setId(p.getId());
        dto.setStudentId(p.getStudentId());
        dto.setAmountPaid(p.getAmountPaid());
        dto.setPaymentDate(p.getPaymentDate());
        dto.setMode(p.getMode());
        dto.setRemarks(p.getRemarks());

        return dto;
    }
}
