package com.school.backend.fee.controller;

import com.school.backend.fee.dto.FeeStructureCreateRequest;
import com.school.backend.fee.dto.FeeStructureDto;
import com.school.backend.fee.service.FeeStructureService;
import com.school.backend.common.tenant.SessionResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.school.backend.user.security.SecurityUtil;

import java.util.List;

@RestController
@RequestMapping("/api/fees/structures")
@RequiredArgsConstructor
public class FeeStructureController {

    private final FeeStructureService service;
    private final SessionResolver sessionResolver;

    // Create fee structure
    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN')")
    public FeeStructureDto create(@Valid @RequestBody FeeStructureCreateRequest req) {
        return service.create(req);
    }

    // List by class+session
    @GetMapping("/by-class/{classId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public List<FeeStructureDto> listByClass(
            @PathVariable Long classId,
            @RequestParam(required = false) Long sessionId) {

        Long effectiveSessionId = sessionId != null ? sessionId : sessionResolver.resolveForCurrentSchool();
        return service.listByClass(classId, effectiveSessionId, SecurityUtil.schoolId());
    }
}
