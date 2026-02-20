package com.school.backend.school.entity;

import com.school.backend.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs", indexes = {
        @Index(name = "idx_activity_logs_school_timestamp", columnList = "school_id, timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class ActivityLog extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String action;      // e.g. "Created Student"
    private String entityType;  // e.g. STUDENT, FEE, TEST
    private Long entityId;

    private String ipAddress;
    private LocalDateTime timestamp;
}
