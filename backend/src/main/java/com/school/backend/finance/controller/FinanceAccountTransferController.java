package com.school.backend.finance.controller;

import com.school.backend.finance.dto.FinanceAccountTransferDto;
import com.school.backend.finance.dto.FinanceAccountTransferRequest;
import com.school.backend.finance.service.FinanceAccountTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/finance/transfers")
@RequiredArgsConstructor
public class FinanceAccountTransferController {

    private final FinanceAccountTransferService transferService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN')")
    public FinanceAccountTransferDto createTransfer(@Valid @RequestBody FinanceAccountTransferRequest req) {
        return transferService.createTransfer(
                req.getTransferDate(),
                req.getAmount(),
                req.getReferenceNumber(),
                req.getRemarks());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public List<FinanceAccountTransferDto> listTransfers(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {
        return transferService.listTransfers(fromDate, toDate);
    }
}
