package com.school.backend.finance.service;

import com.school.backend.common.enums.ExpensePaymentMode;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.finance.dto.DailyCashDashboardDto;
import com.school.backend.expense.repository.ExpenseVoucherRepository;
import com.school.backend.fee.repository.FeePaymentAllocationRepository;
import com.school.backend.fee.repository.FeePaymentRepository;
import com.school.backend.finance.dto.FinancialOverviewDto;
import com.school.backend.finance.entity.DayClosing;
import com.school.backend.finance.repository.DayClosingRepository;
import com.school.backend.finance.repository.FinanceAccountTransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FinanceOverviewService {

        private static final String CASH = "CASH";
        private static final Set<String> BANK_MODES = Set.of("BANK", "UPI", "ONLINE", "BANK_TRANSFER", "CHEQUE");

        private final FeePaymentRepository feePaymentRepository;
        private final FeePaymentAllocationRepository feePaymentAllocationRepository;
        private final ExpenseVoucherRepository expenseVoucherRepository;
        private final FinanceAccountTransferRepository financeAccountTransferRepository;
        private final DayClosingRepository dayClosingRepository;

        @Transactional(readOnly = true)
        public DailyCashDashboardDto getDailyOverview(LocalDate date) {
                Long schoolId = TenantContext.getSchoolId();
                LocalDate effectiveDate = date != null ? date : LocalDate.now();

                // 1. Opening Balance (Treasury Lens)
                DayClosing yesterday = dayClosingRepository
                                .findFirstBySchoolIdAndDateLessThanOrderByDateDesc(schoolId, effectiveDate)
                                .orElse(null);

                BigDecimal openingCash = yesterday != null ? nz(yesterday.getClosingCash()) : BigDecimal.ZERO;
                BigDecimal openingBank = yesterday != null ? nz(yesterday.getClosingBank()) : BigDecimal.ZERO;

                // 2. Daily Movements
                BigDecimal cashRevenue = feePaymentRepository.sumTotalPaidBySchoolDateAndMode(schoolId, effectiveDate,
                                CASH);
                BigDecimal bankRevenue = sumBankRevenue(schoolId, effectiveDate);

                BigDecimal cashExpense = expenseVoucherRepository.sumExpenseBySchoolDateAndMode(schoolId, effectiveDate,
                                ExpensePaymentMode.CASH);
                BigDecimal bankExpense = expenseVoucherRepository.sumExpenseBySchoolDateAndMode(schoolId, effectiveDate,
                                ExpensePaymentMode.BANK);

                BigDecimal transferOut = financeAccountTransferRepository
                                .findBySchoolIdAndTransferDateBetween(schoolId, effectiveDate, effectiveDate)
                                .stream()
                                .filter(t -> CASH.equalsIgnoreCase(t.getFromAccount())
                                                && "BANK".equalsIgnoreCase(t.getToAccount()))
                                .map(t -> nz(t.getAmount()))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // 3. Closing Positions
                BigDecimal closingCash = openingCash.add(cashRevenue).subtract(cashExpense).subtract(transferOut);
                BigDecimal closingBank = openingBank.add(bankRevenue).subtract(bankExpense).add(transferOut);

                // Legacy Net Amount matches current operational DASHBOARD (Cash Revenue - Cash
                // Expense)
                BigDecimal netAmount = cashRevenue.subtract(cashExpense);

                return DailyCashDashboardDto.builder()
                                .totalFeeCollected(cashRevenue)
                                .totalExpense(cashExpense)
                                .cashRevenue(cashRevenue)
                                .bankRevenue(bankRevenue)
                                .cashExpense(cashExpense)
                                .bankExpense(bankExpense)
                                .netCash(closingCash)
                                .netBank(closingBank)
                                .transferOut(transferOut)
                                .transferIn(transferOut)
                                .netAmount(netAmount)
                                .closed(dayClosingRepository.existsBySchoolIdAndDate(schoolId, effectiveDate))
                                .headWiseCollection(feePaymentAllocationRepository.findHeadSummaryBySchoolIdDateAndMode(
                                                schoolId,
                                                effectiveDate, CASH))
                                .expenseBreakdown(expenseVoucherRepository.sumExpenseByHeadForSchoolIdAndDateAndMode(
                                                schoolId,
                                                effectiveDate, ExpensePaymentMode.CASH))
                                .build();
        }

        @Transactional(readOnly = true)
        public FinancialOverviewDto getRangeOverview(LocalDate start, LocalDate end) {
                String periodName = "P&L Report (" + start + " to " + end + ")";
                return getFinancialOverview(start, end, periodName);
        }

        private FinancialOverviewDto getFinancialOverview(LocalDate start, LocalDate end, String periodName) {
                Long schoolId = TenantContext.getSchoolId();

                BigDecimal revenue = nz(feePaymentRepository.sumTotalPaidBySchoolIdAndDateRange(schoolId, start, end));
                BigDecimal expense = nz(
                                expenseVoucherRepository.sumTotalExpenseBySchoolIdAndDateRange(schoolId, start, end));

                BigDecimal cashRevenue = nz(
                                feePaymentRepository.sumTotalPaidBySchoolDateRangeAndMode(schoolId, start, end, CASH));
                BigDecimal bankRevenue = revenue.subtract(cashRevenue);

                BigDecimal cashExpense = nz(expenseVoucherRepository
                                .findBySchoolIdAndExpenseDateBetweenAndActiveTrueOrderByExpenseDateDescIdDesc(schoolId,
                                                start, end)
                                .stream()
                                .filter(v -> ExpensePaymentMode.CASH == v.getPaymentMode())
                                .map(v -> nz(v.getAmount()))
                                .reduce(BigDecimal.ZERO, BigDecimal::add));
                BigDecimal bankExpense = expense.subtract(cashExpense);

                return FinancialOverviewDto.builder()
                                .periodName(periodName)
                                .startDate(start)
                                .endDate(end)
                                .totalRevenue(revenue)
                                .totalExpense(expense)
                                .netProfit(revenue.subtract(expense))
                                .cashRevenue(cashRevenue)
                                .bankRevenue(bankRevenue)
                                .cashExpense(cashExpense)
                                .bankExpense(bankExpense)
                                .build();
        }

        private BigDecimal sumBankRevenue(Long schoolId, LocalDate date) {
                BigDecimal total = BigDecimal.ZERO;
                for (String mode : BANK_MODES) {
                        total = total.add(
                                        nz(feePaymentRepository.sumTotalPaidBySchoolDateAndMode(schoolId, date, mode)));
                }
                return total;
        }

        private BigDecimal nz(BigDecimal val) {
                return val != null ? val : BigDecimal.ZERO;
        }
}
