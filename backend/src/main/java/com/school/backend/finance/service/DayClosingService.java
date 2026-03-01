package com.school.backend.finance.service;

import com.school.backend.common.exception.BusinessException;
import com.school.backend.common.exception.InvalidOperationException;
import com.school.backend.common.tenant.SessionContext;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.finance.dto.DailyCashDashboardDto;
import com.school.backend.finance.dto.DayClosingDto;
import com.school.backend.finance.entity.DayClosing;
import com.school.backend.finance.repository.DayClosingRepository;
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
    private final FinanceOverviewService financeOverviewService;

    @Transactional
    public DayClosingDto closeDate(LocalDate date) {
        LocalDate closeDate = date != null ? date : LocalDate.now();
        if (closeDate.isAfter(LocalDate.now())) {
            throw new BusinessException("Cannot close a future date");
        }

        Long schoolId = TenantContext.getSchoolId();
        // Session ID is still needed for generic TenantEntity compliance but Reporting
        // should be date-based.
        Long sessionId = SessionContext.getSessionId();
        if (sessionId == null) {
            throw new InvalidOperationException("Session context is missing in request");
        }

        DayClosing existing = dayClosingRepository.findBySchoolIdAndDate(schoolId, closeDate).orElse(null);
        if (existing != null && !existing.isOverrideAllowed()) {
            throw new InvalidOperationException("Date already closed");
        }

        // 1. Fetch Today's Overview (includes Opening Balance logic)
        DailyCashDashboardDto daily = financeOverviewService.getDailyOverview(closeDate);

        // 2. Map totals
        DayClosing target = existing != null ? existing : new DayClosing();
        target.setSchoolId(schoolId);
        target.setSessionId(sessionId);
        target.setDate(closeDate);

        // Opening bases are derived from yesterday in Service
        // We fetch yesterday explicitly to store it in DayClosing record for audit
        DayClosing previous = dayClosingRepository
                .findFirstBySchoolIdAndDateLessThanOrderByDateDesc(schoolId, closeDate)
                .orElse(null);

        target.setOpeningCash(previous != null ? nz(previous.getClosingCash()) : BigDecimal.ZERO);
        target.setOpeningBank(previous != null ? nz(previous.getClosingBank()) : BigDecimal.ZERO);

        target.setCashRevenue(daily.getCashRevenue());
        target.setBankRevenue(daily.getBankRevenue());
        target.setCashExpense(daily.getCashExpense());
        target.setBankExpense(daily.getBankExpense());
        target.setTransferOut(daily.getTransferOut());
        target.setTransferIn(daily.getTransferIn());
        target.setClosingCash(daily.getNetCash()); // getNetCash returns closing position
        target.setClosingBank(daily.getNetBank()); // getNetBank returns closing position
        target.setOverrideAllowed(false);
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
