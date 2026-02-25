package com.school.backend.finance.service;

import com.school.backend.common.exception.InvalidOperationException;
import com.school.backend.common.tenant.SessionContext;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.expense.entity.ExpenseVoucher;
import com.school.backend.expense.repository.ExpenseVoucherRepository;
import com.school.backend.fee.entity.FeePayment;
import com.school.backend.fee.repository.FeePaymentRepository;
import com.school.backend.finance.dto.MonthlyPLResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinanceReportingService {

    private final FeePaymentRepository feePaymentRepository;
    private final ExpenseVoucherRepository expenseVoucherRepository;

    @Transactional(readOnly = true)
    public MonthlyPLResponseDto getMonthlyPL(int year, int month) {
        Long schoolId = TenantContext.getSchoolId();
        Long sessionId = SessionContext.getSessionId();
        if (sessionId == null) {
            throw new InvalidOperationException("Session context is missing in request");
        }

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        BigDecimal cashRevenue = BigDecimal.ZERO;
        BigDecimal bankRevenue = BigDecimal.ZERO;
        BigDecimal cashExpense = BigDecimal.ZERO;
        BigDecimal bankExpense = BigDecimal.ZERO;

        List<FeePayment> payments = feePaymentRepository
                .findBySchoolIdAndSessionIdAndPaymentDateBetween(schoolId, sessionId, startDate, endDate);
        for (FeePayment payment : payments) {
            BigDecimal amount = nz(payment.getPrincipalPaid()).add(nz(payment.getLateFeePaid()));
            if (isCashMode(payment.getMode())) {
                cashRevenue = cashRevenue.add(amount);
            } else {
                bankRevenue = bankRevenue.add(amount);
            }
        }

        List<ExpenseVoucher> expenses = expenseVoucherRepository
                .findBySchoolIdAndSessionIdAndExpenseDateBetweenAndActiveTrueOrderByExpenseDateDescIdDesc(
                        schoolId, sessionId, startDate, endDate);
        for (ExpenseVoucher expense : expenses) {
            BigDecimal amount = nz(expense.getAmount());
            if (isCashMode(expense.getPaymentMode() != null ? expense.getPaymentMode().name() : null)) {
                cashExpense = cashExpense.add(amount);
            } else {
                bankExpense = bankExpense.add(amount);
            }
        }

        BigDecimal totalRevenue = cashRevenue.add(bankRevenue);
        BigDecimal totalExpense = cashExpense.add(bankExpense);

        return MonthlyPLResponseDto.builder()
                .year(year)
                .month(month)
                .totalRevenue(totalRevenue)
                .totalExpense(totalExpense)
                .netProfit(totalRevenue.subtract(totalExpense))
                .cashRevenue(cashRevenue)
                .bankRevenue(bankRevenue)
                .cashExpense(cashExpense)
                .bankExpense(bankExpense)
                .netCash(cashRevenue.subtract(cashExpense))
                .netBank(bankRevenue.subtract(bankExpense))
                .build();
    }

    private boolean isCashMode(String mode) {
        String normalized = mode == null ? "" : mode.trim().toUpperCase();
        return "CASH".equals(normalized);
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
