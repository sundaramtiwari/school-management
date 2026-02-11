package com.school.backend.fee.controller;

import com.school.backend.fee.dto.DefaulterDto;
import com.school.backend.fee.service.FeeSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fees/defaulters")
@RequiredArgsConstructor
public class FeeDefaulterController {

    private final FeeSummaryService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public List<DefaulterDto> getDefaulters() {
        return service.getAllDefaulters();
    }
}
