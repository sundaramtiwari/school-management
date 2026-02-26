package com.school.backend.finance.service;

import com.school.backend.common.exception.BusinessException;
import com.school.backend.common.exception.InvalidOperationException;
import com.school.backend.common.tenant.SessionContext;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.core.dashboard.dto.DailyCashDashboardDto;
import com.school.backend.core.dashboard.service.DashboardStatsService;
import com.school.backend.finance.dto.DayClosingDto;
import com.school.backend.finance.entity.DayClosing;
import com.school.backend.finance.repository.DayClosingRepository;
import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.repository.AcademicSessionRepository;
import com.school.backend.user.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DayClosingService {

    private final DayClosingRepository dayClosingRepository;
    private final AcademicSessionRepository academicSessionRepository;
    private final DashboardStatsService dashboardStatsService;
    private final FinanceAccountTransferService financeAccountTransferService;

    @Transactional
    public DayClosingDto closeDate(LocalDate date) {
        LocalDate closeDate = date != null ? date : LocalDate.now();
        if (closeDate.isAfter(LocalDate.now())) {
            throw new BusinessException("Cannot close a future date");
        }

        Long schoolId = TenantContext.getSchoolId();
        Long sessionId = SessionContext.getSessionId();
        if (sessionId == null) {
            throw new InvalidOperationException("Session context is missing in request");
        }

        AcademicSession session = academicSessionRepository.findById(sessionId)
                .filter(s -> schoolId.equals(s.getSchoolId()))
                .orElseThrow(() -> new InvalidOperationException("Session not found for school: " + sessionId));

        if (session.getStartDate() != null && closeDate.isBefore(session.getStartDate())) {
            throw new BusinessException("Date is before session start");
        }
        if (session.getEndDate() != null && closeDate.isAfter(session.getEndDate())) {
            throw new BusinessException("Date is after session end");
        }

        DayClosing existing = dayClosingRepository.findBySchoolIdAndDate(schoolId, closeDate).orElse(null);
        if (existing != null && !existing.isOverrideAllowed()) {
            throw new InvalidOperationException("Date already closed");
        }

        DayClosing previous = dayClosingRepository
                .findFirstBySchoolIdAndDateLessThanOrderByDateDesc(schoolId, closeDate)
                .orElse(null);

        BigDecimal openingCash = previous != null ? nz(previous.getClosingCash()) : BigDecimal.ZERO;
        BigDecimal openingBank = previous != null ? nz(previous.getClosingBank()) : BigDecimal.ZERO;

        DailyCashDashboardDto daily = dashboardStatsService.getDailyCashDashboard(closeDate);
        BigDecimal transfer = financeAccountTransferService.sumTransferAmount(schoolId, sessionId, closeDate, closeDate);

        BigDecimal cashRevenue = nz(daily.getCashRevenue());
        BigDecimal bankRevenue = nz(daily.getBankRevenue());
        BigDecimal cashExpense = nz(daily.getCashExpense());
        BigDecimal bankExpense = nz(daily.getBankExpense());
        BigDecimal transferOut = transfer;
        BigDecimal transferIn = transfer;

        BigDecimal closingCash = openingCash
                .add(cashRevenue)
                .subtract(cashExpense)
                .subtract(transferOut);
        BigDecimal closingBank = openingBank
                .add(bankRevenue)
                .subtract(bankExpense)
                .add(transferIn);

        DayClosing target = existing != null ? existing : new DayClosing();
        target.setSchoolId(schoolId);
        target.setSessionId(sessionId);
        target.setDate(closeDate);
        target.setOpeningCash(openingCash);
        target.setOpeningBank(openingBank);
        target.setCashRevenue(cashRevenue);
        target.setBankRevenue(bankRevenue);
        target.setCashExpense(cashExpense);
        target.setBankExpense(bankExpense);
        target.setTransferOut(transferOut);
        target.setTransferIn(transferIn);
        target.setClosingCash(closingCash);
        target.setClosingBank(closingBank);
        target.setClosedBy(SecurityUtil.userId());
        target.setClosedAt(LocalDateTime.now());

        DayClosing saved = dayClosingRepository.save(target);
        return toDto(saved);
    }

    @Transactional
    public DayClosingDto allowOverride(LocalDate date) {
        Long schoolId = TenantContext.getSchoolId();
        DayClosing closing = dayClosingRepository.findBySchoolIdAndDate(schoolId, date)
                .orElseThrow(() -> new InvalidOperationException("Day closing not found for date: " + date));
        closing.setOverrideAllowed(true);
        DayClosing saved = dayClosingRepository.save(closing);

        log.info("event=DAY_CLOSING_OVERRIDE schoolId={} date={} overriddenBy={}",
                schoolId, date, SecurityUtil.userId());
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public boolean isDateLocked(LocalDate date) {
        Long schoolId = TenantContext.getSchoolId();
        return dayClosingRepository.existsBySchoolIdAndDateAndOverrideAllowedFalse(schoolId, date);
    }

    private DayClosingDto toDto(DayClosing d) {
        return DayClosingDto.builder()
                .id(d.getId())
                .sessionId(d.getSessionId())
                .date(d.getDate())
                .openingCash(d.getOpeningCash())
                .openingBank(d.getOpeningBank())
                .cashRevenue(d.getCashRevenue())
                .bankRevenue(d.getBankRevenue())
                .cashExpense(d.getCashExpense())
                .bankExpense(d.getBankExpense())
                .transferOut(d.getTransferOut())
                .transferIn(d.getTransferIn())
                .closingCash(d.getClosingCash())
                .closingBank(d.getClosingBank())
                .overrideAllowed(d.isOverrideAllowed())
                .closedBy(d.getClosedBy())
                .closedAt(d.getClosedAt())
                .build();
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
