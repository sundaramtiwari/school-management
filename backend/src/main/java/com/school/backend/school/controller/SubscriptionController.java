package com.school.backend.school.controller;

import com.school.backend.school.dto.*;
import com.school.backend.school.service.SubscriptionAccessService;
import com.school.backend.school.service.SubscriptionService;
import com.school.backend.user.security.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map; // This import is already present, but the instruction implies adding it. I will ensure it's there.

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SubscriptionAccessService subscriptionAccessService;

    @PostMapping("/trial")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<SubscriptionDto> createWithTrial(@RequestBody @Valid CreateSubscriptionTrialRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.createSubscriptionWithTrial(req, SecurityUtil.userId()));
    }

    @PostMapping("/{subscriptionId}/activate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<SubscriptionDto> activate(@PathVariable Long subscriptionId,
            @RequestBody @Valid ActivateSubscriptionRequest req) {
        return ResponseEntity.ok(subscriptionService.activateSubscription(subscriptionId, req, SecurityUtil.userId()));
    }

    @PostMapping("/{subscriptionId}/payments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<SubscriptionPaymentDto> recordPayment(@PathVariable Long subscriptionId,
            @RequestBody @Valid RecordPaymentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.recordPayment(subscriptionId, req, SecurityUtil.userId()));
    }

    @PostMapping("/{subscriptionId}/upgrade")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<UpgradePlanResponse> upgrade(@PathVariable Long subscriptionId,
            @RequestBody @Valid UpgradePlanRequest req) {
        return ResponseEntity.ok(subscriptionService.upgradePlan(subscriptionId, req, SecurityUtil.userId()));
    }

    @PostMapping("/{subscriptionId}/downgrade")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<SubscriptionDto> downgrade(@PathVariable Long subscriptionId,
            @RequestBody @Valid DowngradePlanRequest req) {
        return ResponseEntity.ok(subscriptionService.downgradePlan(subscriptionId, req, SecurityUtil.userId()));
    }

    @PostMapping("/{subscriptionId}/extend-trial")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<SubscriptionDto> extendTrial(@PathVariable Long subscriptionId,
            @RequestBody @Valid TrialExtensionRequest req) {
        return ResponseEntity.ok(subscriptionService.extendTrial(
                subscriptionId, req.getAdditionalDays(), req.getReason(), SecurityUtil.userId()));
    }

    @PostMapping("/{subscriptionId}/extend")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<SubscriptionDto> extendSubscription(@PathVariable Long subscriptionId,
            @RequestBody @Valid SubscriptionExtensionRequest req) {
        return ResponseEntity.ok(subscriptionService.extendSubscription(
                subscriptionId, req.getAdditionalDays(), req.getReason(), SecurityUtil.userId()));
    }

    @GetMapping("/school/{schoolId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<SubscriptionDto> getBySchool(@PathVariable Long schoolId) {
        return ResponseEntity.ok(subscriptionService.getBySchool(schoolId));
    }

    @GetMapping("/access-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SubscriptionAccessStatusDto> getAccessStatus() {
        return ResponseEntity.ok(subscriptionAccessService.validateSchoolAccess(SecurityUtil.schoolId()));
    }

    @PostMapping("/{subscriptionId}/suspend")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SubscriptionDto> suspend(@PathVariable Long subscriptionId,
            @RequestBody @Valid ManualSuspendRequest req) {
        return ResponseEntity.ok(subscriptionService.manualSuspend(subscriptionId, req, SecurityUtil.userId()));
    }

    @PostMapping("/{subscriptionId}/reactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SubscriptionDto> reactivate(@PathVariable Long subscriptionId,
            @RequestBody ManualReactivateRequest req) {
        return ResponseEntity.ok(subscriptionService.manualReactivate(subscriptionId, req, SecurityUtil.userId()));
    }

    @GetMapping("/admin-usage/bulk")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<Map<Long, AdminSubscriptionUsageDto>> getAdminUsageBulk(@RequestParam List<Long> schoolIds) {
        return ResponseEntity.ok(subscriptionService.getAdminUsageBulk(schoolIds));
    }

    @GetMapping("/{schoolId}/admin-usage")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<AdminSubscriptionUsageDto> getAdminUsage(@PathVariable Long schoolId) {
        return ResponseEntity.ok(subscriptionService.getAdminUsageBySchool(schoolId));
    }

    @GetMapping("/{subscriptionId}/payments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<List<SubscriptionPaymentDto>> getPaymentHistory(@PathVariable Long subscriptionId) {
        return ResponseEntity.ok(
                subscriptionService.getPaymentHistory(subscriptionId, SecurityUtil.role(), SecurityUtil.schoolId()));
    }

    @GetMapping("/{subscriptionId}/events")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<List<SubscriptionEventDto>> getEventHistory(@PathVariable Long subscriptionId) {
        return ResponseEntity.ok(
                subscriptionService.getEventHistory(subscriptionId, SecurityUtil.role(), SecurityUtil.schoolId()));
    }
}
