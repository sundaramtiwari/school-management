package com.school.backend.school.service;

import com.school.backend.common.enums.ExpiryWarningLevel;
import com.school.backend.common.enums.SubscriptionStatus;
import com.school.backend.common.enums.UsageWarningLevel;
import com.school.backend.common.exception.InvalidOperationException;
import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.common.exception.SubscriptionRuleViolationException;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.school.dto.SubscriptionAccessStatusDto;
import com.school.backend.school.entity.PricingPlan;
import com.school.backend.school.entity.School;
import com.school.backend.school.entity.Subscription;
import com.school.backend.school.repository.SchoolRepository;
import com.school.backend.school.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
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
public class SubscriptionAccessService {

    private static final Set<SubscriptionStatus> LIVE_STATUSES = Set.of(
            SubscriptionStatus.TRIAL,
            SubscriptionStatus.ACTIVE,
            SubscriptionStatus.PAST_DUE);

    private final SubscriptionRepository subscriptionRepository;
    private final SchoolRepository schoolRepository;
    private final StudentRepository studentRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public SubscriptionAccessStatusDto validateSchoolAccess(Long schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found: " + schoolId));

        Subscription subscription = getSubscriptionForAccess(schoolId, false);
        if (subscription == null) {
            return defaultStatus(school);
        }
        updateLifecycleStatus(subscription);

        if (subscription.getStatus() == SubscriptionStatus.SUSPENDED) {
            return buildStatus(subscription, school); // Don't throw, let UI handle it with boolean
        }
        return buildStatus(subscription, school);
    }

    @Transactional
    public SubscriptionAccessStatusDto validateStudentCreationAllowed(Long schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found: " + schoolId));

        if (!school.isActive()) {
            throw new SubscriptionRuleViolationException("School account is inactive.");
        }

        Subscription subscription = getSubscriptionForAccess(schoolId, true);
        if (subscription == null) {
            return defaultStatus(school);
        }
        updateLifecycleStatus(subscription);

        if (subscription.getStatus() == SubscriptionStatus.SUSPENDED) {
            throw new SubscriptionRuleViolationException("Student creation blocked: subscription is suspended.");
        }

        SubscriptionAccessStatusDto status = buildStatus(subscription, school);
        if (status.getUsagePercent().compareTo(BigDecimal.valueOf(100)) >= 0) {
            throw new SubscriptionRuleViolationException("Student creation blocked: student capacity reached.");
        }
        return status;
    }

