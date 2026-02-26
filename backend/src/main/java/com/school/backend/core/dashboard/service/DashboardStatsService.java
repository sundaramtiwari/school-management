package com.school.backend.core.dashboard.service;

import com.school.backend.common.enums.ExpensePaymentMode;
import com.school.backend.common.enums.UserRole;
import com.school.backend.common.exception.InvalidOperationException;
import com.school.backend.common.tenant.SessionContext;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.core.dashboard.dto.DailyCashDashboardDto;
import com.school.backend.core.dashboard.dto.SchoolAdminStatsDto;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.expense.entity.ExpenseVoucher;
import com.school.backend.expense.repository.ExpenseVoucherRepository;
import com.school.backend.finance.repository.FinanceAccountTransferRepository;
import com.school.backend.fee.entity.FeePayment;
import com.school.backend.fee.repository.FeePaymentAllocationRepository;
import com.school.backend.fee.repository.FeePaymentRepository;
import com.school.backend.fee.service.FeeSummaryService;
import com.school.backend.testmanagement.repository.ExamRepository;
import com.school.backend.transport.repository.TransportEnrollmentRepository;
import com.school.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardStatsService {

    private static final String CASH = "CASH";
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final TransportEnrollmentRepository transportRepository;
    private final ExamRepository examRepository;
    private final FeeSummaryService feeSummaryService;
    private final com.school.backend.core.attendance.service.AttendanceService attendanceService;
    private final FeePaymentRepository feePaymentRepository;
    private final FeePaymentAllocationRepository feePaymentAllocationRepository;
    private final ExpenseVoucherRepository expenseVoucherRepository;
    private final FinanceAccountTransferRepository financeAccountTransferRepository;

    public SchoolAdminStatsDto getSchoolAdminStats() {
        Long schoolId = TenantContext.getSchoolId();
        Long effectiveSessionId = SessionContext.getSessionId();
        if (effectiveSessionId == null) {
            throw new InvalidOperationException("Session context is missing in request");
        }

        // 1. Basic Counts
        long totalStudents = studentRepository.countBySchoolIdAndSessionId(schoolId, effectiveSessionId);
        long transportCount = transportRepository.countBySchoolIdAndSessionId(schoolId, effectiveSessionId);
        long totalTeachers = userRepository.countBySchoolIdAndRole(schoolId, UserRole.TEACHER);

        // 2. Fee Stats (Defaulters Count)
        long feePendingCount = feeSummaryService.countDefaulters();

        // 3. Attendance Stats
        double attendancePercentage = attendanceService.getTodayStats(schoolId, effectiveSessionId);

        // 4. Upcoming Exams
        LocalDate now = LocalDate.now();
        var upcomingExams = examRepository
                .findBySchoolIdAndSessionIdAndStartDateAfter(schoolId, effectiveSessionId, now)
                .stream()
                .limit(5)
                .map(e -> SchoolAdminStatsDto.UpcomingExamDto.builder()
                        .name(e.getName())
                        .date(e.getStartDate() != null ? e.getStartDate().toString() : "TBD")
                        .className("Class " + e.getClassId()) // Need class name here ideally
                        .build())
                .collect(Collectors.toList());

        return SchoolAdminStatsDto.builder()
                .totalStudents(totalStudents)
                .transportCount(transportCount)
                .totalTeachers(totalTeachers)
                .feePendingCount(feePendingCount)
                .upcomingExams(upcomingExams)
                .attendancePercentage(attendancePercentage)
                .build();
    }

    public DailyCashDashboardDto getDailyCashDashboard(LocalDate date) {
        Long schoolId = TenantContext.getSchoolId();
        Long sessionId = SessionContext.getSessionId();
        if (sessionId == null) {
            throw new InvalidOperationException("Session context is missing in request");
        }

        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        BigDecimal cashRevenue = BigDecimal.ZERO;
        BigDecimal bankRevenue = BigDecimal.ZERO;
        BigDecimal cashExpense = BigDecimal.ZERO;
        BigDecimal bankExpense = BigDecimal.ZERO;

        List<FeePayment> dailyPayments = feePaymentRepository
                .findBySchoolIdAndSessionIdAndPaymentDate(schoolId, sessionId, effectiveDate);
        for (FeePayment payment : dailyPayments) {
            BigDecimal amount = nz(payment.getPrincipalPaid()).add(nz(payment.getLateFeePaid()));
            if (isCashMode(payment.getMode())) {
                cashRevenue = cashRevenue.add(amount);
            } else {
                bankRevenue = bankRevenue.add(amount);
            }
        }

        java.util.List<ExpenseVoucher> dailyExpenses = expenseVoucherRepository
                .findBySchoolIdAndSessionIdAndExpenseDateAndActiveTrueOrderByExpenseDateDescIdDesc(
                        schoolId, sessionId, effectiveDate);
        for (ExpenseVoucher voucher : dailyExpenses) {
            BigDecimal amount = nz(voucher.getAmount());
            if (isCashMode(voucher.getPaymentMode() != null ? voucher.getPaymentMode().name() : null)) {
                cashExpense = cashExpense.add(amount);
            } else {
                bankExpense = bankExpense.add(amount);
            }
        }

        BigDecimal transferOut = financeAccountTransferRepository
                .findBySchoolIdAndSessionIdAndTransferDateBetween(schoolId, sessionId, effectiveDate, effectiveDate)
                .stream()
                .map(transfer -> nz(transfer.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Preserve existing totals behavior (cash-only operational aggregation)
        BigDecimal totalFeeCollected = cashRevenue;
        BigDecimal totalExpense = cashExpense;
        BigDecimal netCash = cashRevenue.subtract(cashExpense).subtract(transferOut);
        BigDecimal netBank = bankRevenue.subtract(bankExpense).add(transferOut);
        BigDecimal netAmount = totalFeeCollected.subtract(totalExpense);

        return DailyCashDashboardDto.builder()
                .totalFeeCollected(totalFeeCollected)
                .totalExpense(totalExpense)
                .netCash(netCash)
                .cashRevenue(cashRevenue)
                .bankRevenue(bankRevenue)
                .cashExpense(cashExpense)
                .bankExpense(bankExpense)
                .netBank(netBank)
                .netAmount(netAmount)
                .headWiseCollection(feePaymentAllocationRepository
                        .findHeadSummaryBySchoolSessionDateAndMode(
                                schoolId, sessionId, effectiveDate, CASH))
                .expenseBreakdown(expenseVoucherRepository
                        .sumExpenseByHeadForDateAndMode(
                                schoolId, sessionId, effectiveDate,
                                ExpensePaymentMode.CASH))
                .build();
    }

    private boolean isCashMode(String mode) {
        String normalized = mode == null ? "" : mode.trim().toUpperCase();
        return CASH.equals(normalized);
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
