package com.school.backend.fee.controller;

import com.school.backend.fee.dto.DiscountDefinitionDto;
import com.school.backend.fee.service.DiscountDefinitionService;
import com.school.backend.user.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
}
