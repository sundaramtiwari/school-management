package com.school.backend.school.controller;

import com.school.backend.school.dto.PricingPlanCreateRequest;
import com.school.backend.school.dto.PricingPlanDto;
import com.school.backend.school.dto.PricingPlanUpdateRequest;
import com.school.backend.school.service.PricingPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pricing-plans")
@RequiredArgsConstructor
public class PricingPlanController {

    private final PricingPlanService pricingPlanService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<PricingPlanDto> create(@RequestBody @Valid PricingPlanCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pricingPlanService.create(req));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PricingPlanDto>> list() {
        return ResponseEntity.ok(pricingPlanService.list());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PricingPlanDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(pricingPlanService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<PricingPlanDto> update(@PathVariable Long id, @RequestBody @Valid PricingPlanUpdateRequest req) {
        return ResponseEntity.ok(pricingPlanService.update(id, req));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<PricingPlanDto> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(pricingPlanService.deactivate(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<Void> hardDelete(@PathVariable Long id) {
        pricingPlanService.hardDelete(id);
        return ResponseEntity.noContent().build();
    }
}
