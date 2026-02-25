package com.school.backend.expense.service;

import com.school.backend.common.exception.BusinessException;
import com.school.backend.common.exception.InvalidOperationException;
import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.common.tenant.SessionContext;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.expense.dto.*;
import com.school.backend.expense.entity.ExpenseHead;
import com.school.backend.expense.entity.ExpenseVoucher;
import com.school.backend.expense.repository.ExpenseHeadRepository;
import com.school.backend.expense.repository.ExpenseVoucherRepository;
import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.repository.AcademicSessionRepository;
import com.school.backend.user.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseHeadRepository expenseHeadRepository;
    private final ExpenseVoucherRepository expenseVoucherRepository;
    private final AcademicSessionRepository academicSessionRepository;

    @Transactional
    public ExpenseHeadDto createHead(ExpenseHeadCreateRequest req) {
        Long schoolId = TenantContext.getSchoolId();
        String normalized = normalizeName(req.getName());

        if (expenseHeadRepository.existsBySchoolIdAndNormalizedName(schoolId, normalized)) {
            throw new BusinessException("Expense head already exists: " + normalized);
        }

        ExpenseHead saved = expenseHeadRepository.save(ExpenseHead.builder()
                .name(normalized)
                .normalizedName(normalized)
                .description(trimToNull(req.getDescription()))
                .active(true)
                .schoolId(schoolId)
                .build());

        return toHeadDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ExpenseHeadDto> listHeads() {
        Long schoolId = TenantContext.getSchoolId();
        return expenseHeadRepository.findBySchoolIdOrderByNameAsc(schoolId).stream()
                .map(this::toHeadDto)
                .toList();
    }

    @Transactional
    public ExpenseHeadDto toggleHeadActive(Long id) {
        Long schoolId = TenantContext.getSchoolId();
        ExpenseHead head = expenseHeadRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense head not found: " + id));
        head.setActive(!head.isActive());
        return toHeadDto(expenseHeadRepository.save(head));
    }

    @Transactional
    public ExpenseVoucherDto createVoucher(ExpenseVoucherCreateRequest req) {
        Long schoolId = TenantContext.getSchoolId();
        Long sessionId = requireSessionId();
        AcademicSession session = requireActiveSessionForWrite(schoolId, sessionId);

        ExpenseHead head = expenseHeadRepository.findByIdAndSchoolId(req.getExpenseHeadId(), schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense head not found: " + req.getExpenseHeadId()));
        if (!head.isActive()) {
            throw new InvalidOperationException("Expense head is inactive: " + head.getName());
        }
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Expense amount must be greater than zero.");
        }

        // Serialize voucher sequencing per school+session using session row lock
        if (!session.isActive()) {
            throw new InvalidOperationException("Cannot create voucher in inactive session");
        }

        int attempts = 0;
        while (attempts < 3) {
            attempts++;
            String voucherNumber = nextVoucherNumber(schoolId, sessionId);
            try {
                ExpenseVoucher saved = expenseVoucherRepository.save(ExpenseVoucher.builder()
                        .voucherNumber(voucherNumber)
                        .expenseDate(req.getExpenseDate())
                        .expenseHead(head)
                        .amount(req.getAmount())
                        .paymentMode(req.getPaymentMode())
                        .description(trimToNull(req.getDescription()))
                        .referenceNumber(trimToNull(req.getReferenceNumber()))
                        .sessionId(sessionId)
                        .createdBy(SecurityUtil.userId())
                        .active(true)
                        .schoolId(schoolId)
                        .build());
                return toVoucherDto(saved);
            } catch (DataIntegrityViolationException ex) {
                if (attempts >= 3) {
                    throw ex;
                }
            }
        }
        throw new IllegalStateException("Failed to generate voucher number");
    }

    @Transactional(readOnly = true)
    public List<ExpenseVoucherDto> getExpensesByDate(LocalDate date) {
        Long schoolId = TenantContext.getSchoolId();
        Long sessionId = requireSessionId();

        List<ExpenseVoucher> vouchers = (date == null)
                ? expenseVoucherRepository.findBySchoolIdAndSessionIdAndActiveTrueOrderByExpenseDateDescIdDesc(schoolId, sessionId)
                : expenseVoucherRepository
                        .findBySchoolIdAndSessionIdAndExpenseDateAndActiveTrueOrderByExpenseDateDescIdDesc(schoolId, sessionId, date);

        return vouchers.stream()
                .map(this::toVoucherDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExpenseVoucherDto> getMonthlyExpenses(int year, int month) {
        Long schoolId = TenantContext.getSchoolId();
        Long sessionId = requireSessionId();
        if (month < 1 || month > 12) {
            throw new BusinessException("Month must be between 1 and 12");
        }
        return expenseVoucherRepository.findMonthly(schoolId, sessionId, year, month).stream()
                .map(this::toVoucherDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExpenseSessionSummaryDto getSessionSummary() {
        Long schoolId = TenantContext.getSchoolId();
        Long sessionId = requireSessionId();

        return ExpenseSessionSummaryDto.builder()
                .sessionId(sessionId)
                .totalVouchers(expenseVoucherRepository.countBySchoolIdAndSessionIdAndActiveTrue(schoolId, sessionId))
                .totalExpense(expenseVoucherRepository.sumSessionExpense(schoolId, sessionId))
                .headWiseTotals(expenseVoucherRepository.sumSessionExpenseGroupedByHead(schoolId, sessionId))
                .build();
    }

    @Transactional
    public ExpenseVoucherDto toggleVoucherActive(Long id) {
        Long schoolId = TenantContext.getSchoolId();
        ExpenseVoucher voucher = expenseVoucherRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense voucher not found: " + id));
        voucher.setActive(!voucher.isActive());
        return toVoucherDto(expenseVoucherRepository.save(voucher));
    }

    private AcademicSession requireActiveSessionForWrite(Long schoolId, Long sessionId) {
        return academicSessionRepository.findByIdAndSchoolId(sessionId, schoolId)
                .filter(AcademicSession::isActive)
                .orElseThrow(() -> new InvalidOperationException("Cannot create voucher in inactive session"));
    }

    private String nextVoucherNumber(Long schoolId, Long sessionId) {
        int nextSeq = expenseVoucherRepository.findFirstBySchoolIdAndSessionIdOrderByIdDesc(schoolId, sessionId)
                .map(v -> extractSequence(v.getVoucherNumber()) + 1)
                .orElse(1);
        return "EXP-" + sessionId + "-" + String.format("%05d", nextSeq);
    }

    private int extractSequence(String voucherNumber) {
        if (voucherNumber == null || voucherNumber.isBlank()) {
            return 0;
        }
        String[] parts = voucherNumber.split("-");
        if (parts.length == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(parts[parts.length - 1]);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String normalizeName(String raw) {
        String trimmed = trimToNull(raw);
        if (trimmed == null) {
            throw new BusinessException("Expense head name is required");
        }
        String[] words = trimmed.toLowerCase(Locale.ROOT).split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                out.append(word.substring(1));
            }
        }
        return out.toString();
    }

    private ExpenseHeadDto toHeadDto(ExpenseHead head) {
        return ExpenseHeadDto.builder()
                .id(head.getId())
                .name(head.getName())
                .description(head.getDescription())
                .active(head.isActive())
                .build();
    }

    private ExpenseVoucherDto toVoucherDto(ExpenseVoucher voucher) {
        return ExpenseVoucherDto.builder()
                .id(voucher.getId())
                .voucherNumber(voucher.getVoucherNumber())
                .expenseDate(voucher.getExpenseDate())
                .expenseHeadId(voucher.getExpenseHead().getId())
                .expenseHeadName(voucher.getExpenseHead().getName())
                .amount(voucher.getAmount())
                .paymentMode(voucher.getPaymentMode())
                .description(voucher.getDescription())
                .referenceNumber(voucher.getReferenceNumber())
                .sessionId(voucher.getSessionId())
                .createdBy(voucher.getCreatedBy())
                .active(voucher.isActive())
                .build();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Long requireSessionId() {
        Long sessionId = SessionContext.getSessionId();
        if (sessionId == null) {
            throw new InvalidOperationException("Session context is missing in request");
        }
        return sessionId;
    }
}
