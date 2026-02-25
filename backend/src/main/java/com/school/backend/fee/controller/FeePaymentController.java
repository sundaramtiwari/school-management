package com.school.backend.fee.controller;

import com.school.backend.fee.dto.FeePaymentDto;
import com.school.backend.fee.dto.FeePaymentRequest;
import com.school.backend.fee.dto.FeeTypeHeadSummaryDto;
import com.school.backend.fee.service.FeePaymentService;
import com.school.backend.fee.service.FeeReceiptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/fees/payments")
@RequiredArgsConstructor
public class FeePaymentController {

    private final FeePaymentService service;
    private final FeeReceiptService receiptService;

    // Recent payments (global for school)
    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public List<FeePaymentDto> recent(@RequestParam(defaultValue = "10") int limit) {
        return service.getRecentPayments(limit);
    }

    // Make payment
    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN')")
    public FeePaymentDto pay(@Valid @RequestBody FeePaymentRequest req) {
        return service.pay(req);
    }

    // Payment history
    @GetMapping("/students/{studentId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public List<FeePaymentDto> history(@PathVariable Long studentId) {
        return service.getStudentPayments(studentId);
    }

    @GetMapping("/head-summary")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN')")
    public List<FeeTypeHeadSummaryDto> getHeadSummary(@RequestParam LocalDate date) {
        return service.getHeadSummaryByDate(date);
    }

    // Download Receipt
    @GetMapping("/{id}/receipt")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable Long id) {
        byte[] pdf = receiptService.generateReceipt(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=receipt_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