    @Transactional(readOnly = true)
    public BigDecimal getUsagePercentage(Long schoolId) {
        Subscription subscription = getSubscriptionForAccess(schoolId, false);
        if (subscription == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return calculateUsagePercent(schoolId, subscription.getPricingPlan().getStudentCap());
    }

    @Transactional(readOnly = true)
    public ExpiryWarningLevel getExpiryWarningStatus(Long schoolId) {
        Subscription subscription = getSubscriptionForAccess(schoolId, false);
        if (subscription == null) {
            return ExpiryWarningLevel.NONE;
        }
        return calculateExpiryWarning(subscription);
    }

    private Subscription getSubscriptionForAccess(Long schoolId, boolean forUpdate) {
        if (schoolId == null) {
            throw new InvalidOperationException("School context is required for subscription validation.");
        }
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found: " + schoolId));

        if (forUpdate) {
            return subscriptionRepository.findFirstBySchoolIdAndStatusIn(school.getId(), LIVE_STATUSES)
                    .map(s -> subscriptionRepository.findByIdAndSchoolId(s.getId(), schoolId).orElse(s))
                    .or(() -> subscriptionRepository.findFirstBySchoolIdOrderByCreatedAtDesc(school.getId()))
                    .orElse(null);
        }
        return subscriptionRepository.findFirstBySchoolIdAndStatusIn(school.getId(), LIVE_STATUSES)
                .or(() -> subscriptionRepository.findFirstBySchoolIdOrderByCreatedAtDesc(school.getId()))
                .orElse(null);
    }

    private void updateLifecycleStatus(Subscription subscription) {
        if (subscription.getStatus() == SubscriptionStatus.TRIAL) {
            LocalDate today = LocalDate.now(clock);
            if (subscription.getTrialEndDate() != null && today.isAfter(subscription.getTrialEndDate())) {
                subscription.setStatus(SubscriptionStatus.PAST_DUE);
            }
            return;
        }
        if (subscription.getExpiryDate() == null) {
            return;
        }
        LocalDate today = LocalDate.now(clock);
        LocalDate graceLastDate = subscription.getExpiryDate().plusDays(subscription.getGracePeriodDays());

        if (today.isAfter(graceLastDate)) {
            subscription.setStatus(SubscriptionStatus.SUSPENDED);
        } else if (today.isAfter(subscription.getExpiryDate())
                && subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            subscription.setStatus(SubscriptionStatus.PAST_DUE);
        }
    }

    private SubscriptionAccessStatusDto buildStatus(Subscription subscription, School school) {
        PricingPlan plan = subscription.getPricingPlan();
        BigDecimal usagePercent = calculateUsagePercent(school.getId(), plan.getStudentCap());
        UsageWarningLevel usageWarningLevel = calculateUsageWarning(usagePercent, plan);
        long daysToExpiry = calculateDaysToExpiry(subscription);
        ExpiryWarningLevel expiryWarningLevel = calculateExpiryWarning(subscription);

        return SubscriptionAccessStatusDto.builder()
                .subscriptionStatus(subscription.getStatus())
                .usagePercent(usagePercent)
                .usageWarningLevel(usageWarningLevel)
                .daysToExpiry(daysToExpiry)
                .expiryWarningLevel(expiryWarningLevel)
                .schoolActive(school.isActive())
                .build();
    }

    private BigDecimal calculateUsagePercent(Long schoolId, Integer studentCap) {
        Long currentSessionId = schoolRepository.findById(schoolId)
                .map(School::getCurrentSessionId)
                .orElse(null);
        if (currentSessionId == null || studentCap == null || studentCap <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        long activeStudents = studentRepository.countActiveStudentsInSession(schoolId, currentSessionId);
        return BigDecimal.valueOf(activeStudents)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(studentCap), 2, RoundingMode.HALF_UP);
    }

    private UsageWarningLevel calculateUsageWarning(BigDecimal usagePercent, PricingPlan plan) {
        if (usagePercent.compareTo(BigDecimal.valueOf(plan.getCriticalThresholdPercent())) >= 0) {
            return UsageWarningLevel.CRITICAL;
        }
        if (usagePercent.compareTo(BigDecimal.valueOf(plan.getWarningThresholdPercent())) >= 0) {
            return UsageWarningLevel.WARNING;
        }
        return UsageWarningLevel.NONE;
    }

    private long calculateDaysToExpiry(Subscription subscription) {
        LocalDate today = LocalDate.now(clock);
        LocalDate referenceDate = subscription.getStatus() == SubscriptionStatus.TRIAL
                ? subscription.getTrialEndDate()
                : subscription.getExpiryDate();
        if (referenceDate == null) {
            return Long.MAX_VALUE;
        }
        return ChronoUnit.DAYS.between(today, referenceDate);
    }

    private ExpiryWarningLevel calculateExpiryWarning(Subscription subscription) {
        LocalDate today = LocalDate.now(clock);
        LocalDate referenceDate = subscription.getStatus() == SubscriptionStatus.TRIAL
                ? subscription.getTrialEndDate()
                : subscription.getExpiryDate();
        if (referenceDate == null) {
            return ExpiryWarningLevel.NONE;
        }

        long days = ChronoUnit.DAYS.between(today, referenceDate);
        if (days < 0) {
            return ExpiryWarningLevel.EXPIRED;
        }
        if (days <= 7) {
            return ExpiryWarningLevel.CRITICAL_7;
        }
        if (days <= 30) {
            return ExpiryWarningLevel.WARNING_30;
        }
        return ExpiryWarningLevel.NONE;
    }

    private SubscriptionAccessStatusDto defaultStatus(School school) {
        return SubscriptionAccessStatusDto.builder()
                .subscriptionStatus(null)
                .usagePercent(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .usageWarningLevel(UsageWarningLevel.NONE)
                .daysToExpiry(0L)
                .expiryWarningLevel(ExpiryWarningLevel.NONE)
                .schoolActive(school.isActive())
                .build();
    }
}
