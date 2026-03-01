package com.school.backend.school;

import com.school.backend.common.enums.Gender;
import com.school.backend.common.enums.SubscriptionStatus;
import com.school.backend.common.exception.SubscriptionRuleViolationException;
import com.school.backend.common.tenant.SessionContext;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.core.student.entity.Student;
import com.school.backend.core.student.entity.StudentEnrollment;
import com.school.backend.core.student.repository.StudentEnrollmentRepository;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.school.dto.*;
import com.school.backend.school.entity.PricingPlan;
import com.school.backend.school.entity.School;
import com.school.backend.school.entity.Subscription;
import com.school.backend.school.entity.SubscriptionEvent;
import com.school.backend.school.repository.PricingPlanRepository;
import com.school.backend.school.repository.SchoolRepository;
import com.school.backend.school.repository.SubscriptionEventRepository;
import com.school.backend.school.repository.SubscriptionPaymentRepository;
import com.school.backend.school.repository.SubscriptionRepository;
import com.school.backend.school.service.SubscriptionAccessService;
import com.school.backend.school.service.SubscriptionService;
import com.school.backend.user.entity.User;
import com.school.backend.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestClockConfig.class)
class SubscriptionServiceIntegrationTest {

    @Autowired
    private SubscriptionService subscriptionService;
    @Autowired
    private SubscriptionAccessService subscriptionAccessService;
    @Autowired
    private PricingPlanRepository pricingPlanRepository;
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private SubscriptionPaymentRepository paymentRepository;
    @Autowired
    private SubscriptionEventRepository eventRepository;
    @Autowired
    private SchoolRepository schoolRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private StudentEnrollmentRepository enrollmentRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private MutableClock clock;

    private School school;
    private PricingPlan basicPlan;
    private PricingPlan proPlan;
    private User actor;

    @BeforeEach
    void setup() {
        cleanup();
        clock.setDate(LocalDate.of(2026, 1, 1));

        school = schoolRepository.save(School.builder()
                .name("Alpha School")
                .displayName("Alpha")
                .board("CBSE")
                .medium("English")
                .schoolCode("ALPHA001")
                .city("Varanasi")
                .state("UP")
                .contactEmail("alpha@test.com")
                .active(true)
                .build());

        basicPlan = pricingPlanRepository.save(PricingPlan.builder()
                .name("Basic")
                .description("Basic Plan")
                .yearlyPrice(new BigDecimal("120000.00"))
                .studentCap(100)
                .trialDaysDefault(30)
                .gracePeriodDaysDefault(10)
                .warningThresholdPercent(80)
                .criticalThresholdPercent(90)
                .active(true)
                .build());

        proPlan = pricingPlanRepository.save(PricingPlan.builder()
                .name("Pro")
                .description("Pro Plan")
                .yearlyPrice(new BigDecimal("180000.00"))
                .studentCap(250)
                .trialDaysDefault(30)
                .gracePeriodDaysDefault(10)
                .warningThresholdPercent(80)
                .criticalThresholdPercent(90)
                .active(true)
                .build());

        actor = userRepository.save(User.builder()
                .email("platform@test.com")
                .passwordHash(passwordEncoder.encode("admin123"))
                .role(com.school.backend.common.enums.UserRole.PLATFORM_ADMIN)
                .active(true)
                .build());
    }

    @AfterEach
    void tearDown() {
        cleanup();
        TenantContext.clear();
        SessionContext.clear();
    }

    @Test
    void trialCreation_thenPastDueTransition_whenTrialExpires() {
        CreateSubscriptionTrialRequest req = new CreateSubscriptionTrialRequest();
        req.setSchoolId(school.getId());
        req.setPricingPlanId(basicPlan.getId());
        req.setTrialDays(10);

        SubscriptionDto trial = subscriptionService.createSubscriptionWithTrial(req, actor.getId());
        assertEquals(SubscriptionStatus.TRIAL, trial.getStatus());
        assertEquals(LocalDate.of(2026, 1, 11), trial.getTrialEndDate());
        assertNull(trial.getExpiryDate());

        clock.setDate(LocalDate.of(2026, 1, 12));
        SubscriptionAccessStatusDto status = subscriptionAccessService.validateSchoolAccess(school.getId());
        assertEquals(SubscriptionStatus.PAST_DUE, status.getSubscriptionStatus());
    }

