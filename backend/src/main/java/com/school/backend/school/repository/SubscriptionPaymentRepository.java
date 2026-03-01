package com.school.backend.school.repository;

import com.school.backend.school.entity.SubscriptionPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, Long> {
    List<SubscriptionPayment> findBySubscriptionIdOrderByPaymentDateDesc(Long subscriptionId);

    Optional<SubscriptionPayment> findBySubscriptionIdAndReferenceNumber(Long subscriptionId, String referenceNumber);
}
