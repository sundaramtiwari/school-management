package com.school.backend.fee.controller;

import com.school.backend.fee.dto.FeeTypePatchRequest;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.fee.service.FeeTypeService;
import com.school.backend.user.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fees/types")
@RequiredArgsConstructor
public class FeeTypeController {

    private final FeeTypeService service;

    // Create fee type
    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN')")
    public FeeType create(@RequestBody FeeType type) {
        return service.create(type);
    }

    // List fee types
    @GetMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public List<FeeType> list() {
        return service.list();
    }

    // Get by id (optional but useful)
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public FeeType get(@PathVariable Long id) {
        return service.get(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN')")
    public FeeType update(@PathVariable Long id, @RequestBody FeeTypePatchRequest req) {
        return service.update(id, SecurityUtil.schoolId(), req);
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN')")
    public FeeType toggle(@PathVariable Long id) {
        return service.toggleActive(id, SecurityUtil.schoolId());
    }
}