    @Test
    void activationSets365DayExpiryFromPaymentDate() {
        Long subscriptionId = createTrialSubscription(30);

        ActivateSubscriptionRequest request = new ActivateSubscriptionRequest();
        request.setPaymentDate(LocalDate.of(2026, 2, 1));
        request.setReferenceNumber("PAY-001");
        request.setNotes("Activation payment");

        SubscriptionDto activated = subscriptionService.activateSubscription(subscriptionId, request, actor.getId());

        assertEquals(SubscriptionStatus.ACTIVE, activated.getStatus());
        assertEquals(LocalDate.of(2026, 2, 1), activated.getStartDate());
        assertEquals(LocalDate.of(2027, 2, 1), activated.getExpiryDate());
    }

    @Test
    void upgradeCalculatesProration_andKeepsExpiryUnchanged() {
        Long subscriptionId = createPaidSubscription(LocalDate.of(2026, 1, 1));

        Subscription before = subscriptionRepository.findById(subscriptionId).orElseThrow();
        LocalDate originalExpiry = before.getExpiryDate();

        clock.setDate(LocalDate.of(2026, 7, 1));
        UpgradePlanRequest req = new UpgradePlanRequest();
        req.setNewPlanId(proPlan.getId());
        req.setNotes("Mid-year upgrade");

        UpgradePlanResponse response = subscriptionService.upgradePlan(subscriptionId, req, actor.getId());

        assertEquals(new BigDecimal("30246.58"), response.getProratedAmount());
        assertEquals(originalExpiry, response.getSubscription().getExpiryDate());
        assertEquals(proPlan.getId(), response.getSubscription().getPricingPlanId());
        assertEquals(com.school.backend.common.enums.SubscriptionPaymentType.UPGRADE_PRORATION,
                response.getProrationPayment().getType());
    }

    @Test
    void concurrentUpgrade_oneShouldFailWithConcurrencyOrInvariant() throws Exception {
        Long subscriptionId = createPaidSubscription(LocalDate.of(2026, 1, 1));
        clock.setDate(LocalDate.of(2026, 7, 1));

        UpgradePlanRequest req = new UpgradePlanRequest();
        req.setNewPlanId(proPlan.getId());
        req.setNotes("Concurrent upgrade");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<String> task = () -> {
            start.await();
            try {
                subscriptionService.upgradePlan(subscriptionId, req, actor.getId());
                return "SUCCESS";
            } catch (Exception ex) {
                return ex.getClass().getSimpleName();
            }
        };

        Future<String> f1 = executor.submit(task);
        Future<String> f2 = executor.submit(task);
        start.countDown();

        String r1 = f1.get(10, TimeUnit.SECONDS);
        String r2 = f2.get(10, TimeUnit.SECONDS);
        executor.shutdownNow();

        long successCount = List.of(r1, r2).stream().filter("SUCCESS"::equals).count();
        assertEquals(1, successCount, "Exactly one upgrade should succeed");
    }

    @Test
    void downgradeBlockedWhenActiveStudentsExceedCap() {
        Long subscriptionId = createPaidSubscription(LocalDate.of(2026, 1, 1));
        createCurrentSessionAndStudents(120);

        PricingPlan smallPlan = pricingPlanRepository.save(PricingPlan.builder()
                .name("Tiny")
                .description("Tiny Plan")
                .yearlyPrice(new BigDecimal("100000.00"))
                .studentCap(100)
                .trialDaysDefault(15)
                .gracePeriodDaysDefault(5)
                .warningThresholdPercent(80)
                .criticalThresholdPercent(90)
                .active(true)
                .build());

        DowngradePlanRequest req = new DowngradePlanRequest();
        req.setNewPlanId(smallPlan.getId());
        req.setReason("Need lower tier");

        SubscriptionRuleViolationException ex = assertThrows(SubscriptionRuleViolationException.class,
                () -> subscriptionService.downgradePlan(subscriptionId, req, actor.getId()));
        assertTrue(ex.getMessage().contains("Downgrade blocked"));
    }

