package com.school.backend.school.repository;

import com.school.backend.common.enums.SubscriptionStatus;
import com.school.backend.school.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findFirstBySchoolIdAndStatusIn(Long schoolId, Collection<SubscriptionStatus> statuses);
    Optional<Subscription> findFirstBySchoolIdOrderByCreatedAtDesc(Long schoolId);

    boolean existsBySchoolIdAndStatusIn(Long schoolId, Collection<SubscriptionStatus> statuses);

    boolean existsBySchoolIdAndStatusInAndIdNot(Long schoolId,
                                                Collection<SubscriptionStatus> statuses,
                                                Long id);

    List<Subscription> findByStatusIn(Collection<SubscriptionStatus> statuses);

    long countByStatusIn(Collection<SubscriptionStatus> statuses);

    long countByPricingPlanId(Long pricingPlanId);

    boolean existsByPricingPlanIdAndStatusIn(Long pricingPlanId, Collection<SubscriptionStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Subscription> findByIdAndSchoolId(Long id, Long schoolId);
}
