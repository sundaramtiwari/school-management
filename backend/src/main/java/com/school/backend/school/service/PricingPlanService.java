package com.school.backend.school.service;

import com.school.backend.common.enums.SubscriptionStatus;
import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.common.exception.SubscriptionRuleViolationException;
import com.school.backend.school.dto.PricingPlanCreateRequest;
import com.school.backend.school.dto.PricingPlanDto;
import com.school.backend.school.dto.PricingPlanUpdateRequest;
import com.school.backend.school.entity.PricingPlan;
import com.school.backend.school.repository.PricingPlanRepository;
import com.school.backend.school.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PricingPlanService {

    private static final Set<SubscriptionStatus> LIVE_STATUSES = Set.of(
            SubscriptionStatus.TRIAL,
            SubscriptionStatus.ACTIVE,
            SubscriptionStatus.PAST_DUE);

    private final PricingPlanRepository pricingPlanRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public PricingPlanDto create(PricingPlanCreateRequest req) {
        validateThresholds(req.getWarningThresholdPercent(), req.getCriticalThresholdPercent());
        ensureNameUnique(req.getName(), null);

        PricingPlan entity = PricingPlan.builder()
                .name(req.getName().trim())
                .description(req.getDescription())
                .yearlyPrice(normalizeMoney(req.getYearlyPrice()))
                .studentCap(req.getStudentCap())
                .trialDaysDefault(req.getTrialDaysDefault())
                .gracePeriodDaysDefault(req.getGracePeriodDaysDefault())
                .warningThresholdPercent(req.getWarningThresholdPercent())
                .criticalThresholdPercent(req.getCriticalThresholdPercent())
                .active(req.getActive() == null || req.getActive())
                .build();

        return toDto(pricingPlanRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<PricingPlanDto> list() {
        return pricingPlanRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PricingPlanDto getById(Long id) {
        return toDto(findEntity(id));
    }

    @Transactional
    public PricingPlanDto update(Long id, PricingPlanUpdateRequest req) {
        validateThresholds(req.getWarningThresholdPercent(), req.getCriticalThresholdPercent());

        PricingPlan existing = findEntity(id);
        ensureNameUnique(req.getName(), id);

        if (!req.getActive() && existing.isActive()
                && subscriptionRepository.existsByPricingPlanIdAndStatusIn(id, LIVE_STATUSES)) {
            throw new SubscriptionRuleViolationException(
                    "Cannot deactivate pricing plan while ACTIVE/TRIAL/PAST_DUE subscriptions are linked.");
        }

        existing.setName(req.getName().trim());
        existing.setDescription(req.getDescription());
        existing.setYearlyPrice(normalizeMoney(req.getYearlyPrice()));
        existing.setStudentCap(req.getStudentCap());
        existing.setTrialDaysDefault(req.getTrialDaysDefault());
        existing.setGracePeriodDaysDefault(req.getGracePeriodDaysDefault());
        existing.setWarningThresholdPercent(req.getWarningThresholdPercent());
        existing.setCriticalThresholdPercent(req.getCriticalThresholdPercent());
        existing.setActive(req.getActive());

        return toDto(pricingPlanRepository.save(existing));
    }

    @Transactional
    public PricingPlanDto deactivate(Long id) {
        PricingPlan existing = findEntity(id);
        if (subscriptionRepository.existsByPricingPlanIdAndStatusIn(id, LIVE_STATUSES)) {
            throw new SubscriptionRuleViolationException(
                    "Cannot deactivate pricing plan while ACTIVE/TRIAL/PAST_DUE subscriptions are linked.");
        }
        existing.setActive(false);
        return toDto(pricingPlanRepository.save(existing));
    }

    @Transactional
    public void hardDelete(Long id) {
        PricingPlan existing = findEntity(id);
        long totalReferences = subscriptionRepository.countByPricingPlanId(id);
        if (totalReferences > 0) {
            throw new SubscriptionRuleViolationException("Cannot hard delete pricing plan with subscription references.");
        }
        pricingPlanRepository.delete(existing);
    }

    @Transactional(readOnly = true)
    public PricingPlan findActivePlan(Long id) {
        PricingPlan plan = findEntity(id);
        if (!plan.isActive()) {
            throw new SubscriptionRuleViolationException("Selected pricing plan is inactive.");
        }
        return plan;
    }

    private PricingPlan findEntity(Long id) {
        return pricingPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pricing plan not found: " + id));
    }

    private void ensureNameUnique(String name, Long currentId) {
        pricingPlanRepository.findByNameIgnoreCase(name.trim())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(currentId)) {
                        throw new SubscriptionRuleViolationException("Pricing plan name already exists: " + name);
                    }
                });
    }

    private void validateThresholds(Integer warning, Integer critical) {
        if (warning == null || critical == null) {
            throw new SubscriptionRuleViolationException("Warning and critical thresholds are required.");
        }
        if (warning < 0 || critical > 100 || warning >= critical) {
            throw new SubscriptionRuleViolationException(
                    "Thresholds must satisfy 0 <= warningThresholdPercent < criticalThresholdPercent <= 100.");
        }
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            throw new SubscriptionRuleViolationException("Price is required.");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new SubscriptionRuleViolationException("Price cannot be negative.");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private PricingPlanDto toDto(PricingPlan entity) {
        return PricingPlanDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .yearlyPrice(entity.getYearlyPrice())
                .studentCap(entity.getStudentCap())
                .trialDaysDefault(entity.getTrialDaysDefault())
                .gracePeriodDaysDefault(entity.getGracePeriodDaysDefault())
                .warningThresholdPercent(entity.getWarningThresholdPercent())
                .criticalThresholdPercent(entity.getCriticalThresholdPercent())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
