package com.school.backend.student.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.fee.repository.FeePaymentRepository;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.repository.AcademicSessionRepository;
import com.school.backend.student.dto.LedgerSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentLedgerService {

    private final StudentRepository studentRepository;
    private final StudentFeeAssignmentRepository studentFeeAssignmentRepository;
    private final FeePaymentRepository feePaymentRepository;
    private final AcademicSessionRepository academicSessionRepository;

    @Transactional(readOnly = true)
    public List<LedgerSummaryDto> getStudentLedger(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found: " + studentId);
        }

        Map<Long, LedgerSummaryDto> ledgerMap = new LinkedHashMap<>();
        mergeAssigned(studentId, ledgerMap);
        mergePaid(studentId, ledgerMap);

        Set<Long> sessionIds = ledgerMap.keySet();
        Map<Long, String> sessionNames = academicSessionRepository.findAllById(sessionIds)
                .stream()
                .collect(Collectors.toMap(AcademicSession::getId, AcademicSession::getName));

        return ledgerMap.values().stream()
                .peek(item -> {
                    item.setSessionName(sessionNames.getOrDefault(item.getSessionId(), "Unknown Session"));
                    item.setTotalPending(item.getTotalAssigned()
                            .add(item.getTotalLateFee())
                            .subtract(item.getTotalDiscount())
                            .subtract(item.getTotalFunding())
                            .subtract(item.getTotalPaid()));
                })
                .sorted(Comparator.comparing(LedgerSummaryDto::getSessionId))
                .toList();
    }

    private void mergeAssigned(Long studentId, Map<Long, LedgerSummaryDto> ledgerMap) {
        List<Object[]> summary = studentFeeAssignmentRepository.sumFinancialSummaryByStudentGroupedBySession(studentId);
        for (Object[] row : summary) {
            Long sessionId = (Long) row[0];
            if (sessionId == null) {
                continue;
            }
            LedgerSummaryDto dto = ledgerMap.computeIfAbsent(sessionId, this::newLedgerEntry);
            dto.setTotalAssigned(row[1] == null ? BigDecimal.ZERO : (BigDecimal) row[1]);
            dto.setTotalDiscount(row[2] == null ? BigDecimal.ZERO : (BigDecimal) row[2]);
            dto.setTotalFunding(row[3] == null ? BigDecimal.ZERO : (BigDecimal) row[3]);
            dto.setTotalLateFee(row[4] == null ? BigDecimal.ZERO : (BigDecimal) row[4]);
        }
    }

    private void mergePaid(Long studentId, Map<Long, LedgerSummaryDto> ledgerMap) {
        List<Object[]> paid = feePaymentRepository.sumPaidByStudentGroupedBySession(studentId);
        for (Object[] row : paid) {
            Long sessionId = (Long) row[0];
            if (sessionId == null) {
                continue;
            }
            BigDecimal total = row[1] == null ? BigDecimal.ZERO : (BigDecimal) row[1];
            LedgerSummaryDto dto = ledgerMap.computeIfAbsent(sessionId, this::newLedgerEntry);
            dto.setTotalPaid(total);
        }
    }

    private LedgerSummaryDto newLedgerEntry(Long sessionId) {
        LedgerSummaryDto dto = new LedgerSummaryDto();
        dto.setSessionId(sessionId);
        return dto;
    }
}
