package com.school.backend.school;

import com.school.backend.common.enums.SubscriptionStatus;
import com.school.backend.common.exception.SubscriptionRuleViolationException;
import com.school.backend.school.dto.PricingPlanCreateRequest;
import com.school.backend.school.dto.PricingPlanDto;
import com.school.backend.school.dto.PricingPlanUpdateRequest;
import com.school.backend.school.entity.PricingPlan;
import com.school.backend.school.entity.Subscription;
import com.school.backend.school.repository.PricingPlanRepository;
import com.school.backend.school.repository.SchoolRepository;
import com.school.backend.school.repository.SubscriptionRepository;
import com.school.backend.school.service.PricingPlanService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PricingPlanServiceIntegrationTest {

    @Autowired
    private PricingPlanService pricingPlanService;
    @Autowired
    private PricingPlanRepository pricingPlanRepository;
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private SchoolRepository schoolRepository;

    private Long planId;

    @BeforeEach
    void setup() {
        cleanup();
        PricingPlanCreateRequest req = new PricingPlanCreateRequest();
        req.setName("Starter");
        req.setDescription("Starter plan");
        req.setYearlyPrice(new BigDecimal("50000.00"));
        req.setStudentCap(80);
        req.setTrialDaysDefault(20);
        req.setGracePeriodDaysDefault(7);
        req.setWarningThresholdPercent(80);
        req.setCriticalThresholdPercent(90);
        req.setActive(true);

        planId = pricingPlanService.create(req).getId();
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    @Test
    void createUpdateAndDeactivatePlan() {
        PricingPlanUpdateRequest update = new PricingPlanUpdateRequest();
        update.setName("Starter Plus");
        update.setDescription("Updated");
        update.setYearlyPrice(new BigDecimal("75000.00"));
        update.setStudentCap(100);
        update.setTrialDaysDefault(30);
        update.setGracePeriodDaysDefault(10);
        update.setWarningThresholdPercent(75);
        update.setCriticalThresholdPercent(90);
        update.setActive(true);

        PricingPlanDto updated = pricingPlanService.update(planId, update);
        assertEquals("Starter Plus", updated.getName());

        PricingPlanDto deactivated = pricingPlanService.deactivate(planId);
        assertFalse(deactivated.isActive());
    }

    @Test
    void preventDeactivateWhenLiveSubscriptionsExist() {
        var school = schoolRepository.save(com.school.backend.school.entity.School.builder()
                .name("School")
                .board("CBSE")
                .medium("English")
                .schoolCode("SCHP1")
                .city("X")
                .state("Y")
                .contactEmail("x@y.com")
                .build());

        PricingPlan plan = pricingPlanRepository.findById(planId).orElseThrow();
        subscriptionRepository.save(Subscription.builder()
                .schoolId(school.getId())
                .pricingPlan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(java.time.LocalDate.of(2026,1,1))
                .expiryDate(java.time.LocalDate.of(2027,1,1))
                .gracePeriodDays(10)
                .build());

        assertThrows(SubscriptionRuleViolationException.class,
                () -> pricingPlanService.deactivate(planId));
    }

    private void cleanup() {
        subscriptionRepository.deleteAll();
        pricingPlanRepository.deleteAll();
        schoolRepository.deleteAll();
    }
}
