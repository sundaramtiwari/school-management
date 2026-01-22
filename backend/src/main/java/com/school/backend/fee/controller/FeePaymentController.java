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
}
