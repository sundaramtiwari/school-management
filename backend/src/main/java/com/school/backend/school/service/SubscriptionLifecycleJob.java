package com.school.backend.school.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionLifecycleJob {

    private final SubscriptionService subscriptionService;

    @Scheduled(cron = "0 0 2 * * *")
    public void runDailyLifecycle() {
        int transitioned = subscriptionService.runDailyLifecycleTransition();
        if (transitioned > 0) {
            log.info("Subscription lifecycle job transitioned {} subscriptions.", transitioned);
        }
    }
}
