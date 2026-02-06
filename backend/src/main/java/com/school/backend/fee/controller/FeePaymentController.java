package com.school.backend.fee.controller;

import com.school.backend.fee.dto.FeePaymentDto;
import com.school.backend.fee.dto.FeePaymentRequest;
import com.school.backend.fee.service.FeePaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fees/payments")
@RequiredArgsConstructor
public class FeePaymentController {

    private final FeePaymentService service;
    private final com.school.backend.fee.service.FeeReceiptService receiptService;

    // Recent payments (global for school)
    @GetMapping("/recent")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN')")
    public List<FeePaymentDto> recent() {
        return service.getRecentPayments();
    }

    // Make payment
    @PostMapping
    public FeePaymentDto pay(@Valid @RequestBody FeePaymentRequest req) {
        return service.pay(req);
    }

    // Payment history
    @GetMapping("/students/{studentId}")
    public List<FeePaymentDto> history(@PathVariable Long studentId) {
        return service.getHistory(studentId);
    }

    // Download Receipt
    @GetMapping("/{id}/receipt")
    public org.springframework.http.ResponseEntity<byte[]> downloadReceipt(@PathVariable Long id) {
        byte[] pdf = receiptService.generateReceipt(id);

        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=receipt_" + id + ".pdf")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
