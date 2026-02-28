package com.school.backend.finance.service;

import com.school.backend.common.exception.BusinessException;
import com.school.backend.common.exception.InvalidOperationException;
import com.school.backend.common.tenant.SessionContext;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.finance.dto.FinanceAccountTransferDto;
import com.school.backend.finance.entity.FinanceAccountTransfer;
import com.school.backend.finance.repository.DayClosingRepository;
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
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private static final java.util.Set<String> CASH_MODES = java.util.Set.of("CASH");
    private static final java.util.Set<String> BANK_MODES = java.util.Set.of("BANK", "UPI", "ONLINE", "BANK_TRANSFER",
            "CHEQUE");

    private final FinanceAccountTransferRepository transferRepository;
    private final AcademicSessionRepository academicSessionRepository;
    private final DayClosingRepository dayClosingRepository;

    @Transactional
    public FinanceAccountTransferDto createTransfer(
            Long requestedSessionId,
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
        if (requestedSessionId != null && !requestedSessionId.equals(sessionId)) {
            throw new InvalidOperationException("Session mismatch between request and context");
        }

        AcademicSession session = academicSessionRepository.findById(sessionId)
                .filter(s -> schoolId.equals(s.getSchoolId()))
                .orElseThrow(() -> new InvalidOperationException("Session not found for school: " + sessionId));

        LocalDate transferDate = date != null ? date : LocalDate.now();
        if (dayClosingRepository.existsBySchoolIdAndDateAndOverrideAllowedFalse(schoolId, transferDate)) {
            throw new InvalidOperationException("Date already closed");
        }
        LocalDate sessionStart = session.getStartDate();
        LocalDate sessionEnd = session.getEndDate() != null ? session.getEndDate() : LocalDate.now();
        if (sessionStart == null) {
            throw new InvalidOperationException("Session start date is missing: " + sessionId);
        }
        if (transferDate.isBefore(sessionStart) || transferDate.isAfter(sessionEnd)) {
            throw new BusinessException("Transfer date must be within session date range.");
        }

        BigDecimal availableCash = computeAvailableCashBeforeNewTransfer(schoolId, sessionId, sessionStart,
                transferDate);
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
        return transferRepository
                .findBySchoolIdAndSessionIdAndTransferDateBetween(schoolId, sessionId, fromDate, toDate)
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

        // M-05: Fetch session opening balance from day-closing or session setup
        BigDecimal openingBalance = dayClosingRepository
                .findFirstBySchoolIdAndDateLessThanOrderByDateDesc(schoolId, sessionStart)
                .map(com.school.backend.finance.entity.DayClosing::getClosingCash)
                .orElse(ZERO);

        if (openingBalance.equals(ZERO)) {
            // TODO: If opening balance is still ZERO, check if it was initialized in
            // academic_sessions
            // For now, logging warning to avoid silent assumptions.
            // log.warn("Opening balance for schoolId={} sessionId={} is ZERO. Ensure
            // initialization is complete.", schoolId, sessionId);
        }

        FinanceAccountTransferRepository.MovementAggregate totals = transferRepository
                .aggregateMovements(schoolId, sessionId, sessionStart, transferDate);

        BigDecimal cashRevenue = totals != null ? nz(totals.getCashRevenue()) : ZERO;
        BigDecimal cashExpense = totals != null ? nz(totals.getCashExpense()) : ZERO;
        BigDecimal transferOut = totals != null ? nz(totals.getTransferOut()) : ZERO;

        BigDecimal available = openingBalance
                .add(cashRevenue)
                .subtract(cashExpense)
                .subtract(transferOut);
        return available.compareTo(ZERO) < 0 ? ZERO : available;
    }

    public static boolean isCashMode(String mode) {
        if (mode == null)
            return false;
        String m = mode.trim().toUpperCase();
        if (CASH_MODES.contains(m))
            return true;
        if (BANK_MODES.contains(m))
            return false;
        throw new BusinessException("Unknown payment mode: " + mode);
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
        return value != null ? value : ZERO;
    }
}
