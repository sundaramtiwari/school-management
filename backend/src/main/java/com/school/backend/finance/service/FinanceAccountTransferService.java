package com.school.backend.finance.service;

import com.school.backend.common.exception.BusinessException;
import com.school.backend.common.exception.InvalidOperationException;
import com.school.backend.common.tenant.SessionContext;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.core.dashboard.dto.DailyCashDashboardDto;
import com.school.backend.core.dashboard.service.DashboardStatsService;
import com.school.backend.finance.dto.FinanceAccountTransferDto;
import com.school.backend.finance.entity.FinanceAccountTransfer;
import com.school.backend.finance.repository.FinanceAccountTransferRepository;
import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.repository.AcademicSessionRepository;
import com.school.backend.user.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinanceAccountTransferService {

    private static final String FROM_ACCOUNT = "CASH";
    private static final String TO_ACCOUNT = "BANK";

    private final FinanceAccountTransferRepository transferRepository;
    private final AcademicSessionRepository academicSessionRepository;
    private final DashboardStatsService dashboardStatsService;

    @Transactional
    public FinanceAccountTransferDto createTransfer(
            LocalDate date,
            BigDecimal amount,
            String referenceNumber,
            String remarks) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Transfer amount must be greater than zero.");
        }

        Long schoolId = TenantContext.getSchoolId();
        Long sessionId = SessionContext.getSessionId();
        if (sessionId == null) {
            throw new InvalidOperationException("Session context is missing in request");
        }

        AcademicSession session = academicSessionRepository.findById(sessionId)
                .filter(s -> schoolId.equals(s.getSchoolId()))
                .orElseThrow(() -> new InvalidOperationException("Session not found for school: " + sessionId));

        LocalDate transferDate = date != null ? date : LocalDate.now();
        LocalDate sessionStart = session.getStartDate();
        LocalDate sessionEnd = session.getEndDate() != null ? session.getEndDate() : LocalDate.now();
        if (sessionStart == null) {
            throw new InvalidOperationException("Session start date is missing: " + sessionId);
        }
        if (transferDate.isBefore(sessionStart) || transferDate.isAfter(sessionEnd)) {
            throw new BusinessException("Transfer date must be within session date range.");
        }

        BigDecimal availableCash = computeAvailableCashBeforeNewTransfer(schoolId, sessionId, sessionStart, transferDate);
        if (amount.compareTo(availableCash) > 0) {
            throw new BusinessException("Transfer exceeds available cash balance. Available: " + availableCash);
        }

        FinanceAccountTransfer saved = transferRepository.save(FinanceAccountTransfer.builder()
                .schoolId(schoolId)
                .sessionId(sessionId)
                .transferDate(transferDate)
                .amount(amount)
                .fromAccount(FROM_ACCOUNT)
                .toAccount(TO_ACCOUNT)
                .referenceNumber(trimToNull(referenceNumber))
                .remarks(trimToNull(remarks))
                .createdBy(SecurityUtil.userId())
                .build());

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<FinanceAccountTransferDto> listTransfers(LocalDate fromDate, LocalDate toDate) {
        Long schoolId = TenantContext.getSchoolId();
        Long sessionId = SessionContext.getSessionId();
        if (sessionId == null) {
            throw new InvalidOperationException("Session context is missing in request");
        }

        LocalDate from = fromDate != null ? fromDate : LocalDate.now().minusDays(30);
        LocalDate to = toDate != null ? toDate : LocalDate.now();
        if (to.isBefore(from)) {
            throw new BusinessException("toDate must be on or after fromDate");
        }

        return transferRepository.findBySchoolIdAndSessionIdAndTransferDateBetween(schoolId, sessionId, from, to)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal sumTransferAmount(Long schoolId, Long sessionId, LocalDate fromDate, LocalDate toDate) {
        return transferRepository.findBySchoolIdAndSessionIdAndTransferDateBetween(schoolId, sessionId, fromDate, toDate)
                .stream()
                .map(FinanceAccountTransfer::getAmount)
                .map(this::nz)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal computeAvailableCashBeforeNewTransfer(
            Long schoolId,
            Long sessionId,
            LocalDate sessionStart,
            LocalDate transferDate) {
        // Opening balance is currently zero; structure remains compatible for day-closing extension.
        BigDecimal openingBalance = BigDecimal.ZERO;
        BigDecimal cumulativeOperationalCash = BigDecimal.ZERO;

        LocalDate cursor = sessionStart;
        while (!cursor.isAfter(transferDate)) {
            DailyCashDashboardDto daily = dashboardStatsService.getDailyCashDashboard(cursor);
            // add only operational movement; transfers handled separately below
            cumulativeOperationalCash = cumulativeOperationalCash
                    .add(nz(daily.getCashRevenue()))
                    .subtract(nz(daily.getCashExpense()));
            cursor = cursor.plusDays(1);
        }

        BigDecimal previousTransfers = sumTransferAmount(schoolId, sessionId, sessionStart, transferDate);
        BigDecimal available = openingBalance.add(cumulativeOperationalCash).subtract(previousTransfers);
        return available.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : available;
    }

    private FinanceAccountTransferDto toDto(FinanceAccountTransfer transfer) {
        return FinanceAccountTransferDto.builder()
                .id(transfer.getId())
                .sessionId(transfer.getSessionId())
                .transferDate(transfer.getTransferDate())
                .amount(transfer.getAmount())
                .fromAccount(transfer.getFromAccount())
                .toAccount(transfer.getToAccount())
                .referenceNumber(transfer.getReferenceNumber())
                .remarks(transfer.getRemarks())
                .createdBy(transfer.getCreatedBy())
                .createdAt(transfer.getCreatedAt())
                .build();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
