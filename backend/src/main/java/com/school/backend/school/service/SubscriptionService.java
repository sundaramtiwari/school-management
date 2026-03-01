package com.school.backend.school.service;

import com.school.backend.common.enums.SubscriptionEventType;
import com.school.backend.common.enums.SubscriptionPaymentType;
import com.school.backend.common.enums.SubscriptionStatus;
import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.common.exception.SubscriptionConcurrencyException;
import com.school.backend.common.exception.SubscriptionRuleViolationException;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.school.dto.*;
import com.school.backend.school.entity.PricingPlan;
import com.school.backend.school.entity.School;
import com.school.backend.school.entity.Subscription;
import com.school.backend.school.entity.SubscriptionEvent;
import com.school.backend.school.entity.SubscriptionPayment;
import com.school.backend.school.repository.SchoolRepository;
import com.school.backend.school.repository.SubscriptionEventRepository;
import com.school.backend.school.repository.SubscriptionPaymentRepository;
import com.school.backend.school.repository.SubscriptionRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private static final Set<SubscriptionStatus> LIVE_STATUSES = Set.of(
            SubscriptionStatus.TRIAL,
            SubscriptionStatus.ACTIVE,
            SubscriptionStatus.PAST_DUE);

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final SubscriptionEventRepository eventRepository;
    private final PricingPlanService pricingPlanService;
    private final SchoolRepository schoolRepository;
    private final StudentRepository studentRepository;
    private final Clock clock;

    @Transactional
    public SubscriptionDto createSubscriptionWithTrial(CreateSubscriptionTrialRequest req, Long performedByUserId) {
        PricingPlan plan = pricingPlanService.findActivePlan(req.getPricingPlanId());
        School school = schoolRepository.findById(req.getSchoolId())
                .orElseThrow(() -> new ResourceNotFoundException("School not found: " + req.getSchoolId()));

        enforceSingleLiveSubscription(school.getId(), null);

        int trialDays = req.getTrialDays() != null ? req.getTrialDays() : plan.getTrialDaysDefault();
        LocalDate today = LocalDate.now(clock);

        Subscription entity = Subscription.builder()
                .schoolId(school.getId())
                .pricingPlan(plan)
                .status(SubscriptionStatus.TRIAL)
                .startDate(today)
                .trialEndDate(today.plusDays(trialDays))
                .expiryDate(null)
                .gracePeriodDays(plan.getGracePeriodDaysDefault())
                .build();

        Subscription saved = subscriptionRepository.save(entity);
        log.info("Trial subscription created schoolId={} subscriptionId={} planId={}",
                school.getId(), saved.getId(), plan.getId());
        return toDto(saved);
    }

    @Transactional
    public SubscriptionDto activateSubscription(Long subscriptionId, ActivateSubscriptionRequest req, Long performedByUserId) {
        Subscription subscription = getById(subscriptionId);
        if (subscription.getStatus() != SubscriptionStatus.TRIAL && subscription.getStatus() != SubscriptionStatus.PAST_DUE) {
            throw new SubscriptionRuleViolationException("Only TRIAL or PAST_DUE subscriptions can be activated.");
        }

        enforceSingleLiveSubscription(subscription.getSchoolId(), subscriptionId);
        createPayment(subscription, normalizeMoney(subscription.getPricingPlan().getYearlyPrice()),
                SubscriptionPaymentType.PAYMENT, req.getPaymentDate(), req.getReferenceNumber(), req.getNotes(),
                performedByUserId);

        subscription.setStartDate(req.getPaymentDate());
        subscription.setExpiryDate(req.getPaymentDate().plusDays(365));
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(subscription);
        return toDto(subscription);
    }

    @Transactional
    public SubscriptionPaymentDto recordPayment(Long subscriptionId, RecordPaymentRequest req, Long performedByUserId) {
        Subscription subscription = getById(subscriptionId);
        try {
            SubscriptionPayment payment = createPayment(subscription, req.getAmount(), req.getType(),
                    req.getPaymentDate(), req.getReferenceNumber(), req.getNotes(), performedByUserId);
            if (req.getType() == SubscriptionPaymentType.PAYMENT) {
                subscription.setStartDate(req.getPaymentDate());
                subscription.setExpiryDate(req.getPaymentDate().plusDays(365));
                subscription.setStatus(SubscriptionStatus.ACTIVE);
                subscriptionRepository.save(subscription);
            }
            return toDto(payment);
        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
            throw new SubscriptionConcurrencyException("Concurrent subscription update detected while recording payment.");
        }
    }

    @Transactional
    public UpgradePlanResponse upgradePlan(Long subscriptionId, UpgradePlanRequest req, Long performedByUserId) {
        try {
            Subscription subscription = getById(subscriptionId);
            if (subscription.getStatus() == SubscriptionStatus.SUSPENDED) {
                throw new SubscriptionRuleViolationException("Suspended subscriptions cannot be upgraded.");
            }
            if (subscription.getExpiryDate() == null) {
                throw new SubscriptionRuleViolationException("Upgrade is supported only for paid subscriptions.");
            }
            PricingPlan currentPlan = subscription.getPricingPlan();
            PricingPlan newPlan = pricingPlanService.findActivePlan(req.getNewPlanId());
            if (newPlan.getYearlyPrice().compareTo(currentPlan.getYearlyPrice()) <= 0) {
                throw new SubscriptionRuleViolationException("Upgrade rejected: new plan price must be greater than current plan price.");
            }

            LocalDate today = LocalDate.now(clock);
            long remainingDays = Math.max(0, ChronoUnit.DAYS.between(today, subscription.getExpiryDate()));
            BigDecimal priceDiff = newPlan.getYearlyPrice().subtract(currentPlan.getYearlyPrice());
            BigDecimal proratedAmount = priceDiff
                    .multiply(BigDecimal.valueOf(remainingDays))
                    .divide(BigDecimal.valueOf(365), 2, RoundingMode.HALF_UP);

            subscription.setPricingPlan(newPlan);
            subscriptionRepository.save(subscription);

            SubscriptionPayment prorationPayment = createPayment(
                    subscription,
                    proratedAmount,
                    SubscriptionPaymentType.UPGRADE_PRORATION,
                    today,
                    "UPG-" + subscription.getId() + "-" + System.nanoTime(),
                    req.getNotes(),
                    performedByUserId);

            eventRepository.save(SubscriptionEvent.builder()
                    .subscriptionId(subscription.getId())
                    .type(SubscriptionEventType.PLAN_UPGRADED)
                    .daysAdded(0)
                    .previousExpiryDate(subscription.getExpiryDate())
                    .newExpiryDate(subscription.getExpiryDate())
                    .reason(req.getNotes())
                    .performedByUserId(performedByUserId)
                    .build());

            return UpgradePlanResponse.builder()
                    .subscription(toDto(subscription))
                    .prorationPayment(toDto(prorationPayment))
                    .proratedAmount(proratedAmount)
                    .build();
        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
            throw new SubscriptionConcurrencyException("Concurrent subscription update detected while upgrading plan.");
        }
    }

    @Transactional
    public SubscriptionDto downgradePlan(Long subscriptionId, DowngradePlanRequest req, Long performedByUserId) {
        try {
            Subscription subscription = getById(subscriptionId);
            if (subscription.getStatus() == SubscriptionStatus.SUSPENDED) {
                throw new SubscriptionRuleViolationException("Suspended subscriptions cannot be downgraded.");
            }
            PricingPlan newPlan = pricingPlanService.findActivePlan(req.getNewPlanId());

            Long currentSessionId = schoolRepository.findById(subscription.getSchoolId())
                    .map(School::getCurrentSessionId)
                    .orElse(null);
            long activeStudents = currentSessionId == null ? 0
                    : studentRepository.countActiveStudentsInSession(subscription.getSchoolId(), currentSessionId);
            if (activeStudents > newPlan.getStudentCap()) {
                throw new SubscriptionRuleViolationException(
                        "Downgrade blocked: active students (" + activeStudents + ") exceed new plan cap (" + newPlan.getStudentCap() + ").");
            }

            subscription.setPricingPlan(newPlan);
            return toDto(subscriptionRepository.save(subscription));
        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
            throw new SubscriptionConcurrencyException("Concurrent subscription update detected while downgrading plan.");
        }
    }

    @Transactional
    public SubscriptionDto extendTrial(Long subscriptionId, Integer additionalDays, String reason, Long performedByUserId) {
        Subscription subscription = getById(subscriptionId);
        if (subscription.getStatus() != SubscriptionStatus.TRIAL) {
            throw new SubscriptionRuleViolationException("Trial extension is allowed only for TRIAL subscriptions.");
        }
        if (additionalDays == null || additionalDays <= 0) {
            throw new SubscriptionRuleViolationException("additionalDays must be greater than 0.");
        }
        LocalDate previous = subscription.getTrialEndDate();
        LocalDate base = previous != null ? previous : LocalDate.now(clock);
        LocalDate updated = base.plusDays(additionalDays);
        subscription.setTrialEndDate(updated);
        subscriptionRepository.save(subscription);

        eventRepository.save(SubscriptionEvent.builder()
                .subscriptionId(subscription.getId())
                .type(SubscriptionEventType.TRIAL_EXTENDED)
                .daysAdded(additionalDays)
                .previousExpiryDate(previous)
                .newExpiryDate(updated)
                .reason(reason)
                .performedByUserId(performedByUserId)
                .build());
        return toDto(subscription);
    }

    @Transactional
    public SubscriptionDto extendSubscription(Long subscriptionId, Integer additionalDays, String reason, Long performedByUserId) {
        Subscription subscription = getById(subscriptionId);
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE && subscription.getStatus() != SubscriptionStatus.PAST_DUE) {
            throw new SubscriptionRuleViolationException("Subscription extension is allowed only for ACTIVE/PAST_DUE subscriptions.");
        }
        if (additionalDays == null || additionalDays <= 0) {
            throw new SubscriptionRuleViolationException("additionalDays must be greater than 0.");
        }
        LocalDate previous = subscription.getExpiryDate();
        if (previous == null) {
            throw new SubscriptionRuleViolationException("Cannot extend a subscription without expiryDate.");
        }
        LocalDate updated = previous.plusDays(additionalDays);
        subscription.setExpiryDate(updated);
        subscriptionRepository.save(subscription);

        eventRepository.save(SubscriptionEvent.builder()
                .subscriptionId(subscription.getId())
                .type(SubscriptionEventType.SUBSCRIPTION_EXTENDED)
                .daysAdded(additionalDays)
                .previousExpiryDate(previous)
                .newExpiryDate(updated)
                .reason(reason)
                .performedByUserId(performedByUserId)
                .build());
        return toDto(subscription);
    }

    @Transactional(readOnly = true)
    public SubscriptionDto getBySchool(Long schoolId) {
        Subscription subscription = subscriptionRepository.findFirstBySchoolIdAndStatusIn(schoolId, LIVE_STATUSES)
                .orElseThrow(() -> new ResourceNotFoundException("No subscription found for school: " + schoolId));
        return toDto(subscription);
    }

    @Transactional
    public int runDailyLifecycleTransition() {
        LocalDate today = LocalDate.now(clock);
        int updatedCount = 0;
        for (Subscription subscription : subscriptionRepository.findByStatusIn(Set.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE))) {
            if (subscription.getExpiryDate() == null) {
                continue;
            }
            LocalDate graceLastDate = subscription.getExpiryDate().plusDays(subscription.getGracePeriodDays());
            if (today.isAfter(graceLastDate) && subscription.getStatus() != SubscriptionStatus.SUSPENDED) {
                subscription.setStatus(SubscriptionStatus.SUSPENDED);
                updatedCount++;
            } else if (today.isAfter(subscription.getExpiryDate()) && subscription.getStatus() == SubscriptionStatus.ACTIVE) {
                subscription.setStatus(SubscriptionStatus.PAST_DUE);
                updatedCount++;
            }
        }
        return updatedCount;
    }

    private Subscription getById(Long id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + id));
    }

    private void enforceSingleLiveSubscription(Long schoolId, Long currentSubscriptionId) {
        boolean exists = currentSubscriptionId == null
                ? subscriptionRepository.existsBySchoolIdAndStatusIn(schoolId, LIVE_STATUSES)
                : subscriptionRepository.existsBySchoolIdAndStatusInAndIdNot(schoolId, LIVE_STATUSES, currentSubscriptionId);
        if (exists) {
            throw new SubscriptionRuleViolationException(
                    "Invariant violation: school already has a TRIAL/ACTIVE/PAST_DUE subscription.");
        }
    }

    private SubscriptionPayment createPayment(Subscription subscription,
                                              BigDecimal amount,
                                              SubscriptionPaymentType type,
                                              LocalDate paymentDate,
                                              String referenceNumber,
                                              String notes,
                                              Long performedByUserId) {
        if (referenceNumber != null && !referenceNumber.isBlank() &&
                paymentRepository.findBySubscriptionIdAndReferenceNumber(subscription.getId(), referenceNumber).isPresent()) {
            throw new SubscriptionRuleViolationException("Duplicate payment reference for this subscription.");
        }

        SubscriptionPayment payment = SubscriptionPayment.builder()
                .schoolId(subscription.getSchoolId())
                .subscription(subscription)
                .amount(normalizeMoney(amount))
                .type(type)
                .paymentDate(paymentDate)
                .referenceNumber(referenceNumber)
                .notes(notes)
                .recordedByUserId(performedByUserId)
                .build();

        return paymentRepository.save(payment);
    }

    private BigDecimal normalizeMoney(BigDecimal input) {
        if (input == null) {
            throw new SubscriptionRuleViolationException("Amount is required.");
        }
        if (input.compareTo(BigDecimal.ZERO) < 0) {
            throw new SubscriptionRuleViolationException("Amount cannot be negative.");
        }
        return input.setScale(2, RoundingMode.HALF_UP);
    }

    private SubscriptionDto toDto(Subscription entity) {
        return SubscriptionDto.builder()
                .id(entity.getId())
                .schoolId(entity.getSchoolId())
                .pricingPlanId(entity.getPricingPlan().getId())
                .pricingPlanName(entity.getPricingPlan().getName())
                .status(entity.getStatus())
                .startDate(entity.getStartDate())
                .trialEndDate(entity.getTrialEndDate())
                .expiryDate(entity.getExpiryDate())
                .gracePeriodDays(entity.getGracePeriodDays())
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private SubscriptionPaymentDto toDto(SubscriptionPayment payment) {
        return SubscriptionPaymentDto.builder()
                .id(payment.getId())
                .subscriptionId(payment.getSubscription().getId())
                .amount(payment.getAmount())
                .type(payment.getType())
                .paymentDate(payment.getPaymentDate())
                .referenceNumber(payment.getReferenceNumber())
                .notes(payment.getNotes())
                .recordedByUserId(payment.getRecordedByUserId())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