    @Test
    void studentCreationBlockedAt100PercentUsage() {
        createPaidSubscription(LocalDate.of(2026, 1, 1));
        createCurrentSessionAndStudents(100);

        SubscriptionAccessStatusDto status = subscriptionAccessService.validateSchoolAccess(school.getId());
        assertEquals(new BigDecimal("100.00"), status.getUsagePercent());

        assertThrows(SubscriptionRuleViolationException.class,
                () -> subscriptionAccessService.validateStudentCreationAllowed(school.getId()));
    }

    @Test
    void suspendedAfterGrace_blocksAccess() {
        createPaidSubscription(LocalDate.of(2026, 1, 1));

        clock.setDate(LocalDate.of(2027, 1, 13)); // expiry 2027-01-01 + grace 10 => suspended after 2027-01-11
        SubscriptionRuleViolationException ex = assertThrows(SubscriptionRuleViolationException.class,
                () -> subscriptionAccessService.validateSchoolAccess(school.getId()));
        assertTrue(ex.getMessage().toLowerCase().contains("suspended")
                || ex.getMessage().toLowerCase().contains("no active/trial subscription"));
    }

    @Test
    void trialAndPaidExtension_areExplicitlyLogged() {
        Long trialId = createTrialSubscription(10);

        SubscriptionDto trialExtended = subscriptionService.extendTrial(trialId, 5, "Support approved", actor.getId());
        assertEquals(LocalDate.of(2026, 1, 16), trialExtended.getTrialEndDate());

        ActivateSubscriptionRequest activate = new ActivateSubscriptionRequest();
        activate.setPaymentDate(LocalDate.of(2026, 1, 1));
        activate.setReferenceNumber("PAY-EXT-" + trialId);
        activate.setNotes("Activate for extension");
        subscriptionService.activateSubscription(trialId, activate, actor.getId());

        SubscriptionDto extended = subscriptionService.extendSubscription(trialId, 20, "Manual extension", actor.getId());
        assertEquals(LocalDate.of(2027, 1, 21), extended.getExpiryDate());

        List<SubscriptionEvent> events = eventRepository.findAll();
        assertEquals(2, events.size());
    }

    private Long createTrialSubscription(int trialDays) {
        CreateSubscriptionTrialRequest req = new CreateSubscriptionTrialRequest();
        req.setSchoolId(school.getId());
        req.setPricingPlanId(basicPlan.getId());
        req.setTrialDays(trialDays);
        return subscriptionService.createSubscriptionWithTrial(req, actor.getId()).getId();
    }

    private Long createPaidSubscription(LocalDate paymentDate) {
        Long id = createTrialSubscription(10);
        ActivateSubscriptionRequest request = new ActivateSubscriptionRequest();
        request.setPaymentDate(paymentDate);
        request.setReferenceNumber("PAY-" + id);
        request.setNotes("Initial activation");
        return subscriptionService.activateSubscription(id, request, actor.getId()).getId();
    }

    private void createCurrentSessionAndStudents(int count) {
        school.setCurrentSessionId(999L);
        schoolRepository.save(school);

        for (int i = 0; i < count; i++) {
            Student student = studentRepository.save(Student.builder()
                    .schoolId(school.getId())
                    .admissionNumber("ADM" + i)
                    .firstName("S" + i)
                    .gender(Gender.MALE)
                    .active(true)
                    .build());

            enrollmentRepository.save(StudentEnrollment.builder()
                    .schoolId(school.getId())
                    .studentId(student.getId())
                    .classId(1L)
                    .sessionId(999L)
                    .active(true)
                    .build());
        }
    }

    private void cleanup() {
        eventRepository.deleteAll();
        paymentRepository.deleteAll();
        subscriptionRepository.deleteAll();
        enrollmentRepository.deleteAll();
        studentRepository.deleteAll();
        userRepository.deleteAll();
        pricingPlanRepository.deleteAll();
        schoolRepository.deleteAll();
    }
}
