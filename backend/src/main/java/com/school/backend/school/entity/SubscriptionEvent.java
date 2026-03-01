package com.school.backend.school.entity;

import com.school.backend.common.entity.BaseEntity;
import com.school.backend.common.enums.SubscriptionEventType;
import com.school.backend.common.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "subscription_events",
        indexes = {
                @Index(name = "idx_subscription_event_subscription_id", columnList = "subscription_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class SubscriptionEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "subscription_id", nullable = false)
    private Long subscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SubscriptionEventType type;

    @Column(name = "days_added")
    private Integer daysAdded;

    @Column(name = "previous_expiry_date")
    private LocalDate previousExpiryDate;

    @Column(name = "new_expiry_date")
    private LocalDate newExpiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 20)
    private SubscriptionStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 20)
    private SubscriptionStatus newStatus;

    @Column(length = 500)
    private String reason;

    @Column(name = "performed_by_user_id", nullable = false)
    private Long performedByUserId;
}
