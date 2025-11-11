package com.school.backend.school.repository;

import com.school.backend.school.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findBySchoolIdAndActive(Long schoolId, boolean active);
}
