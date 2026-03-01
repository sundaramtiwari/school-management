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
}
