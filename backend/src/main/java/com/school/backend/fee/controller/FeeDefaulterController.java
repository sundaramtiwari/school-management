package com.school.backend.fee.controller;

import com.school.backend.common.dto.PageResponse;
import com.school.backend.common.dto.PageResponseMapper;
import com.school.backend.fee.dto.DefaulterDto;
import com.school.backend.fee.service.FeeSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/fees/defaulters")
@RequiredArgsConstructor
public class FeeDefaulterController {

    private final FeeSummaryService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public PageResponse<DefaulterDto> getDefaulters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) BigDecimal minAmountDue,
            @RequestParam(required = false) Integer minDaysOverdue) {

        Pageable pageable = PageRequest.of(page, size);
        return PageResponseMapper
                .fromPage(service.getPaginatedDefaulters(search, classId, minAmountDue, minDaysOverdue, pageable));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public List<DefaulterDto> exportDefaulters(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) BigDecimal minAmountDue,
            @RequestParam(required = false) Integer minDaysOverdue) {
        return service.exportDefaulters(search, classId, minAmountDue, minDaysOverdue);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public com.school.backend.fee.dto.DefaulterStatsDto getDefaulterStats() {
        return service.getDefaulterStats();
    }
}
