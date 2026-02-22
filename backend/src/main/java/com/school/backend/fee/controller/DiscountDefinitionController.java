package com.school.backend.fee.controller;

import com.school.backend.fee.dto.DiscountDefinitionCreateRequest;
import com.school.backend.fee.dto.DiscountDefinitionDto;
import com.school.backend.fee.service.DiscountDefinitionService;
import com.school.backend.user.security.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/fees/discount-definitions")
@RequiredArgsConstructor
public class DiscountDefinitionController {

    private final DiscountDefinitionService discountDefinitionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','ACCOUNTANT','SUPER_ADMIN','PLATFORM_ADMIN')")
    public List<DiscountDefinitionDto> listActiveDefinitions() {
        return discountDefinitionService.findActiveBySchool(SecurityUtil.schoolId());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','SUPER_ADMIN','PLATFORM_ADMIN')")
    public DiscountDefinitionDto create(@Valid @RequestBody DiscountDefinitionCreateRequest request) {
        return discountDefinitionService.create(SecurityUtil.schoolId(), request);
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','SUPER_ADMIN','PLATFORM_ADMIN')")
    public DiscountDefinitionDto toggle(@PathVariable Long id) {
        return discountDefinitionService.toggleActive(id, SecurityUtil.schoolId());
    }
}
