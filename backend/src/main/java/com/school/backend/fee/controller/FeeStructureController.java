package com.school.backend.fee.controller;

import com.school.backend.fee.dto.FeeStructureCreateRequest;
import com.school.backend.fee.dto.FeeStructureDto;
import com.school.backend.fee.dto.FeeStructurePatchRequest;
import com.school.backend.fee.service.FeeStructureService;
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

    // Create fee structure
    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN')")
    public FeeStructureDto create(@Valid @RequestBody FeeStructureCreateRequest req) {
        return service.create(req);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN')")
    public FeeStructureDto update(@PathVariable Long id, @RequestBody FeeStructurePatchRequest req) {
        return service.update(id, SecurityUtil.schoolId(), req);
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN')")
    public FeeStructureDto toggle(@PathVariable Long id) {
        return service.toggleActive(id, SecurityUtil.schoolId());
    }

    // List by class+session
    @GetMapping("/by-class/{classId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public List<FeeStructureDto> listByClass(@PathVariable Long classId) {
        return service.listByClass(classId, null, SecurityUtil.schoolId());
    }
}
