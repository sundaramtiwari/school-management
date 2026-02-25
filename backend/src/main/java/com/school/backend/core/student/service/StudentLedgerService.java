package com.school.backend.core.student.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.fee.service.FeeMath;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.repository.AcademicSessionRepository;
import com.school.backend.core.student.dto.LedgerSummaryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class StudentLedgerService {

    private final StudentRepository studentRepository;
    private final StudentFeeAssignmentRepository studentFeeAssignmentRepository;
    private final AcademicSessionRepository academicSessionRepository;

    @Transactional(readOnly = true)
    public List<LedgerSummaryDto> getStudentLedger(Long studentId) {
        log.debug("Fetching financial ledger for studentId={}", studentId);
        if (!studentRepository.existsById(studentId)) {
            log.warn("Ledger lookup failed: student not found studentId={}", studentId);
            throw new ResourceNotFoundException("Student not found: " + studentId);
        }

        Map<Long, LedgerSummaryDto> ledgerMap = new LinkedHashMap<>();
        mergeAssigned(studentId, ledgerMap);

        Set<Long> sessionIds = ledgerMap.keySet();
        log.debug("Ledger aggregation complete for studentId={}, sessionsFound={}", studentId, sessionIds.size());
        Map<Long, String> sessionNames = academicSessionRepository.findAllById(sessionIds)
                .stream()
                .collect(Collectors.toMap(AcademicSession::getId, AcademicSession::getName));

        List<LedgerSummaryDto> result = ledgerMap.values().stream()
                .peek(item -> {
                    item.setSessionName(sessionNames.getOrDefault(item.getSessionId(), "Unknown Session"));
                })
                .sorted(Comparator.comparing(LedgerSummaryDto::getSessionId))
                .toList();
        log.debug("Ledger response prepared for studentId={}, rowCount={}", studentId, result.size());
        return result;
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

            BigDecimal lateFeePaid = row[5] == null ? BigDecimal.ZERO : (BigDecimal) row[5];
            BigDecimal lateFeeWaived = row[6] == null ? BigDecimal.ZERO : (BigDecimal) row[6];
            BigDecimal principalPaid = row[7] == null ? BigDecimal.ZERO : (BigDecimal) row[7];

            dto.setTotalPaid(principalPaid.add(lateFeePaid));
            dto.setTotalPending(FeeMath.computePending(StudentFeeAssignment.builder()
                    .amount(dto.getTotalAssigned())
                    .lateFeeAccrued(dto.getTotalLateFee())
                    .totalDiscountAmount(dto.getTotalDiscount())
                    .sponsorCoveredAmount(dto.getTotalFunding())
                    .lateFeeWaived(lateFeeWaived)
                    .principalPaid(principalPaid)
                    .lateFeePaid(lateFeePaid)
                    .build()));
        }
    }

    private LedgerSummaryDto newLedgerEntry(Long sessionId) {
        LedgerSummaryDto dto = new LedgerSummaryDto();
        dto.setSessionId(sessionId);
        dto.setTotalAssigned(BigDecimal.ZERO);
        dto.setTotalDiscount(BigDecimal.ZERO);
        dto.setTotalFunding(BigDecimal.ZERO);
        dto.setTotalLateFee(BigDecimal.ZERO);
        dto.setTotalPaid(BigDecimal.ZERO);
        dto.setTotalPending(BigDecimal.ZERO);
        return dto;
    }
}
