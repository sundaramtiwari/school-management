package com.school.backend.fee.controller;

import com.school.backend.common.dto.PageResponse;
import com.school.backend.common.dto.PageResponseMapper;
import com.school.backend.common.tenant.SessionResolver;
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
    private final SessionResolver sessionResolver;

    @GetMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public PageResponse<DefaulterDto> getDefaulters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) BigDecimal minAmountDue,
            @RequestParam(required = false) Integer minDaysOverdue,
            @RequestParam(required = false) Long sessionId) {

        Pageable pageable = PageRequest.of(page, size);
        Long effectiveSessionId = sessionId != null ? sessionId : sessionResolver.resolveForCurrentSchool();
        return PageResponseMapper
                .fromPage(service.getPaginatedDefaulters(effectiveSessionId, search, classId, minAmountDue, minDaysOverdue, pageable));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public List<DefaulterDto> exportDefaulters(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) BigDecimal minAmountDue,
            @RequestParam(required = false) Integer minDaysOverdue,
            @RequestParam(required = false) Long sessionId) {
        Long effectiveSessionId = sessionId != null ? sessionId : sessionResolver.resolveForCurrentSchool();
        return service.exportDefaulters(effectiveSessionId, search, classId, minAmountDue, minDaysOverdue);
    }
}
