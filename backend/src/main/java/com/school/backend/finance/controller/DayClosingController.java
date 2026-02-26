package com.school.backend.finance.controller;

import com.school.backend.finance.dto.DayClosingDto;
import com.school.backend.finance.service.DayClosingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/finance/day-closing")
@RequiredArgsConstructor
public class DayClosingController {

    private final DayClosingService dayClosingService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN')")
    public DayClosingDto closeDate(@RequestParam LocalDate date) {
        return dayClosingService.closeDate(date);
    }

    @PatchMapping("/{date}/override")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public DayClosingDto overrideClosedDate(@PathVariable LocalDate date) {
        return dayClosingService.allowOverride(date);
    }
}
