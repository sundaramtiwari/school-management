package com.school.backend.school.repository;

import com.school.backend.school.entity.SubscriptionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionEventRepository extends JpaRepository<SubscriptionEvent, Long> {
    List<SubscriptionEvent> findBySubscriptionIdOrderByCreatedAtDesc(Long subscriptionId);
}
