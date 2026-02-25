package com.school.backend.expense.controller;

import com.school.backend.expense.dto.*;
import com.school.backend.expense.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping("/heads")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN')")
    public ExpenseHeadDto createHead(@Valid @RequestBody ExpenseHeadCreateRequest req) {
        return expenseService.createHead(req);
    }

    @GetMapping("/heads")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN')")
    public List<ExpenseHeadDto> listHeads() {
        return expenseService.listHeads();
    }

    @PatchMapping("/heads/{id}/toggle-active")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN')")
    public ExpenseHeadDto toggleHeadActive(@PathVariable Long id) {
        return expenseService.toggleHeadActive(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN')")
    public ExpenseVoucherDto createExpense(@Valid @RequestBody ExpenseVoucherCreateRequest req) {
        return expenseService.createVoucher(req);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN')")
    public List<ExpenseVoucherDto> getExpenses(@RequestParam(required = false) LocalDate date) {
        return expenseService.getExpensesByDate(date);
    }

    @GetMapping("/monthly")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN')")
    public List<ExpenseVoucherDto> getMonthly(
            @RequestParam int year,
            @RequestParam int month) {
        return expenseService.getMonthlyExpenses(year, month);
    }

    @GetMapping("/session-summary")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN')")
    public ExpenseSessionSummaryDto getSessionSummary() {
        return expenseService.getSessionSummary();
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN')")
    public ExpenseVoucherDto toggleVoucherActive(@PathVariable Long id) {
        return expenseService.toggleVoucherActive(id);
    }
}
